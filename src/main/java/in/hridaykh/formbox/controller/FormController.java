package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
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

	public FormController(TenantRepository tenantRepository, FormRepository formRepository) {
		this.tenantRepository = tenantRepository;
		this.formRepository = formRepository;
	}


	// ========== FORM CRUD ==========
	@PostMapping
	public String createForm(@RequestAttribute JwtPayload userMetadata, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, HttpServletResponse response) {

		Tenant tenant = resolveTenant(userMetadata);
		Form savedForm = formRepository.save(new Form(tenant, name, redirectUrl));

		// Instruct HTMX to push the user directly to the new form management screen
		response.setHeader("HX-Redirect", PathRegistry.Form.BASE + "/" + savedForm.getId());

		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@GetMapping
	public String listForms(@RequestAttribute JwtPayload userMetadata, Model model) {
		model.addAttribute("forms", formRepository.findByTenantAndIsDeletedIsFalse(resolveTenant(userMetadata)));
		return "fragments/form-list :: form-rows";
	}

	@GetMapping("/{id}")
	public String manageForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId, Model model) {

		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));

		if (!form.compareTenant(resolveTenant(userMetadata))) {
			throw new RuntimeException("Unauthorized access to form system.");
		}

		model.addAttribute("form", form);
		return "dashboard/manage-form"; // Returns the empty view skeleton
	}

	@PutMapping("/{id}")
	public String updateForm(@RequestAttribute JwtPayload userMetadata, @PathVariable("id") UUID formId, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, @RequestParam(value = "isActive", required = false) Boolean isActive, Model model) {

		Tenant tenant = resolveTenant(userMetadata);
		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));

		if (!form.compareTenant(tenant)) {
			throw new RuntimeException("Unauthorized access to form system.");
		}

		// Apply variations from parameters
		form.setName(name);
		form.setRedirectUrl(redirectUrl == null || redirectUrl.isBlank() ? null : redirectUrl);
		// Handle checkboxes safely (checkboxes are missing from parameters if unchecked)
		form.setIsActive(isActive != null && isActive);

		formRepository.save(form);

		model.addAttribute("form", form);
		model.addAttribute("message", "Form configurations updated successfully!");

		// Re-render the form fragments container inside the configuration workspace
		return "ashboard/manage-form :: settings-panel";
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