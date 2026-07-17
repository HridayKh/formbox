package in.hridaykh.formbox.billing.controller;

import in.hridaykh.formbox.billing.service.WebhookService;
import in.hridaykh.formbox.constant.PathRegistry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import sh.polar.sdk.PolarWebhookVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PolarWebhookController {

	private final PolarWebhookVerifier polarWebhookVerifier;
	private final WebhookService webhooksService;

	@PostMapping(PathRegistry.Webhooks.POLAR)
	@WithSpan
	public ResponseEntity<Map<String, Object>> handlePolarWebhook(HttpServletRequest request) {
		log.trace("Received incoming webhook HTTP request payload from Polar platform.");
		Map<String, Object> responseBody = new LinkedHashMap<>();

		String body = "";
		try (InputStream is = request.getInputStream()) {
			body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to read body from webhook request", e);
			responseBody.put("status", "rejected");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
		}

		String webhookId = request.getHeader("webhook-id");
		String timestamp = request.getHeader("webhook-timestamp");
		String signature = request.getHeader("webhook-signature");

		log.debug("Extracted verification header values -> Webhook-ID: [{}], Timestamp: [{}], Signature present: {}",
			webhookId, timestamp, signature != null);

		boolean isValid = false;
		try {
			isValid = polarWebhookVerifier.verify(body, webhookId, timestamp, signature);
		} catch (Exception e) {
			log.warn("Cryptographic verification failed for webhook ID: {}", webhookId, e);
		}

		HttpStatus status = HttpStatus.UNAUTHORIZED;
		responseBody.put("status", "rejected");

		if (isValid) {
			log.info("Polar incoming webhook event payload successfully authenticated. Event ID: {}", webhookId);
			status = HttpStatus.OK;
			responseBody.put("status", "accepted");

			try {
				webhooksService.processHook(body);
				log.debug("Webhook processing completed for ID: {}", webhookId);
			} catch (Exception e) {
				log.error("Failed to process webhook for ID: {}", webhookId, e);
			}
		} else {
			log.warn("Security rejection: Incoming request failed signature validation for webhook ID: {}", webhookId);
		}

		return ResponseEntity.status(status).body(responseBody);
	}

	@PostMapping("/webhooks/polar/test")
	public ResponseEntity<Map<String, Object>> handlePolarWebhookTest(HttpServletRequest request) {
		log.warn("RECEIVED MOCK WEBHOOK INGESTION REQUEST FOR LOCAL TESTING");
		Map<String, Object> responseBody = new LinkedHashMap<>();
		String body = "";
		try (InputStream is = request.getInputStream()) {
			body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to read body", e);
			responseBody.put("status", "rejected");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
		}
		try {
			webhooksService.processHook(body);
			responseBody.put("status", "accepted");
			return ResponseEntity.ok(responseBody);
		} catch (Exception e) {
			log.error("Failed downstream webhook test consumption", e);
			responseBody.put("status", "error");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
		}
	}
}