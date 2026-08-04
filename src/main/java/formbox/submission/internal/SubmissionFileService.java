package formbox.submission.internal;

import formbox.form.FormDto;
import formbox.notifs.DiscordNotif;
import formbox.notifs.EmailAutoresponse;import formbox.notifs.UploadService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.ISpan;
import io.sentry.Sentry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
class SubmissionFileService {

	private final DiscordNotif discordNotif;
	private final UploadService uploadService;
	private final SubmissionRepository submissionRepository;private final EmailAutoresponse emailAutoresponse;

	@WithSpan
	@Transactional
	public void uploadFilesAndInitNotifsWebhooks(FormDto form, Submission submission, Map<String, String> payload, HttpServletRequest request) {
		uploadFiles(request, submission);
		discordNotif.sendDiscordNotif(form.formNotifs(), payload);
		emailAutoresponse.sendEmailAutoresponse(form.formNotifs(), payload);
	}

	@Transactional
	void uploadFiles(HttpServletRequest request, Submission submission) {
		ISpan span = null;
		try {
			if (Sentry.getSpan() != null)
				span = Sentry.getSpan().startChild("SubmissionFileService.uploadFiles");
			if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
				log.debug("Skipping file upload for non multipart submission.");
				return;
			}
			var payload = (submission.getPayload());
			Collection<Part> parts = request.getParts();
			if (parts == null || parts.isEmpty()) {
				return;
			}
			for (Part part : parts) {
				if (part.getSubmittedFileName() == null || part.getSubmittedFileName().isBlank())
					continue;
				String contentType = part.getContentType();
				if (contentType == null || contentType.isBlank()) continue;
				payload.put(part.getName(), part.getSubmittedFileName());
				payload.put(part.getName() + "__url", uploadService.uploadFile(part.getInputStream(), part.getSubmittedFileName()));
				Sentry.metrics().distribution("submissions.stats.fileSizeBytes", part.getSize() * 1.0, "byte");
			}
			submission.setPayload(payload);
			submissionRepository.save(submission);
		} catch (IOException | ServletException e) {
			log.error("Failed to parse file parts from HttpServletRequest", e);
		} finally {
			if (span != null)
				span.finish();
		}
	}
}
