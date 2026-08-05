package formbox.notifs.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import formbox.notifs.EmailStatus;
import formbox.notifs.SubmissionNotifApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ZeptoWebhookController {

	private final SubmissionNotifApi submissionNotifApi;

	@PostMapping("/webhooks/zeptomail")
	@WithSpan
	ResponseEntity<Void> handleZeptoMailWebhook(@RequestBody ZeptoWebhookPayload payload) {
		EmailStatus status = resolveStatus(payload.getEventName());
		if (status != null && payload.getRequestId() != null)
			submissionNotifApi.updateEmailStatus(payload.getRequestId(), status);
		return ResponseEntity.ok().build();
	}

	private EmailStatus resolveStatus(String eventName) {
		if (eventName == null) {
			return null;
		}
		return switch (eventName.toLowerCase()) {
			case "delivered" -> EmailStatus.DELIVERED;
			case "softbounce" -> EmailStatus.SOFT_BOUNCE;
			case "hardbounce" -> EmailStatus.HARD_BOUNCE;
			case "feedback loop", "feedbackloop", "fbl_complaint" -> EmailStatus.MARKED_AS_SPAM;
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