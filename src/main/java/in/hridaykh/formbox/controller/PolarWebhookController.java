package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.service.polar.PolarWebhooksService;
import jakarta.servlet.http.HttpServletRequest;
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
public class PolarWebhookController {

	private final PolarWebhooksService polarWebhooksService;
	private final PolarWebhookVerifier polarWebhookVerifier;

	public PolarWebhookController(PolarWebhooksService polarWebhooksService, PolarWebhookVerifier polarWebhookVerifier) {
		this.polarWebhooksService = polarWebhooksService;
		this.polarWebhookVerifier = polarWebhookVerifier;
	}

	@PostMapping(PathRegistry.Webhooks.POLAR) // "/polar"
	public ResponseEntity<Map<String, Object>> handlePolarWebhook(HttpServletRequest request) {
		Map<String, Object> responseBody = new LinkedHashMap<>();

		String body = "";
		if (request.getContentLengthLong() > 0) {
			try (BufferedReader reader = request.getReader()) {
				body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
			} catch (IOException e) {
				responseBody.put("status", "rejected");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
			}
		}

		String webhookId = request.getHeader("webhook-id");
		String timestamp = request.getHeader("webhook-timestamp");
		String signature = request.getHeader("webhook-signature");
		boolean isValid = polarWebhookVerifier.verify(body, webhookId, timestamp, signature);

		HttpStatus status = HttpStatus.UNAUTHORIZED;
		responseBody.put("status", "rejected");

		if (isValid) {
			status = HttpStatus.OK;
			responseBody.put("status", "accepted");
			polarWebhooksService.processHook(body);
		}

		return ResponseEntity.status(status).body(responseBody);
	}
}