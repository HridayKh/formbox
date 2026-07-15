package in.hridaykh.formbox.billing.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.billing.service.PolarWebhooksService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import sh.polar.sdk.PolarWebhookVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PolarWebhookController {

	private final PolarWebhooksService polarWebhooksService;
	private final PolarWebhookVerifier polarWebhookVerifier;

	@PostMapping(PathRegistry.Webhooks.POLAR)
	public ResponseEntity<Map<String, Object>> handlePolarWebhook(HttpServletRequest request) {
		log.trace("Received incoming webhook HTTP request payload from Polar platform.");
		Map<String, Object> responseBody = new LinkedHashMap<>();

		String body = "";
		if (request.getContentLengthLong() > 0) {
			try (BufferedReader reader = request.getReader()) {
				body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
			} catch (IOException e) {
				log.error("Failed to parse or read structural string request input buffer stream context from webhook body.", e);
				responseBody.put("status", "rejected");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
			}
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
			log.error("Cryptographic execution failure thrown during payload authenticity verification matching tasks.", e);
		}

		HttpStatus status = HttpStatus.UNAUTHORIZED;
		responseBody.put("status", "rejected");

		if (isValid) {
			log.info("Polar incoming webhook event payload successfully authenticated for execution. Event ID: {}", webhookId);
			status = HttpStatus.OK;
			responseBody.put("status", "accepted");

			try {
				polarWebhooksService.processHook(body);
				log.debug("Downstream processing service completed webhook ingestion task asynchronously or sequentially.");
			} catch (Exception e) {
				log.error("Internal business layer logic failure crashed during webhook consumption payload mapping pipeline for ID: {}", webhookId, e);
			}
		} else {
			log.warn("Security rejection. Incoming request headers failed signature validation verification bounds for payload ID: {}", webhookId);
		}

		return ResponseEntity.status(status).body(responseBody);
	}
}