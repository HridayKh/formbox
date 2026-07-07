package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
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
public class FormController {

	private final TenantRepository tenantRepository;
	private final FormRepository formRepository;
	private final SubmissionCacheService submissionCacheService;
	private final TenantTierCacheService tenantTierCacheService;
	private final FormCacheService formCacheService;

	public FormController(TenantRepository tenantRepository, FormRepository formRepository, SubmissionCacheService submissionCacheService, TenantTierCacheService tenantTierCacheService, FormCacheService formCacheService) {
		this.tenantRepository = tenantRepository;
		this.formRepository = formRepository;
		this.submissionCacheService = submissionCacheService;
		this.tenantTierCacheService = tenantTierCacheService;
		this.formCacheService = formCacheService;
	}


	// ========== FORM CRUD ==========
	@PostMapping
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, HttpServletResponse response) {
		String msgParam = "";

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		if ("free-v1".equals(tenantTierCacheService.resolveHighestActiveTierNonNull(tenantId)) && redirectUrl != null && !redirectUrl.isBlank()) {
			redirectUrl = null;
			msgParam = "?msg=upgrade_required_for_redirect";
		}
		Form newForm = new Form();
		newForm.setTenant(tenantRepository.getReferenceById(tenantId));
		newForm.setName(name);
		newForm.setRedirectUrl(redirectUrl);
		Form savedForm = formRepository.save(newForm);

		formCacheService.updateFormCache(savedForm);
		formCacheService.evictTenantForms(tenantId);

		response.setHeader("HX-Redirect", PathRegistry.Form.BASE + "/" + savedForm.getId() + msgParam);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@GetMapping
	public String listForms(@RequestAttribute JwtPayload userMetadata, Model model) {
		String tenantId = userMetadata.getSub();
		if (tenantId == null) return "redirect:" + PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		List<CachedForm> forms = formCacheService.getTenantForms(UUID.fromString(tenantId));
		model.addAttribute("forms", forms);
		return ViewRegistry.Fragments.FORM_ROWS;
	}

	@GetMapping("/{formId}")
	public String manageForm(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(value = "msg", required = false) String msg, Model model) {
		CachedForm form = formCacheService.getCachedForm(formId);

		if (!form.tenantId().toString().equals(userMetadata.getSub()))
			throw new RuntimeException("Unauthorized access to form system.");

		if ("upgrade_required_for_redirect".equals(msg))
			model.addAttribute("warningMessage", "Form created successfully! However, custom redirects are only available on paid tiers.");

		FormSubmissionsResponse submissions = submissionCacheService.getFormSubmissionsGrouped(formId);

		model.addAttribute("form", form);
		model.addAttribute("tier", tenantTierCacheService.resolveHighestActiveTierNonNull(form.tenantId()));
		model.addAttribute("submissions", submissions.submissions());
		model.addAttribute("spamSubmissions", submissions.spam());

		return "dashboard/manage-form";
	}

	@PutMapping("/{id}")
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, @RequestParam(value = "isActive", required = false) Boolean isActive, Model model) {
		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));

		if (!form.getTenant().getId().toString().equals(userMetadata.getSub()))
			throw new RuntimeException("Unauthorized access to form system.");

		boolean tierViolationAttempted = false;
		if ("free-v1".equals(tenantTierCacheService.resolveHighestActiveTierNonNull(form.getTenant().getId())) && redirectUrl != null && !redirectUrl.isBlank()) {
			redirectUrl = null;
			tierViolationAttempted = true;
		}

		form.setName(name);
		form.setRedirectUrl(redirectUrl == null || redirectUrl.isBlank() ? null : redirectUrl);
		form.setIsActive(isActive != null && isActive);
		Form savedForm = formRepository.save(form);

		formCacheService.updateFormCache(savedForm);
		formCacheService.evictTenantForms(savedForm.getTenant().getId());

		model.addAttribute("form", savedForm);


		// 3. Contextual UI Messages based on behavior
		if (tierViolationAttempted) {
			model.addAttribute("warningMessage", "Settings updated, but custom redirects require a premium upgrade!");
		} else {
			model.addAttribute("message", "Form configurations updated successfully!");
		}

		return ViewRegistry.Fragments.SETTINGS;
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId) {
		CachedForm form = formCacheService.getCachedForm(formId);
		if (!form.tenantId().toString().equals(userMetadata.getSub())) return;
		formRepository.deleteById(form.id());
		formCacheService.evictFormCache(formId);
		formCacheService.evictTenantForms(form.tenantId());

	}

}