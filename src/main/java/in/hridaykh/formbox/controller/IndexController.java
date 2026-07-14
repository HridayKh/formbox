package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.Tiers;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.exception.FormNotFoundException;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.service.FormFileService;
import in.hridaykh.formbox.service.FormSubmissionService;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.service.cache.TenantCacheService;
import in.hridaykh.formbox.service.polar.PolarCacheService;
import in.hridaykh.formbox.util.TurnstileVerifier;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

	private final FormSubmissionService submissionService;
	private final PolarCacheService polarCacheService;
	private final FormCacheService formCacheService;
	private final FormFileService formFileService;
	private final ObjectMapper objectMapper;
	private final TenantCacheService tenantCacheService;

	@GetMapping("/")
	public String index(@RequestAttribute(required = false) JwtPayload userMetadata, Model model) {
		model.addAttribute("loggedIn", userMetadata != null && userMetadata.getSub() != null);
		return ViewRegistry.INDEX;
	}

	@PostMapping("/f/{formId}")
	public String submission(@PathVariable UUID formId, @RequestParam Map<String, String> payload, HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.debug("Processing incoming webhook submission request path channel for form ID: {}", formId);

		// step 1: get form (404 if db request says form doesn't exist)
		CachedForm form;
		try {
			form = formCacheService.getCachedForm(formId);
		} catch (FormNotFoundException e) {
			log.info("Submission rejected. Form destination completely missing from structural records for ID: {}", formId);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return "submit/form-not-found";
		}

		// step 2: per form rate limit (error 429)
		if (!submissionService.rateLimitPassed(formId, form.rateLimitRpm())) {
			response.setStatus(429);
			return "submit/rate-limit";
		}

		// step 3: check submissions quota
		if (polarCacheService.getCachedSubmissionBalance(form.tenantId()) <= 0) {
			response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
			return "submit/out-of-submissions";
		}

		// step 4: check if content type allowed
		boolean isContentTypeJson = submissionService.isContentTypeJson(request);
		if (!form.allowJson() && isContentTypeJson)
			return "submit/json-not-allowed";

		// step 5: check honeypot
		if (!payload.getOrDefault(form.honeypotName(), "").isBlank()) {
			submissionService.saveSubmission(form.id(), request.getRemoteAddr(), payload, true);
			return "submit/thanks";
		}

		// step 6: check turnstile
		String turnstileSecretKey = form.turnstileSecretKey();
		if (TurnstileVerifier.turnstileFailed(payload, turnstileSecretKey, objectMapper)) {
			submissionService.saveSubmission(form.id(), request.getRemoteAddr(), payload, true);
			return "submit/thanks";
		}

		// step 7: abort request if files not allowed (error 400)
		if (!form.allowFiles()) {
			try {
				Collection<Part> parts = request.getParts();
				if (parts != null && !parts.isEmpty()) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return "submit/files-not-allowed";
				}
			} catch (Exception _) {
			}
		}

		// step 8: abort request if invalid mime type on file (error 400)
		if (!submissionService.filesHaveValidMimeTypes(request)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return "submit/files-not-allowed";
		}

		// step 9: check custom filters and validations (error 400)
		if (!submissionService.validateFields(payload, form)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return "submit/invalid-fields";
		}

		// step 10: save form payload and metadata
		submissionService.saveSubmission(form.id(), request.getRemoteAddr(), payload, false);

		// step 11: update leftover submission balance
		polarCacheService.decrementCachedSubmissionBalance(form.tenantId());

		// step 13: async start upload files/attachments
		// step 14: async 3rd party webhooks and notifs
		formFileService.uploadFilesAndInitNotifsWebhooks(form, payload, request);

		log.debug("processed the form!!!!!!!!!");

		// step 12: return 200 ok
		if (isContentTypeJson) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			return "submit/json-response";
		}
		String tier = tenantCacheService.resolveHighestActiveTierNonNull(form.tenantId());
		if (form.redirectUrl() == null || form.redirectUrl().isBlank() || !Tiers.t(tier).redirectUrlAllowed())
			return "submit/thanks";
		return "redirect:" + form.redirectUrl();
	}
}