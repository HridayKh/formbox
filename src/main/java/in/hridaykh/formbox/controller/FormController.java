package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.dto.FormSettingsRequest;
import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.dto.TierValidationResult;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.repository.FolderRepository;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.service.cache.SubmissionCacheService;
import in.hridaykh.formbox.service.form.FormSettingsService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
public class FormController {

	private final TenantRepository tenantRepository;
	private final FormRepository formRepository;
	private final SubmissionCacheService submissionCacheService;
	private final FormCacheService formCacheService;
	private final FormSettingsService formSettingsService;
	private final EntitlementsCacheService entitlementsCacheService;
	private final FolderRepository folderRepository;

	@PostMapping("/{folderId}")
	@WithSpan
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam String formName,
	                         @RequestParam(required = false) String redirectUrl, @PathVariable UUID folderId) {
		log.debug("Processing request to create a new form. Name: [{}], Requested Redirect URL: [{}]", formName, redirectUrl);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		List<CachedForm> forms = formCacheService.getTenantForms(tenantId);

		Entitlements entitlements = entitlementsCacheService.getEntitlements(tenantId);
		String msg = "";

		if (forms.size() >= entitlements.formsLimit()) {
			msg = "Your have Reached Your Forms Limit, Upgrade For More!";
			return PathRegistry.DASHBOARD + "?msg=" + msg;
		}

		if (!entitlements.redirectUrlsAllowed() && redirectUrl != null && !redirectUrl.isBlank()) {
			log.warn("Tier constraint violation intercepted. Free tier tenant: {} attempted custom redirect validation rules.", tenantId);
			redirectUrl = null;
			msg = "Please upgrade for redirect URL!";
		}

		Form newForm = new Form();
		newForm.setTenant(tenantRepository.getReferenceById(tenantId));
		newForm.setName(formName);
		newForm.setRedirectUrl(redirectUrl);
		newForm.setAllowJson(entitlements.jsonFormsAllowed());
		newForm.setAllowFiles(entitlements.fileUploadsAllowed());
		newForm.setRateLimitRpm(Math.min(20, entitlements.maxRateLimitRpm()));
		newForm.setFolder(folderRepository.getReferenceById(folderId));

		Form savedForm = formRepository.save(newForm);
		log.info("Successfully persisted new Form Entity. ID: {} for tenant ID: {}", savedForm.getId(), tenantId);

		formCacheService.updateFormCache(savedForm);
		formCacheService.evictTenantForms(tenantId);

		return "redirect:/forms/" + folderId + "/" + savedForm.getId() + msg;
	}

	@GetMapping("/{folderId}")
	@WithSpan
	public String listForms(@RequestAttribute JwtPayload userMetadata, Model model, @PathVariable String folderId) {
		String tenantId = userMetadata.getSub();
		log.trace("Processing request to render forms row table layout map context for user reference: {}", tenantId);

		if (tenantId == null) {
			log.warn("Forms retrieval denied. Intercepted request thread missing user target metadata properties.");
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		List<CachedForm> forms = formCacheService.getTenantForms(UUID.fromString(tenantId));
		log.debug("Loaded {} forms from cache layers for tenant index ID: {}", forms.size(), tenantId);

		model.addAttribute("forms", forms);
		return ViewRegistry.Fragments.FORM_ROWS;
	}

	@GetMapping("/{folderId}/{formId}")
	@WithSpan
	public String manageForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(value = "msg", required = false) String msg, Model model, @PathVariable String folderId) {
		log.debug("Loading primary console management data array structure for form ID: {} triggered by user: {}", formId, userMetadata.getSub());
		CachedForm form = formCacheService.getCachedForm(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Security authorization intercept triggered. User: {} failed ownership check rule bounds for form ID: {} belonging to tenant: {}", userMetadata.getSub(), formId, form.tenantId());
			throw new RuntimeException("Unauthorized access to form system.");
		}

		if ("upgrade_required_for_redirect".equals(msg)) {
			log.trace("Redirect upgrade warning detected in query parameters");
			model.addAttribute("warningMessage", "Form created successfully! However, custom redirects are only available on paid tiers.");
		}

		FormSubmissionsResponse submissions = submissionCacheService.getFormSubmissionsGrouped(formId);
		Entitlements entitlements = entitlementsCacheService.getEntitlements(form.tenantId());

		log.trace("Loaded dashboard variables for form {}: {} submissions, {} spam", formId, submissions.submissions().size(), submissions.spam().size());

		model.addAttribute("form", form);
		model.addAttribute("entitlements", entitlements);
		model.addAttribute("redirectUrlNotAllowed", !entitlements.redirectUrlsAllowed());
		model.addAttribute("fieldValidationsNotAllowed", !entitlements.fieldValidationsAllowed());
		model.addAttribute("turnstileNotAllowed", !entitlements.turnstileAllowed());
		model.addAttribute("jsonFormsNotAllowed", !entitlements.jsonFormsAllowed());
		model.addAttribute("fileUploadsNotAllowed", !entitlements.fileUploadsAllowed());
		model.addAttribute("submissions", submissions.submissions());
		model.addAttribute("spamSubmissions", submissions.spam());

		return "pages/manage-form";
	}

	@PutMapping("/{folderId}/{formId}")
	@WithSpan
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(value = "fieldValidationsRaw", required = false) String fieldValidationsRaw, @ModelAttribute FormSettingsRequest request, Model model, @PathVariable String folderId) {

		log.debug("Initiating settings update for form ID: {}", formId);

		List<String> validations = new ArrayList<>();
		if (fieldValidationsRaw != null) {
			validations = Arrays.stream(fieldValidationsRaw.split("\\r?\\n")).map(String::strip).filter(s -> !s.isEmpty()).toList();
		}

		FormSettingsRequest fullRequest = new FormSettingsRequest(request.name(), request.redirectUrl(), request.isActive(), request.turnstileSecretKey(), request.honeypotName(), request.rateLimitRpm(), request.allowFiles(), request.allowHtmx(), request.allowJson(), validations);

		// Execute core business logic
		TierValidationResult result = formSettingsService.updateFormSettings(formId, userMetadata.getSub(), fullRequest);

		Entitlements entitlements = entitlementsCacheService.getEntitlements(UUID.fromString(userMetadata.getSub()));
		model.addAttribute("entitlements", entitlements);
		model.addAttribute("redirectUrlNotAllowed", !entitlements.redirectUrlsAllowed());
		model.addAttribute("fieldValidationsNotAllowed", !entitlements.fieldValidationsAllowed());
		model.addAttribute("turnstileNotAllowed", !entitlements.turnstileAllowed());
		model.addAttribute("jsonFormsNotAllowed", !entitlements.jsonFormsAllowed());
		model.addAttribute("fileUploadsNotAllowed", !entitlements.fileUploadsAllowed());

		if (result.hasWarnings()) {
			model.addAttribute("warnings", result.warnings());
		} else {
			model.addAttribute("message", "Form configurations updated successfully!");
		}

		model.addAttribute("form", result.updatedForm());

		return ViewRegistry.Fragments.SETTINGS;
	}

	@DeleteMapping("/{folderId}/{formId}")
	@ResponseBody
	@WithSpan
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @PathVariable String folderId) {
		log.debug("Deleting form with ID: {}", formId);
		CachedForm form = formCacheService.getCachedForm(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized delete attempt of form {} by user {}", formId, userMetadata.getSub());
			return;
		}

		formRepository.deleteById(form.id());
		log.info("Permanently deleted form ID: {}", formId);

		formCacheService.evictFormCache(formId);
		formCacheService.evictTenantForms(form.tenantId());
	}
}