package formbox.submission.internal;

import formbox.notifs.*;
import formbox.shared.CacheNames;
import formbox.form.FormDto;
import formbox.shared.Entitlements;
import formbox.shared.RedisCache;
import formbox.submission.SubmissionApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.ISpan;
import io.sentry.Sentry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

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
	private final UploadService uploadService;
	private final DiscordNotif discordNotif;
	private final SubmissionEmailsService submissionEmailsService;
	private final formbox.billing.StorageApi storageApi;

	@WithSpan
	public boolean rateLimitPassed(UUID formId, Integer rpm) {
		return redisCache.increment(CacheNames.FORM_RATE_LIMIT_RPM, formId.toString(), Duration.ofMinutes(1)).map(c -> c <= rpm).orElse(false);
	}

	public boolean isContentTypeJson(HttpServletRequest request) {
		String contentType = request.getContentType();
		String accept = request.getHeader("Accept");
		return (contentType != null && contentType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) || (accept != null && accept.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE));
	}

	@WithSpan
	@Transactional
	public Submission saveSubmission(UUID formId, UUID tenantId, Map<String, String> payload, boolean isSpam, HttpServletRequest request) {
		UploadResult uploadResult = isSpam ? new UploadResult(payload, 0L) : uploadFiles(request, payload);
		var savedSubmission = submissionRepository.save(new Submission(formId, tenantId, uploadResult.payload(), isSpam, uploadResult.totalBytes()));
		submissionApi.updateFormSubmissionsCache(formId, savedSubmission.toSubmissionItem());
		Sentry.configureScope(scope -> scope.setTag("submissionId", savedSubmission.getId().toString()));
		return savedSubmission;
	}

	private record UploadResult(Map<String, String> payload, long totalBytes) {}

	private UploadResult uploadFiles(HttpServletRequest request, Map<String, String> payload) {
		ISpan span = null;
		long totalBytes = 0;
		try {
			if (Sentry.getSpan() != null)
				span = Sentry.getSpan().startChild("SubmissionFileService.uploadFiles");
			if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
				log.debug("Skipping file upload for non multipart submission.");
				return new UploadResult(payload, 0L);
			}
			Collection<Part> parts = request.getParts();
			if (parts == null || parts.isEmpty()) {
				return new UploadResult(payload, 0L);
			}
			for (Part part : parts) {
				if (part.getSubmittedFileName() == null || part.getSubmittedFileName().isBlank())
					continue;
				String contentType = part.getContentType();
				if (contentType == null || contentType.isBlank()) continue;
				payload.put(part.getName(), part.getSubmittedFileName());
				payload.put(part.getName() + "__url", uploadService.uploadFile(part.getInputStream(), part.getSubmittedFileName(), part.getSize(), part.getContentType()));
				totalBytes += part.getSize();
				Sentry.metrics().distribution("submissions.stats.fileSizeBytes", part.getSize() * 1.0, "byte");
			}
			return new UploadResult(payload, totalBytes);
		} catch (IOException | ServletException e) {
			log.error("Failed to parse file parts from HttpServletRequest", e);
		} finally {
			if (span != null) span.finish();
		}
		return new UploadResult(payload, totalBytes);
	}

	@WithSpan
	public boolean validateFiles(HttpServletRequest request, Entitlements entitlements, UUID tenantId) {
		if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
			log.debug("Request is not a multipart form submission; skipping file validation.");
			return true;
		}
		List<String> contentTypes = null;
		try {
			Collection<Part> parts = request.getParts();
			if (parts == null || parts.isEmpty()) {
				return true;
			}
			contentTypes = new ArrayList<>(parts.size());
			long totalUploadSize = 0;
			for (Part part : parts) {
				if (part.getSubmittedFileName() == null || part.getSubmittedFileName().isBlank())
					continue;
				if (part.getSize() > entitlements.maxFileSizeBytes()) {
					log.debug("File size ({}) too big (max: {}) for field: {}", part.getSize(), entitlements.maxFileSizeBytes(), part.getName());
					return false;
				}
				String contentType = part.getContentType();
				if (contentType == null || contentType.isBlank()) continue;
				contentTypes.add(contentType.trim().toLowerCase());
				if (!ALLOWED_MIME_TYPES.contains(contentType.trim().toLowerCase())) {
					log.debug("Invalid MIME type detected: {} for file field: {}", contentType, part.getName());
					return false;
				}
				totalUploadSize += part.getSize();
			}
			if (totalUploadSize > 0) {
				long consumed = storageApi.getStorageBytesConsumed(tenantId);
				if (consumed + totalUploadSize > entitlements.storageLimitBytes()) {
					log.debug("Storage limit exceeded for tenant {}. Max: {}, Attempted: {}", tenantId, entitlements.storageLimitBytes(), consumed + totalUploadSize);
					return false;
				}
			}
			return true;
		} catch (IOException | ServletException e) {
			log.error("Failed to parse file parts from HttpServletRequest", e);
			return true;
		} finally {
			var finalContentTypes = contentTypes;
			Sentry.metrics().distribution("submissions.stats.contentTypes", contentTypes.size() * 1.0);
			Sentry.configureScope(scope -> scope.setContexts("contentTypes", finalContentTypes == null ? List.of("") : finalContentTypes));
		}
	}

	public boolean validateFields(Map<String, String> ignoredPayload, FormDto ignoredForm) {
		return true;
	}

	@WithSpan
	@Transactional
//	@Async
	public void asyncSendNotifs(FormDto form, Submission submission, Map<String, String> payload) {
		discordNotif.sendDiscordNotif(form.formNotifs(), payload);

		ZeptoMailSuccessResponse autoresponse = submissionEmailsService.sendEmailAutoresponse(form.formNotifs(), payload);
		submission.setEmailAutoresponseRequestId(autoresponse == null ? null : autoresponse.getRequestId());
		submission.setEmailAutoresponseEmailStatus(autoresponse == null ? null : EmailStatus.SENT);

		ZeptoMailSuccessResponse emailNotif = submissionEmailsService.sendEmailNotif(form.formNotifs(), payload, form, submission.getCreatedAt());
		submission.setEmailNotifRequestId(emailNotif == null ? null : emailNotif.getRequestId());
		submission.setEmailNotifStatus(emailNotif == null ? null : EmailStatus.SENT);

		submissionRepository.save(submission);
	}

}

