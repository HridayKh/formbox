package formbox.submission.internal;

import formbox.form.FormHardDeleteRequestedEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import formbox.notifs.UploadService;

@Component
@RequiredArgsConstructor
@Slf4j
class FormSubmissionCleanupListener {

	private final SubmissionRepository submissionRepository;
	private final UploadService uploadService;
	private static final int BATCH_SIZE = 500;
	private static final long PAUSE_MS = 100;

	@EventListener
//	@Async
	@WithSpan
	public void onFormHardDelete(FormHardDeleteRequestedEvent event) {
		int deletedCount;
		long totalDeleted = 0;
		try {
			do {
				var batch = submissionRepository.findTop500ByFormId(event.formId());
				if (batch.isEmpty()) {
					break;
				}
				for (Submission sub : batch) {
					var payload = sub.getPayload();
					if (payload != null && !payload.isEmpty()) {
						for (var entry : payload.entrySet()) {
							if (entry.getKey() != null && entry.getKey().endsWith("__url")) {
								String fileUrl = entry.getValue();
								if (fileUrl != null && !fileUrl.isBlank()) {
									try {
										uploadService.deleteFileByUrl(fileUrl.strip());
									} catch (Exception e) {
										log.error("Failed to delete S3 attachment {} during form cleanup", fileUrl, e);
									}
								}
							}
						}
					}
				}

				deletedCount = submissionRepository.deleteSubmissionsInBatch(event.formId(), BATCH_SIZE);
				totalDeleted += deletedCount;
				if (deletedCount > 0)
					Thread.sleep(PAUSE_MS);
			} while (deletedCount > 0);
			log.info("Cleaned up {} submissions for deleted form {}", totalDeleted, event.formId());

			uploadService.deleteAllCsvExports(event.formId());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("Failed to clean up S3 resources for deleted form {}", event.formId(), e);
		}
	}
}