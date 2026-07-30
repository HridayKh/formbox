package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.billing.service.PolarCacheService;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.dto.FormSettingsRequest;
import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.dto.TierValidationResult;
import in.hridaykh.formbox.model.entity.Folder;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.repository.FolderRepository;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import in.hridaykh.formbox.service.cache.FolderCacheService;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.service.cache.SubmissionCacheService;
import in.hridaykh.formbox.service.form.FormSettingsService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
	private final PolarCacheService polarCacheService;
	private final FolderCacheService folderCacheService;

	@PostMapping("/{folderId}")
	@WithSpan
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam String formName,
	                         @RequestParam(required = false) String redirectUrl, @PathVariable UUID folderId) {
		log.debug("Processing request to create a new form. Name: [{}], Requested Redirect URL: [{}]", formName, redirectUrl);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		List<CachedForm> forms = formCacheService.getTenantForms(tenantId);

		Entitlements entitlements = entitlementsCacheService.getEntitlements(tenantId);
		String msg = "Form created successfully!";

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

		return "redirect:/forms/" + folderId + "/" + savedForm.getId() + "?msg=" + msg;
	}

	@GetMapping("/{ignoredFolderId}/{formId}")
	@WithSpan
	public String manageFormPage(@RequestAttribute JwtPayload userMetadata, @RequestParam(required = false) String msg, @PathVariable UUID formId, Model model, @PathVariable String ignoredFolderId) {
		log.debug("Loading primary console management data array structure for form ID: {} triggered by user: {}", formId, userMetadata.getSub());
		CachedForm form = formCacheService.getCachedForm(formId);

		if (form == null)
			return "redirect:/dashboard?msg=Form not found!";

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized form manage attempt of form {} by user {}", formId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid form";
		}

		FormSubmissionsResponse submissions = submissionCacheService.getFormSubmissionsGrouped(formId);
		Entitlements entitlements = entitlementsCacheService.getEntitlements(form.tenantId());

		log.trace("Loaded dashboard variables for form {}: {} submissions, {} spam", formId, submissions.submissions().size(), submissions.spam().size());

		model.addAttribute("msg", msg);
		model.addAttribute("balanceLeft", polarCacheService.getCachedSubmissionBalance(form.tenantId()));
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", userMetadata.getEmail());

		model.addAttribute("folders", folderCacheService.getTenantFolders(form.tenantId()));
		model.addAttribute("form", form);
		model.addAttribute("entitlements", entitlements);
		model.addAttribute("validSubmissions", submissions.submissions());
		model.addAttribute("spamSubmissions", submissions.spam());

		return "dash/manage-form";
	}

	@PostMapping("/{folderId}/{formId}/move")
	@WithSpan
	@Transactional
	public String moveForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID folderId, @PathVariable UUID formId) {
		log.debug("Moving folder: {} for user {}", folderId, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Optional<Folder> folderOpt = folderRepository.findById(folderId);
		if (folderOpt.isEmpty())
			return "redirect:/dashboard?msg=Folder not found!";
		Folder folder = folderOpt.get();
		if (!folder.getTenant().getId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized form move attempt of folder {} by user {}", folderId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid folder";
		}

		Optional<Form> form = formRepository.findById(formId);
		if (form.isEmpty())
			return "redirect:/dashboard?msg=Form not found!";
		if (!form.get().getTenant().getId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized form move attempt of form {} by user {}", formId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid form";
		}

		Form f = form.get();
		f.setFolder(folder);
		Form savedF = formRepository.save(f);
		log.info("Successfully moved form. ID: {} for tenant ID: {}", formId, tenantId);

		formCacheService.updateFormCache(savedF);
		formCacheService.evictTenantForms(tenantId);

		String msg = "Successfully moved the form " + f.getName() + " to folder " + folder.getName();
		return "redirect:/forms/" + folderId + "/" + formId + "?msg=" + msg;
	}

	@PutMapping("/{ignoredFolderId}/{formId}")
	@WithSpan // TODO: verify the tenant owns the form
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(value = "fieldValidationsRaw", required = false) String fieldValidationsRaw, @ModelAttribute FormSettingsRequest request, Model model, @PathVariable String ignoredFolderId) {

		log.debug("Initiating settings update for form ID: {}", formId);

		List<String> validations = new ArrayList<>();
		if (fieldValidationsRaw != null) {
			validations = Arrays.stream(fieldValidationsRaw.split("\\r?\\n")).map(String::strip).filter(s -> !s.isEmpty()).toList();
		}

		FormSettingsRequest fullRequest = new FormSettingsRequest(request.name(), request.redirectUrl(), request.isActive(), request.turnstileSecretKey(), request.honeypotName(), request.rateLimitRpm(), request.allowFiles(), request.allowHtmx(), request.allowJson(), validations);

		// Execute core business logic
		TierValidationResult result = formSettingsService.updateFormSettings(formId, userMetadata.getSub(), fullRequest);

		Entitlements entitlements = entitlementsCacheService.getEntitlements(UUID.fromString(Objects.requireNonNull(userMetadata.getSub())));
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

	@DeleteMapping("/{ignoredFolderId}/{formId}")
	@ResponseBody
	@WithSpan
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @PathVariable String ignoredFolderId) {
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