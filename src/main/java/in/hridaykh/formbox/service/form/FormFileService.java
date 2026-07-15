package in.hridaykh.formbox.service.form;

import in.hridaykh.formbox.model.dto.CachedForm;
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
	public void uploadFilesAndInitNotifsWebhooks(CachedForm form, Map<String, String> payload, HttpServletRequest request) {
		// step 14: async start upload files/attachments
		// step 15: async 3rd party webhooks and notifs
		log.info("Processing background task");
		try {
			Thread.sleep(10000);
			log.info("Task Processed");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.info("Task Failed", e);
		}
	}
}
