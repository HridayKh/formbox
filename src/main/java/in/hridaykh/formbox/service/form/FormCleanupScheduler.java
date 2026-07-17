package in.hridaykh.formbox.service.form;

import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormCleanupScheduler {

	private final FormRepository formRepository;
	private final SubmissionRepository submissionRepository;

	private static final int FORM_LIMIT_PER_RUN = 10;
	private static final int BATCH_SIZE = 500;
	private static final long PAUSE_MS = 100;

	@Scheduled(fixedDelay = 10 * 60 * 1000)
	@WithSpan
	public void cleanupDeletedForms() {
		log.debug("Starting scheduled cleanup of soft-deleted forms...");

		List<UUID> softDeletedFormIds = formRepository.findSoftDeletedFormIds(FORM_LIMIT_PER_RUN);
		if (softDeletedFormIds.isEmpty()) {
			log.debug("No soft-deleted forms found for cleanup.");
			return;
		}

		for (UUID formId : softDeletedFormIds) {
			log.debug("Cleaning up submissions for Form ID: {}", formId);
			try {
				int deletedCount;
				long totalDeletedSubmissions = 0;
				do {
					deletedCount = submissionRepository.deleteSubmissionsInBatch(formId, BATCH_SIZE);
					totalDeletedSubmissions += deletedCount;
					if (deletedCount > 0) {
						log.debug("Deleted batch of {} submissions for Form ID: {}", deletedCount, formId);
						Thread.sleep(PAUSE_MS);
					}
				} while (deletedCount > 0);
				formRepository.hardDeleteForm(formId);
				log.info("Successfully hard-deleted Form ID: {} along with {} submissions.", formId, totalDeletedSubmissions);

			} catch (InterruptedException e) {
				log.error("Cleanup thread interrupted while processing Form ID: {}", formId, e);
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("Failed to clean up Form ID: {}", formId, e);
			}
		}

		log.debug("Finished scheduled cleanup of soft-deleted forms.");
	}
}