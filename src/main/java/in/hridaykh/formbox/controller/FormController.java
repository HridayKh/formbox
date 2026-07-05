package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.PurchasesRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.service.SubmissionService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.Form.BASE)
public class FormController {

	private final TenantRepository tenantRepository;
	private final FormRepository formRepository;
	private final SubmissionService submissionService;
	private final PurchasesRepository purchasesRepository;

	public FormController(TenantRepository tenantRepository, FormRepository formRepository, SubmissionService submissionService, PurchasesRepository purchasesRepository) {
		this.tenantRepository = tenantRepository;
		this.formRepository = formRepository;
		this.submissionService = submissionService;
		this.purchasesRepository = purchasesRepository;
	}


	// ========== FORM CRUD ==========
	@PostMapping
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, HttpServletResponse response) {
		Tenant tenant = resolveTenant(userMetadata);
		String msgParam = "";

		if ("free-v1".equals(tenant.resolveHighestActiveTier(purchasesRepository)) && redirectUrl != null && !redirectUrl.isBlank()) {
			redirectUrl = null;
			msgParam = "?msg=upgrade_required_for_redirect";
		}
		Form newForm = new Form();
		newForm.setTenant(tenant);
		newForm.setName(name);
		newForm.setRedirectUrl(redirectUrl);
		Form savedForm = formRepository.save(newForm);

		// Append the message query parameter to the redirect path
		response.setHeader("HX-Redirect", PathRegistry.Form.BASE + "/" + savedForm.getId() + msgParam);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@GetMapping
	public String listForms(@RequestAttribute JwtPayload userMetadata, Model model) {
		model.addAttribute("forms", formRepository.findByTenantAndIsDeletedIsFalse(resolveTenant(userMetadata)));
		return ViewRegistry.Fragments.FORM_ROWS;
	}

	@GetMapping("/{id}")
	public String manageForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId, @RequestParam(value = "msg", required = false) String msg, Model model) {
		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));

		if (!form.compareTenant(resolveTenant(userMetadata))) {
			throw new RuntimeException("Unauthorized access to form system.");
		}

		if ("upgrade_required_for_redirect".equals(msg)) {
			model.addAttribute("warningMessage", "Form created successfully! However, custom redirects are only available on paid tiers.");
		}

		FormSubmissionsResponse submissions = submissionService.getFormSubmissionsGrouped(formId);

		model.addAttribute("form", form);
		model.addAttribute("tier", resolveTenant(userMetadata).resolveHighestActiveTier(purchasesRepository));
		model.addAttribute("submissions", submissions.submissions());
		model.addAttribute("spamSubmissions", submissions.spam());
		return "dashboard/manage-form";
	}

	@PutMapping("/{id}")
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, @RequestParam(value = "isActive", required = false) Boolean isActive, Model model) {
		Tenant tenant = resolveTenant(userMetadata);
		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));

		if (!form.compareTenant(tenant))
			throw new RuntimeException("Unauthorized access to form system.");

		// 1. Enforce business rule check on updates
		boolean tierViolationAttempted = false;
		if ("free-v1".equals(tenant.resolveHighestActiveTier(purchasesRepository)) && redirectUrl != null && !redirectUrl.isBlank()) {
			redirectUrl = null;
			tierViolationAttempted = true;
		}

		// 2. Safe variable mapping
		form.setName(name);
		form.setRedirectUrl(redirectUrl == null || redirectUrl.isBlank() ? null : redirectUrl);
		form.setIsActive(isActive != null && isActive);

		formRepository.save(form);
		model.addAttribute("form", form);

		// 3. Contextual UI Messages based on behavior
		if (tierViolationAttempted) {
			model.addAttribute("warningMessage", "Settings updated, but custom redirects require a premium upgrade!");
		} else {
			model.addAttribute("message", "Form configurations updated successfully!");
		}

		return ViewRegistry.Fragments.SETTINGS ;
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public void deleteForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId) {
		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));
		if (form.compareTenant(resolveTenant(userMetadata))) formRepository.delete(form);
	}

	// ========== PRIVATE HELPERS ==========
	private Tenant resolveTenant(JwtPayload userMetadata) {
		if (userMetadata == null) throw new RuntimeException("Unauthorized");
		if (userMetadata.getSub() == null) throw new RuntimeException("Tenant not found");
		UUID userId = UUID.fromString(userMetadata.getSub());
		return tenantRepository.findById(userId).orElseThrow(() -> new RuntimeException("Tenant not found"));
	}
}