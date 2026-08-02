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

	@PostMapping("/{}/{formId}/update")
	@WithSpan
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @ModelAttribute FormSettingsRequest request) {
		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		var result = formSettingsService.updateFormSettings(formId, tenantId, request);

		String msg = "Form configurations updated successfully!";
		if (result.hasWarnings()) msg += "\nYou have warning(s):\n" + String.join("\n", result.getWarnings());

		log.debug("Updated settings for formId: {}", formId);
		return "redirect:/forms/" + result.getFolderId() + "/" + formId + "?msg=" + msg;
	}


	@PostMapping("/{}/{formId}/delete")
	@WithSpan
	public String deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId) {
		FormDto form = formApi.getFormDto(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized delete attempt of form {} by user {}", formId, userMetadata.getSub());
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Invalid Form";
		}

		formRepository.deleteById(form.id());
		formApi.evictFormCache(formId);
		formApi.evictTenantForms(form.tenantId());

		log.info("Deleted form ID: {}", formId);
		return "redirect:/dashboard?msg=Successfully deleted form: " + form.name();
	}
}