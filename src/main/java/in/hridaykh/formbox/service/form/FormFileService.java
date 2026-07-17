package in.hridaykh.formbox.service.form;

import in.hridaykh.formbox.model.dto.CachedForm;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FormFileService {
	@Async
	@WithSpan
	public void uploadFilesAndInitNotifsWebhooks(CachedForm form, Map<String, String> payload, HttpServletRequest request) {
		// step 14: async start upload files/attachments
		// step 15: async 3rd party webhooks and notifs
		log.debug("Processing background task for form ID: {}", form.id());
		try {
			Thread.sleep(10000);
			log.debug("Background task processed successfully for form ID: {}", form.id());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Background task interrupted or failed for form ID: {}", form.id(), e);
		}
	}
}
