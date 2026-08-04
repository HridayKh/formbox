package formbox.submission.internal;

import formbox.form.FormHardDeleteRequestedEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class FormSubmissionCleanupListener {

	private final SubmissionRepository submissionRepository;
	private static final int BATCH_SIZE = 500;
	private static final long PAUSE_MS = 100;

	@EventListener
	@Async
	@WithSpan
	public void onFormHardDelete(FormHardDeleteRequestedEvent event) {
		int deletedCount;
		long totalDeleted = 0;
		try {
			do {
				deletedCount = submissionRepository.deleteSubmissionsInBatch(event.formId(), BATCH_SIZE);
				totalDeleted += deletedCount;
				if (deletedCount > 0)
					Thread.sleep(PAUSE_MS);
			} while (deletedCount > 0);
			log.info("Cleaned up {} submissions for deleted form {}", totalDeleted, event.formId());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}