package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.exception.FormNotFoundException;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.service.FormFileService;
import in.hridaykh.formbox.service.FormSubmissionService;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.service.polar.PolarCacheService;
import in.hridaykh.formbox.service.cache.TenantTierCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

	private final FormSubmissionService formSubmissionService;
	private final PolarCacheService polarCacheService;
	private final TenantTierCacheService tenantTierCacheService;
	private final FormCacheService formCacheService;
	private final FormFileService formFileService;

	@GetMapping("/")
	public String index(@RequestAttribute(required = false) JwtPayload userMetadata, Model model) {
		boolean isLoggedIn = userMetadata != null && userMetadata.getSub() != null;
		log.trace("Landing index page evaluated. Auth active status: {}", isLoggedIn);
		model.addAttribute("loggedIn", isLoggedIn);
		return ViewRegistry.INDEX;
	}

	@GetMapping(PathRegistry.WAITLIST)
	public String waitlist(@RequestParam(required = false) String ignoredEmail) {
//		log.error("WHO ENTERED THE WAITLIST BURH\nIncoming request to join product waitlist allocation for email placeholder: {}", ignoredEmail, new Exception("WAITLIST"));
		formFileService.uploadFilesAndInitNotifsWebhooks(null, null, null);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping("/f/{formId}")
	public String submission(@PathVariable UUID formId, @RequestParam Map<String, String> payload, HttpServletRequest request, HttpServletResponse response) {
		log.debug("Processing incoming webhook submission request path channel for form ID: {}", formId);

		// step 1: get form (404 if db request says form doesn't exist)
		CachedForm form;
		try {
			form = formCacheService.getCachedForm(formId);
		} catch (FormNotFoundException e) {
			log.warn("Submission rejected. Form destination completely missing from structural records for ID: {}", formId);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return "submit/form-not-found";
		}

		// step 2: per form rate limit (error 429)
		if (!formSubmissionService.checkRateLimit(formId, form.rateLimitRpm())) {
			response.setStatus(429);
			return "submit/rate-limit";
		}

		// step 3: check submissions quota
		if (polarCacheService.getCachedSubmissionBalance(form.tenantId()) <= 0) {
			response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
			return "submit/out-of-submissions";
		}

		// step 4: check if content type allowed
		if (request.getHeader("content-type").equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE) && !form.allowJson())
			return "submit/json-not-allowed"; // TODO: Check for htmx and add null checks this one

		// step 5: check honeypot
		if (!payload.getOrDefault(form.honeypotName(), "").isBlank()) {
			formSubmissionService.saveSpamSubmission(form.id(), request.getRemoteAddr(), payload);
			return "submit/thanks";
		}

		// step 6: check turnstile
		String turnstileSecretKey = form.turnstileSecretKey();
		if (!formSubmissionService.verifyTurnstile(payload, turnstileSecretKey)) {
			formSubmissionService.saveSpamSubmission(form.id(), request.getRemoteAddr(), payload);
			return "submit/thanks";
		}

		// step 7: abort request if files not allowed (error 400)
		if (!form.allowFiles()) {
			try {
				request.getParts();
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "submit/files-not-allowed";
			} catch (Exception _) {
			}
		}

		// step 8: 8. abort request if invalid mime type on file (error 400)
		if (!formSubmissionService.filesHaveValidMimeTypes(request)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return "submit/files-not-allowed";
		}

		// step 9: check custom filters and validations (error 400)
		if (!formSubmissionService.vaildateFields(payload, form)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return "submit/invalid-fields";
		}

		// step 10: save form payload and metadata
		formSubmissionService.saveSpamSubmission(form.id(), request.getRemoteAddr(), payload);

		// step 11: update leftover submission balance
		polarCacheService.decrementCachedSubmissionBalance(form.tenantId());

		// step 12: update form submissions cache
		formCacheService.evictFormCache(formId);

		// step 14: async start upload files/attachments
		// step 15: async 3rd party webhooks and notifs
		formFileService.uploadFilesAndInitNotifsWebhooks(form, payload, request);

		log.debug("processed the form!!!!!!!!!");

		// step 13: return 200 ok
		return getValidReturnForSubmission(form);
	}

	private String getValidReturnForSubmission(CachedForm form) {
		return "submit/thanks";
	}
}