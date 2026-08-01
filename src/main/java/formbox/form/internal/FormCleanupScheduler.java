package formbox.form.internal;

import formbox.form.FormHardDeleteRequestedEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class FormCleanupScheduler {

	private final FormRepository formRepository;

	private static final int FORM_LIMIT_PER_RUN = 10;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Scheduled(fixedDelay = 60 * 60 * 1000)
	@WithSpan
	void cleanupDeletedForms() {
		log.debug("Starting scheduled cleanup of soft-deleted forms...");

		List<UUID> softDeletedFormIds = formRepository.findSoftDeletedFormIds(FORM_LIMIT_PER_RUN);
		if (softDeletedFormIds.isEmpty()) {
			log.debug("No soft-deleted forms found for cleanup.");
			return;
		}

		for (UUID formId : softDeletedFormIds) {
			try {
				applicationEventPublisher.publishEvent(new FormHardDeleteRequestedEvent(formId));
				formRepository.hardDeleteForm(formId);
				log.info("Successfully requested cleanup and hard-deleted Form ID: {}", formId);
			} catch (Exception e) {
				log.error("Failed to clean up Form ID: {}", formId, e);
			}
		}

		log.debug("Finished scheduled cleanup of soft-deleted forms.");
	}
}