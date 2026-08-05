package formbox.submission.internal;

import formbox.billing.PolarSubmissionApi;
import formbox.shared.FormNotFoundException;
import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.shared.TurnstileVerifierUtil;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
class SubmissionController {

	private final FormSubmissionService submissionService;
	private final FormApi formApi;
	private final ObjectMapper objectMapper;
	private final PolarSubmissionApi polarSubmissionApi;


	@PostMapping("/f/{formId}")
	@WithSpan
	public String submission(@PathVariable UUID formId, @RequestParam Map<String, String> payload, HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.debug("Handling form submission for form ID: {}", formId);

		String userAgent = request.getHeader("User-Agent");

		Sentry.metrics().count(SubmissionMetrics.ANY_SUBMISSION);
		Sentry.configureScope(scope -> {
			scope.setTag("formId", formId.toString());
			scope.setTag("userAgent", userAgent != null ? userAgent : "unknown");
			scope.setTag("contentType", request.getContentType());
		});

		FormDto form;
		try {
			form = formApi.getFormDto(formId);
		} catch (FormNotFoundException e) {
			log.info("Submission rejected. Form ID {} not found.", formId);
			Sentry.addBreadcrumb("Form not found for formId " + formId);
			Sentry.metrics().count(SubmissionMetrics.Failed.FORM_NOT_FOUND);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return "submit/form-not-found";
		}

		Sentry.configureScope(scope -> scope.setTag("tenantId", form.tenantId().toString()));

		double payloadFieldCount = payload.size();
		double payloadSizeBytes = payload.entrySet().stream().mapToLong(e -> e.getKey().length() + (e.getValue() == null ? 0 : e.getValue().length())).sum();

		Sentry.metrics().distribution(SubmissionMetrics.PAYLOAD_FIELD_COUNT, payloadFieldCount);
		Sentry.metrics().distribution(SubmissionMetrics.PAYLOAD_SIZE_BYTES, payloadSizeBytes);

		if (!submissionService.rateLimitPassed(formId, form.rateLimitRpm())) {
			Sentry.addBreadcrumb("Rate limit exceeded for form " + formId);
			Sentry.metrics().count(SubmissionMetrics.Failed.RATE_LIMIT_PASSED);
			response.setStatus(429);
			return "submit/rate-limit";
		}

		if (polarSubmissionApi.getCachedSubmissionBalance(form.tenantId()) <= 0) {
			Sentry.addBreadcrumb("Submission balance exhausted for tenant " + form.tenantId());
			Sentry.metrics().count(SubmissionMetrics.Failed.OUT_OF_SUBMISSIONS);
			response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
			return "submit/out-of-submissions";
		}

		boolean isContentTypeJson = submissionService.isContentTypeJson(request);
		if (!form.allowJson() && isContentTypeJson) {
			Sentry.addBreadcrumb("JSON content type rejected for form " + formId);
			Sentry.metrics().count(SubmissionMetrics.Failed.JSON_NOT_ALLOWED);
			return "submit/json-not-allowed";
		}

		if (!payload.getOrDefault(form.honeypotName(), "").isBlank()) {
			Sentry.addBreadcrumb("Honeypot field populated for form " + formId);
			submissionService.saveSubmission(form.id(), form.tenantId(), request.getRemoteAddr(), payload, true, request);
			Sentry.metrics().count(SubmissionMetrics.Failed.HONEYPOT);
			return "submit/thanks";
		}

		if (TurnstileVerifierUtil.turnstileFailed(payload, form.turnstileSecretKey(), objectMapper)) {
			Sentry.addBreadcrumb("Turnstile verification failed for form " + formId);
			submissionService.saveSubmission(form.id(), form.tenantId(), request.getRemoteAddr(), payload, true, request);
			Sentry.metrics().count(SubmissionMetrics.Failed.TURNSTILE);
			return "submit/thanks";
		}

		if (!form.allowFiles()) {
			try {
				Collection<Part> parts = request.getParts();
				if (parts != null && !parts.isEmpty()) {
					Sentry.addBreadcrumb("Files rejected for form " + formId + " (files not allowed)");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					Sentry.metrics().count(SubmissionMetrics.Failed.FILES_NOT_ALLOWED);
					return "submit/files-not-allowed";
				}
			} catch (Exception e) {
				log.warn("Failed to read request parts for form {}: {}", formId, e.getMessage(), e);
				Sentry.addBreadcrumb("Error reading request parts for form " + formId);
				Sentry.metrics().count(SubmissionMetrics.Failed.PARTS_READ_ERROR);
			}
		}

		if (!submissionService.filesHaveValidMimeTypes(request)) {
			Sentry.addBreadcrumb("Invalid MIME type in files for form " + formId);
			Sentry.metrics().count(SubmissionMetrics.Failed.INVALID_MIME_TYPES);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return "submit/files-not-allowed";
		}

		if (!submissionService.validateFields(payload, form)) {
			Sentry.addBreadcrumb("Field validation failed for form " + formId);
			Sentry.metrics().count(SubmissionMetrics.Failed.INVALID_FIELDS);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return "submit/invalid-fields";
		}

		var submission = submissionService.saveSubmission(form.id(), form.tenantId(), request.getRemoteAddr(), payload, false, request);
		submissionService.asyncSendNotifs(form, submission, payload, request);
		polarSubmissionApi.asyncDecrementCachedSubmissionBalance(form.tenantId());

		log.info("Successfully processed submission for form ID: {}", formId);

		if (isContentTypeJson) {
			Sentry.metrics().count(SubmissionMetrics.SUCCESSFUL);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			return "submit/json-response";
		}

		if (form.redirectUrl() == null || form.redirectUrl().isBlank()) return "submit/thanks";

		return "redirect:" + form.redirectUrl();
	}

}

