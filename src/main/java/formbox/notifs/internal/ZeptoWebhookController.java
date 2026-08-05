package formbox.notifs.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import formbox.notifs.EmailStatus;
import formbox.notifs.SubmissionNotifApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ZeptoWebhookController {

	private final SubmissionNotifApi submissionNotifApi;
	private final ZeptoWebhookSignatureValidator signatureValidator;
	private final ObjectMapper objectMapper;

	@PostMapping("/webhooks/zeptomail")
	@WithSpan
	ResponseEntity<Void> handleZeptoMailWebhook(HttpServletRequest request, @RequestHeader(value = "producer-signature", required = false) String signature) {

		String rawBody;
		try {
			rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to read ZeptoMail webhook body", e);
			return ResponseEntity.badRequest().build();
		}

		if (!signatureValidator.isValid(rawBody, signature)) {
			log.warn("Rejected ZeptoMail webhook request: invalid signature");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		ZeptoWebhookPayload payload;
		try {
			payload = objectMapper.readValue(rawBody, ZeptoWebhookPayload.class);
		} catch (JacksonException e) {
			log.error("Failed to parse ZeptoMail webhook payload", e);
			return ResponseEntity.badRequest().build();
		}

		EmailStatus status = resolveStatus(payload.getEventName());
		log.debug("processed email track webhook for: {} {}, raw event name: {}", payload.getRequestId(), status, payload.eventName());
		if (status != null && payload.getRequestId() != null)
			submissionNotifApi.updateEmailStatus(payload.getRequestId(), status);
		return ResponseEntity.ok().build();
	}

	private EmailStatus resolveStatus(String eventName) {
		if (eventName == null) {
			return null;
		}
		return switch (eventName.toLowerCase().strip()) {
			case "delivered" -> EmailStatus.DELIVERED;
			case "softbounce" -> EmailStatus.SOFT_BOUNCE;
			case "hardbounce" -> EmailStatus.HARD_BOUNCE;
			case "fbl_compliant" -> EmailStatus.MARKED_AS_SPAM;
			default -> null;
		};
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
record ZeptoWebhookPayload(@JsonProperty("event_name") List<String> eventName,
                           @JsonProperty("event_message") List<EventMessage> eventMessage) {
	public String getEventName() {
		return (eventName == null || eventName.isEmpty()) ? null : eventName.getFirst();
	}

	public String getRequestId() {
		return eventMessage == null || eventMessage.isEmpty() ? null : eventMessage.getFirst().requestId();
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
record EventMessage(@JsonProperty("request_id") String requestId) {
}