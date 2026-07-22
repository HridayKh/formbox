package in.hridaykh.formbox.service.form;

import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.entity.Submission;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import in.hridaykh.formbox.service.cache.SubmissionCacheService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
public class FormSubmissionService {

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

	private final StringRedisTemplate redis;
	private final FormRepository formRepository;
	private final SubmissionRepository submissionRepository;
	private final SubmissionCacheService submissionCacheService;

	// step 2: per form rate limit (error 429)
	@WithSpan
	public boolean rateLimitPassed(UUID formId, Integer rateLimitRpm) {
		String redisKey = String.format("formbox:%s:%s", CacheNames.FORM_RATE_LIMIT_RPM, formId);
		try {
			Long currentCount = redis.opsForValue().increment(redisKey);
			if (currentCount == null) {
				log.error("Redis increment returned null for key: {}", redisKey);
				return false;
			}
			if (currentCount == 1) redis.expire(redisKey, Duration.ofMinutes(1));
			return currentCount <= rateLimitRpm;
		} catch (Exception e) {
			log.error("Failed to execute rate limit logic in Redis for key: {}", redisKey, e);
			return false;
		}
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
	public void asyncSaveSubmission(UUID formId, String remoteAddr, Map<String, String> payload, boolean isSpam) {
		var s = new Submission(formRepository.getReferenceById(formId), payload, remoteAddr, isSpam);
		submissionRepository.save(s);
		submissionCacheService.updateFormSubmissionsCache(formId, s.toSubmissionItem());
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
	public boolean validateFields(Map<String, String> ignoredPayload, CachedForm ignoredForm) {
		return true;
	}

}

