package formbox.submission.internal;

import formbox.shared.CacheNames;
import formbox.form.FormDto;
import formbox.shared.RedisCache;
import formbox.submission.SubmissionApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
class FormSubmissionService {

	private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
		// Pictures / Images
		"image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml", "image/vnd.microsoft.icon", "image/tiff", "image/bmp",
		// Documents & Office Files
		"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/vnd.oasis.opendocument.text",
		// Basic & Text Files
		"text/plain", "text/csv", "text/html", "text/css", "application/json", "application/xml",
		// Archives & Compressed Files
		"application/zip", "application/vnd.rar", "application/x-tar", "application/gzip", "application/x-7z-compressed",
		// Audio & Video
		"audio/mpeg", "audio/wav", "video/mp4", "video/mpeg", "video/webm");

	private final SubmissionRepository submissionRepository;
	private final SubmissionApi submissionApi;
	private final RedisCache redisCache;

	// step 2: per form rate limit (error 429)
	@WithSpan
	public boolean rateLimitPassed(UUID formId, Integer rpm) {
		return redisCache.increment(CacheNames.FORM_RATE_LIMIT_RPM, formId.toString(), Duration.ofMinutes(1)).map(c -> c <= rpm).orElse(false);
	}

	// step 4: check if content type allowed
	public boolean isContentTypeJson(HttpServletRequest request) {
		String contentType = request.getContentType();
		String accept = request.getHeader("Accept");
		return (contentType != null && contentType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) || (accept != null && accept.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE));
	}

	// step 5: check honeypot
	// step 6: check turnstile
	// step 10: save form payload and metadata
	@WithSpan
	@Async
	public void asyncSaveSubmission(UUID formId, UUID tenantId, String remoteAddr, Map<String, String> payload, boolean isSpam) {
		var s = new Submission(formId, tenantId, payload, remoteAddr, isSpam);
		submissionRepository.save(s);
		submissionApi.updateFormSubmissionsCache(formId, s.toSubmissionItem());
	}

	// step 8: abort request if invalid mime type on file (error 400)
	@WithSpan
	public boolean filesHaveValidMimeTypes(HttpServletRequest request) {
		if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
			log.debug("Request is not a multipart form submission; skipping file validation.");
			return true;
		}

		try {
			Collection<Part> parts = request.getParts();
			if (parts == null || parts.isEmpty()) {
				return true;
			}
			for (Part part : parts) {
				if (part.getSubmittedFileName() == null || part.getSubmittedFileName().isBlank())
					continue;
				String contentType = part.getContentType();
				if (contentType == null || contentType.isBlank()) continue;
				if (!ALLOWED_MIME_TYPES.contains(contentType.trim().toLowerCase())) {
					log.warn("Invalid MIME type detected: {} for file field: {}", contentType, part.getName());
					return false;
				}
			}

			return true;
		} catch (IOException | ServletException e) {
			log.error("Failed to parse file parts from HttpServletRequest", e);
			return true;
		}
	}

	// step 9: check custom filters and validations (error 400)
	public boolean validateFields(Map<String, String> ignoredPayload, FormDto ignoredForm) {
		return true;
	}

}

