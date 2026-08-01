package formbox.form.internal;

import formbox.folder.FolderApi;
import formbox.folder.FolderDto;
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
import org.springframework.transaction.annotation.Transactional;
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
	private final FolderApi folderApi;

	@PostMapping("/{folderId}")
	@WithSpan
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam String formName,
	                         @RequestParam(required = false) String redirectUrl, @PathVariable UUID folderId) {
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

		formApi.updateFormCache(savedForm.toCachedFormDto());
		formApi.evictTenantForms(tenantId);

		return "redirect:/forms/" + folderId + "/" + savedForm.getId() + "?msg=" + msg;
	}



	@PostMapping("/{folderId}/{formId}/move")
	@WithSpan
	@Transactional
	public String moveForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID folderId, @PathVariable UUID formId) {
		log.debug("Moving folderId: {} for user {}", folderId, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Optional<FolderDto> folderOpt = folderApi.getFolderById(folderId);
		if (folderOpt.isEmpty())
			return "redirect:/dashboard?msg=Folder not found!";
		FolderDto folder = folderOpt.get();
		if (!folder.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized form move attempt of folderId {} by user {}", folderId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid folderId";
		}

		Optional<Form> form = formRepository.findById(formId);
		if (form.isEmpty())
			return "redirect:/dashboard?msg=Form not found!";
		if (!form.get().getTenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized form move attempt of form {} by user {}", formId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid form";
		}

		Form f = form.get();
		f.setFolderId(folder.id());
		Form savedF = formRepository.save(f);
		log.info("Successfully moved form. ID: {} for tenant ID: {}", formId, tenantId);

		formApi.updateFormCache(savedF.toCachedFormDto());
		formApi.evictTenantForms(tenantId);

		String msg = "Successfully moved the form " + f.getName() + " to folderId " + folder.name();
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
		FormTierValidationResult result = formSettingsService.updateFormSettings(formId, userMetadata.getSub(), fullRequest);

		Entitlements entitlements = entitlementsApi.getEntitlements(UUID.fromString(Objects.requireNonNull(userMetadata.getSub())));
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

		return "fragments/manage/tab-settings :: settings-panel";
	}

	@DeleteMapping("/{ignoredFolderId}/{formId}")
	@ResponseBody
	@WithSpan
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @PathVariable String ignoredFolderId) {
		log.debug("Deleting form with ID: {}", formId);
		FormDto form = formApi.getCachedForm(formId);

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