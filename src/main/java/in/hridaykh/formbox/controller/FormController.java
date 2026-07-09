package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.Tiers;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.service.cache.SubmissionCacheService;
import in.hridaykh.formbox.service.cache.TenantTierCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping(PathRegistry.Form.BASE)
@RequiredArgsConstructor
public class FormController {

	private final TenantRepository tenantRepository;
	private final FormRepository formRepository;
	private final SubmissionCacheService submissionCacheService;
	private final TenantTierCacheService tenantTierCacheService;
	private final FormCacheService formCacheService;

	@PostMapping
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, HttpServletResponse response) {
		log.debug("Processing request to create a new form. Name: [{}], Requested Redirect URL: [{}]", name, redirectUrl);
		String msgParam = "";

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		String highestTier = tenantTierCacheService.resolveHighestActiveTierNonNull(tenantId);

		if (!Tiers.t(highestTier).redirectUrlAllowed() && redirectUrl != null && !redirectUrl.isBlank()) {
			log.warn("Tier constraint violation intercepted. Free tier tenant: {} attempted custom redirect validation rules.", tenantId);
			redirectUrl = null;
			msgParam = "?msg=upgrade_required_for_redirect";
		}

		Form newForm = new Form();
		newForm.setTenant(tenantRepository.getReferenceById(tenantId));
		newForm.setName(name);
		newForm.setRedirectUrl(redirectUrl);

		Form savedForm = formRepository.save(newForm);
		log.info("Successfully persisted new Form Entity. ID: {} for tenant ID: {}", savedForm.getId(), tenantId);

		formCacheService.updateFormCache(savedForm);
		formCacheService.evictTenantForms(tenantId);

		response.setHeader("HX-Redirect", PathRegistry.Form.BASE + "/" + savedForm.getId() + msgParam);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@GetMapping
	public String listForms(@RequestAttribute JwtPayload userMetadata, Model model) {
		String tenantId = userMetadata.getSub();
		log.trace("Processing request to render forms row table layout map context for user reference: {}", tenantId);

		if (tenantId == null) {
			log.warn("Forms retrieval denied. Intercepted request thread missing user target metadata properties.");
			return "redirect:" + PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}

		List<CachedForm> forms = formCacheService.getTenantForms(UUID.fromString(tenantId));
		log.debug("Loaded {} forms from cache layers for tenant index ID: {}", forms.size(), tenantId);

		model.addAttribute("forms", forms);
		return ViewRegistry.Fragments.FORM_ROWS;
	}

	@GetMapping("/{formId}")
	public String manageForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(value = "msg", required = false) String msg, Model model) {
		log.debug("Loading primary console management data array structure for form ID: {} triggered by user: {}", formId, userMetadata.getSub());
		CachedForm form = formCacheService.getCachedForm(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.error("Security authorization intercept triggered. User: {} failed ownership check rule bounds for form ID: {} belonging to tenant: {}",
				userMetadata.getSub(), formId, form.tenantId());
			throw new RuntimeException("Unauthorized access to form system.");
		}

		if ("upgrade_required_for_redirect".equals(msg)) {
			log.trace("Injecting contextual warning constraint flag context into rendering engine stack variables.");
			model.addAttribute("warningMessage", "Form created successfully! However, custom redirects are only available on paid tiers.");
		}

		FormSubmissionsResponse submissions = submissionCacheService.getFormSubmissionsGrouped(formId);
		String currentTier = tenantTierCacheService.resolveHighestActiveTierNonNull(form.tenantId());

		log.trace("Aggregated presentation variables completely loaded for form context identifier: {}. Submissions collection count: {}, Spam flags matched: {}",
			formId, submissions.submissions().size(), submissions.spam().size());

		model.addAttribute("form", form);
		model.addAttribute("tier", currentTier);
		model.addAttribute("submissions", submissions.submissions());
		model.addAttribute("spamSubmissions", submissions.spam());

		return "dashboard/manage-form";
	}

	@PutMapping("/{id}")
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, @RequestParam(value = "isActive", required = false) Boolean isActive, Model model) {
		log.debug("Processing persistence updates mapping target parameters for form validation configuration modification ID: {}", formId);
		Form form = formRepository.findById(formId).orElseThrow(() -> {
			log.error("Target resource lookup mismatch error structure. Record completely missing for form entity updating target: {}", formId);
			return new RuntimeException("Form not found");
		});

		if (!form.getTenant().getId().toString().equals(userMetadata.getSub())) {
			log.error("Security authorization intercept triggered. User: {} failed structural access requirements updates checklist for resource context: {}", userMetadata.getSub(), formId);
			throw new RuntimeException("Unauthorized access to form system.");
		}

		boolean tierViolationAttempted = false;

		String subscriptionTier = tenantTierCacheService.resolveHighestActiveTierNonNull(form.getTenant().getId());
		if (!Tiers.t(subscriptionTier).redirectUrlAllowed() && redirectUrl != null && !redirectUrl.isBlank()) {
			log.warn("Intercepted invalid configuration upgrade tier parameter state. Dropping restricted input field variables for form: {}", formId);
			redirectUrl = null;
			tierViolationAttempted = true;
			model.addAttribute("warningMessage", "Settings updated, but custom redirects require a premium upgrade!");
		}

		form.setName(name);
		form.setRedirectUrl(redirectUrl == null || redirectUrl.isBlank() ? null : redirectUrl);
		form.setIsActive(isActive != null && isActive);

		Form savedForm = formRepository.save(form);
		log.info("Successfully updated configurations record layout properties mapping data for form ID: {}", formId);

		formCacheService.updateFormCache(savedForm);
		formCacheService.evictTenantForms(savedForm.getTenant().getId());

		model.addAttribute("form", savedForm);

		if (!tierViolationAttempted)
			model.addAttribute("message", "Form configurations updated successfully!");

		return ViewRegistry.Fragments.SETTINGS;
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId) {
		log.debug("Received explicit destruction request targeting form reference ID entity index mapping: {}", formId);
		CachedForm form = formCacheService.getCachedForm(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.error("Security authorization intercept triggered. Non-owner identity: {} denied deletion execution rights over form index payload: {}", userMetadata.getSub(), formId);
			return;
		}

		formRepository.deleteById(form.id());
		log.info("Form record data reference permanently dropped from standard database collections. ID: {}", formId);

		formCacheService.evictFormCache(formId);
		formCacheService.evictTenantForms(form.tenantId());
	}
}