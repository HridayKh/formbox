package formbox.form.internal;

import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.shared.Entitlements;
import formbox.shared.PathRegistry;
import formbox.billing.EntitlementsApi;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
class FormController {

	private final FormRepository formRepository;
	private final FormApi formApi;
	private final FormSettingsService formSettingsService;
	private final EntitlementsApi entitlementsApi;

	@PostMapping("/{folderId}")
	@WithSpan
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam String formName, @RequestParam(required = false) String redirectUrl, @PathVariable UUID folderId) {
		log.debug("Processing request to create a new form. Name: [{}], Requested Redirect URL: [{}]", formName, redirectUrl);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		List<FormDto> forms = formApi.getTenantForms(tenantId);

		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
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
		newForm.setTenantId(tenantId);
		newForm.setName(formName);
		newForm.setRedirectUrl(redirectUrl);
		newForm.setAllowJson(entitlements.jsonFormsAllowed());
		newForm.setAllowFiles(entitlements.fileUploadsAllowed());
		newForm.setRateLimitRpm(Math.min(20, entitlements.maxRateLimitRpm()));
		newForm.setFolderId(folderId);

		Form savedForm = formRepository.save(newForm);
		log.info("Successfully persisted new Form Entity. ID: {} for tenant ID: {}", savedForm.getId(), tenantId);

		formApi.evictTenantForms(tenantId);

		return "redirect:/forms/" + folderId + "/" + formApi.updateFormCache(savedForm.toFormDto()).id() + "?msg=" + msg;
	}

	@PutMapping("/{ignoredFId}/{formId}")
	@WithSpan
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @PathVariable String ignoredFId, @RequestParam(required = false) String fieldValidationsRaw, Model model, @ModelAttribute FormSettingsRequest request) {
		List<String> validations = new ArrayList<>();
		if (fieldValidationsRaw != null)
			validations = Arrays.stream(fieldValidationsRaw.split("\\r?\\n")).map(String::strip).filter(s -> !s.isEmpty()).toList();

		var fullRequest = new FormSettingsRequest(request.name(), request.redirectUrl(), request.isActive(), request.turnstileSecretKey(), request.honeypotName(), request.rateLimitRpm(), request.allowFiles(), request.allowHtmx(), request.allowJson(), validations);

		var result = formSettingsService.updateFormSettings(formId, userMetadata.getSub(), fullRequest);

		var entitlements = entitlementsApi.getEntitlements(UUID.fromString(Objects.requireNonNull(userMetadata.getSub())));
		model.addAttribute("entitlements", entitlements);
		model.addAttribute("redirectUrlNotAllowed", !entitlements.redirectUrlsAllowed());
		model.addAttribute("fieldValidationsNotAllowed", !entitlements.fieldValidationsAllowed());
		model.addAttribute("turnstileNotAllowed", !entitlements.turnstileAllowed());
		model.addAttribute("jsonFormsNotAllowed", !entitlements.jsonFormsAllowed());
		model.addAttribute("fileUploadsNotAllowed", !entitlements.fileUploadsAllowed());

		if (result.hasWarnings()) model.addAttribute("warnings", result.warnings());
		else model.addAttribute("message", "Form configurations updated successfully!");

		model.addAttribute("form", result.updatedForm());

		log.debug("Updated settings for formId: {}", formId);
		return "fragments/manage/tab-settings :: settings-panel";
	}

	@DeleteMapping("/{ignoredFolderId}/{formId}")
	@ResponseBody
	@WithSpan
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @PathVariable String ignoredFolderId) {
		log.debug("Deleting form with ID: {}", formId);
		FormDto form = formApi.getFormDto(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized delete attempt of form {} by user {}", formId, userMetadata.getSub());
			return;
		}

		formRepository.deleteById(form.id());
		log.info("Permanently deleted form ID: {}", formId);

		formApi.evictFormCache(formId);
		formApi.evictTenantForms(form.tenantId());
	}
}