package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.exception.FormNotFoundException;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import in.hridaykh.formbox.service.form.FormFileService;
import in.hridaykh.formbox.service.form.FormSubmissionService;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.billing.service.PolarCacheService;
import in.hridaykh.formbox.util.TurnstileVerifier;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
	private final EntitlementsCacheService entitlementsCacheService;

	@GetMapping("/")
	@WithSpan
	public String index(@RequestAttribute(required = false) JwtPayload userMetadata, Model model) {
		model.addAttribute("loggedIn", userMetadata != null && userMetadata.getSub() != null);
		return ViewRegistry.INDEX;
	}

	@PostMapping("/f/{formId}")
	@WithSpan
	public String submission(@PathVariable UUID formId, @RequestParam Map<String, String> payload, HttpServletRequest request, HttpServletResponse response) throws IOException {
		long startTime = System.currentTimeMillis();
		long stepStart;

		log.debug("Processing incoming webhook submission request path channel for form ID: {}", formId);

		try {
			// step 1: get form (404 if db request says form doesn't exist)
			stepStart = System.currentTimeMillis();
			CachedForm form;
			try {
				form = formCacheService.getCachedForm(formId);
				log.debug("Step 1 (Get Form) took {} ms", System.currentTimeMillis() - stepStart);
			} catch (FormNotFoundException e) {
				log.debug("Step 1 (Get Form - FormNotFound) took {} ms", System.currentTimeMillis() - stepStart);
				log.warn("Submission rejected. Form ID {} not found.", formId);
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return "submit/form-not-found";
			}

			// step 2: per form rate limit (error 429)
			stepStart = System.currentTimeMillis();
			boolean rateLimitPassed = submissionService.rateLimitPassed(formId, form.rateLimitRpm());
			log.debug("Step 2 (Rate Limit check) took {} ms", System.currentTimeMillis() - stepStart);
			if (!rateLimitPassed) {
				response.setStatus(429);
				return "submit/rate-limit";
			}

			// step 3: check submissions quota
			stepStart = System.currentTimeMillis();
			long balance = polarCacheService.getCachedSubmissionBalance(form.tenantId());
			log.debug("Step 3 (Quota check) took {} ms", System.currentTimeMillis() - stepStart);
			if (balance <= 0) {
				response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
				return "submit/out-of-submissions";
			}

			// step 4: check if content type allowed
			stepStart = System.currentTimeMillis();
			boolean isContentTypeJson = submissionService.isContentTypeJson(request);
			log.debug("Step 4 (Content-Type check) took {} ms", System.currentTimeMillis() - stepStart);
			if (!form.allowJson() && isContentTypeJson)
				return "submit/json-not-allowed";

			// step 5: check honeypot
			stepStart = System.currentTimeMillis();
			boolean isHoneypot = !payload.getOrDefault(form.honeypotName(), "").isBlank();
			if (isHoneypot) {
				submissionService.asyncSaveSubmission(form.id(), request.getRemoteAddr(), payload, true);
				log.debug("Step 5 (Honeypot caught & saved) took {} ms", System.currentTimeMillis() - stepStart);
				return "submit/thanks";
			}
			log.debug("Step 5 (Honeypot check passed) took {} ms", System.currentTimeMillis() - stepStart);

			// step 6: check turnstile
			stepStart = System.currentTimeMillis();
			String turnstileSecretKey = form.turnstileSecretKey();
			boolean turnstileFailed = TurnstileVerifier.turnstileFailed(payload, turnstileSecretKey, objectMapper);
			if (turnstileFailed) {
				submissionService.asyncSaveSubmission(form.id(), request.getRemoteAddr(), payload, true);
				log.debug("Step 6 (Turnstile failed & saved) took {} ms", System.currentTimeMillis() - stepStart);
				return "submit/thanks";
			}
			log.debug("Step 6 (Turnstile verification passed) took {} ms", System.currentTimeMillis() - stepStart);

			// step 7: abort request if files not allowed (error 400)
			stepStart = System.currentTimeMillis();
			if (!form.allowFiles()) {
				try {
					Collection<Part> parts = request.getParts();
					if (parts != null && !parts.isEmpty()) {
						log.debug("Step 7 (File check - forbidden) took {} ms", System.currentTimeMillis() - stepStart);
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						return "submit/files-not-allowed";
					}
				} catch (Exception e) {
					log.trace("Suppressed content parse failure context check. Client sent no multi-part payload structure.");
				}
			}
			log.debug("Step 7 (File check passed) took {} ms", System.currentTimeMillis() - stepStart);

			// step 8: abort request if invalid mime type on file (error 400)
			stepStart = System.currentTimeMillis();
			boolean validMime = submissionService.filesHaveValidMimeTypes(request);
			log.debug("Step 8 (MIME type check) took {} ms", System.currentTimeMillis() - stepStart);
			if (!validMime) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "submit/files-not-allowed";
			}

			// step 9: check custom filters and validations (error 400)
			stepStart = System.currentTimeMillis();
			boolean validFields = submissionService.validateFields(payload, form);
			log.debug("Step 9 (Field validations) took {} ms", System.currentTimeMillis() - stepStart);
			if (!validFields) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "submit/invalid-fields";
			}

			// step 10: save form payload and metadata
			stepStart = System.currentTimeMillis();
			submissionService.asyncSaveSubmission(form.id(), request.getRemoteAddr(), payload, false);
			log.debug("Step 10 (Save payload) took {} ms", System.currentTimeMillis() - stepStart);

			// step 11: update leftover submission balance
			stepStart = System.currentTimeMillis();
			polarCacheService.asyncDecrementCachedSubmissionBalance(form.tenantId());
			log.debug("Step 11 (Decrement quota balance) took {} ms", System.currentTimeMillis() - stepStart);

			// step 13: async start upload files/attachments
			// step 14: async 3rd party webhooks and notifs
			stepStart = System.currentTimeMillis();
			formFileService.uploadFilesAndInitNotifsWebhooks(form, payload, request);
			log.debug("Steps 13 & 14 (Async upload & webhook init) took {} ms", System.currentTimeMillis() - stepStart);

			log.info("Successfully processed submission for form ID: {}", formId);

			// step 12: return 200 ok
			if (isContentTypeJson) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json");
				return "submit/json-response";
			}

			stepStart = System.currentTimeMillis();
			Entitlements entitlements = entitlementsCacheService.getEntitlements(form.tenantId());
			log.debug("Entitlements check took {} ms", System.currentTimeMillis() - stepStart);

			if (form.redirectUrl() == null || form.redirectUrl().isBlank() || !entitlements.redirectUrlsAllowed())
				return "submit/thanks";

			return "redirect:" + form.redirectUrl();
		} finally {
			log.info("TOTAL request execution time for form ID {}: {} ms", formId, System.currentTimeMillis() - startTime);
		}
	}
}