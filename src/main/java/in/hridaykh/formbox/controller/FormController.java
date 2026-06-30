package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.AuthServiceKt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.Form.BASE)
public class FormController {

	private final AuthServiceKt authServiceKt;
	private final TenantRepository tenantRepository;
	private final FormRepository formRepository;

	public FormController(AuthServiceKt authServiceKt, TenantRepository tenantRepository, FormRepository formRepository) {
		this.authServiceKt = authServiceKt;
		this.tenantRepository = tenantRepository;
		this.formRepository = formRepository;
	}

	@GetMapping
	public String listForms(@CookieValue(name = "sb_token", required = false) String token, Model model) {
		model.addAttribute("forms", formRepository.findByTenantAndIsDeletedIsFalse(resolveTenant(token)));
		return "fragments/form-list :: form-rows";
	}

	@PostMapping
	public String createForm(@CookieValue(name = "sb_token", required = false) String token, @RequestParam("name") String name, @RequestParam(value = "redirectUrl", required = false) String redirectUrl, Model model) {

		Tenant tenant = resolveTenant(token);

		formRepository.save(new Form(tenant, name, redirectUrl));

		List<Form> forms = formRepository.findByTenantAndIsDeletedIsFalse(tenant);
		model.addAttribute("forms", forms);

		return "fragments/form-list :: form-rows";
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public void deleteForm(@CookieValue(name = "sb_token", required = false) String token, @PathVariable("id") UUID formId) {
		Form form = formRepository.findById(formId).orElseThrow(() -> new RuntimeException("Form not found"));
		if (form.compareTenant(resolveTenant(token)))
			formRepository.delete(form);
	}

	private Tenant resolveTenant(String token) {
		if (token == null || token.isBlank()) throw new RuntimeException("Unauthorized");
		var userMetadata = authServiceKt.getUserMetadata(token);
		if (userMetadata.getSub() == null)
			throw new RuntimeException("Tenant not found");
		UUID userId = UUID.fromString(userMetadata.getSub());
		return tenantRepository.findById(userId).orElseThrow(() -> new RuntimeException("Tenant not found"));
	}
}