package formbox.notifs.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import formbox.notifs.ZeptoMailSuccessResponse;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties(EmailProperties.class)
public class ZeptomailService {

	private final RestClient restClient;
	private final EmailProperties properties;
	private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator(false, false, DomainValidator.getInstance(false));

	public ZeptomailService(EmailProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder().baseUrl(properties.apiUrl()).defaultHeader("Accept", "application/json").defaultHeader("Authorization", "Zoho-enczapikey " + properties.apiKey()).build();
	}

	@WithSpan
	public ZeptoMailSuccessResponse sendEmail(String to, List<String> cc, List<String> bcc, List<String> replyTo, String subject, String htmlBody, String fromName, String fromAddress) {

		cc = cc.stream().filter(e -> !e.isBlank()).toList();
		bcc = bcc.stream().filter(e -> !e.isBlank()).toList();
		replyTo = replyTo.stream().filter(e -> !e.isBlank()).toList();

		int ccCount = sizeOf(cc);
		int bccCount = sizeOf(bcc);
		int replyToCount = sizeOf(replyTo);

		log.info("Sending {} with {} cc recipients, {} bcc recipients, {} reply-to addresses, subject length {}",fromName, ccCount, bccCount, replyToCount, subject == null ? 0 : subject.length());

		ISpan httpSpan = Sentry.getSpan();

		if (subject == null || subject.isBlank()) {
			subject = htmlBody.split("\n", 2)[0];
		}

		long startedAtNanos = System.nanoTime();

		try {
			var request = new ZeptoMailRequest(new ZeptoMailRequest.From(fromAddress + properties.fromAddress(), fromName),
				List.of(ZeptoMailRequest.Recipient.of(to)),
				toRecipients(cc), toRecipients(bcc), toReplyTo(replyTo), subject, htmlBody);

			ZeptoMailSuccessResponse response = restClient.post().contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(ZeptoMailSuccessResponse.class);

			long durationNs = System.nanoTime() - startedAtNanos;
			if (httpSpan != null) httpSpan.setStatus(SpanStatus.OK);
			recordMetrics(durationNs, true);
			log.info("Email sent successfully in {} ms", durationNs / 1_000_000);

			return response;

		} catch (RestClientResponseException e) {
			long durationNs = System.nanoTime() - startedAtNanos;

			if (httpSpan != null) httpSpan.setStatus(SpanStatus.INTERNAL_ERROR);
			recordMetrics(durationNs, false);

			ZeptoMailErrorResponse errorResponse = parseErrorResponse(e);

			log.error("Failed to send autoresponder after {} ms, http status {}, error {}", durationNs / 1_000_000, e.getStatusCode(), errorResponse, e);

			Sentry.withScope(scope -> {
				scope.setTag("http_status", e.getStatusCode().toString());
				if (errorResponse != null && errorResponse.error() != null) {
					scope.setTag("zeptomail_error_code", errorResponse.error().code());
				}
				Sentry.captureException(e);
			});

			return null;

		} catch (Exception e) {
			long durationNs = System.nanoTime() - startedAtNanos;

			if (httpSpan != null) httpSpan.setStatus(SpanStatus.INTERNAL_ERROR);
			recordMetrics(durationNs, false);

			log.error("Failed to send autoresponder after {} ms", durationNs, e);
			Sentry.captureException(e);

			return null;

		} finally {
			if (httpSpan != null) httpSpan.finish();
		}
	}

	private ZeptoMailErrorResponse parseErrorResponse(RestClientResponseException e) {
		try {
			return e.getResponseBodyAs(ZeptoMailErrorResponse.class);
		} catch (Exception parseException) {
			log.warn("Could not parse ZeptoMail error response body", parseException);
			return null;
		}
	}

	private void recordMetrics(long durationNs, boolean success) {
		if (success) Sentry.metrics().count("autoresponder.autoresponse.failure", 1.0);
		else Sentry.metrics().count("autoresponder.autoresponse.success", 1.0);
		Sentry.metrics().count("autoresponder.sent", 1.0);
		Sentry.metrics().distribution("autoresponder.sendTimeMs", (double) durationNs, "ns");
	}

	private List<ZeptoMailRequest.Recipient> toRecipients(List<String> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return List.of();
		}
		return addresses.stream().filter(EMAIL_VALIDATOR::isValid).map(ZeptoMailRequest.Recipient::of).toList();
	}

	private List<ZeptoMailRequest.ReplyTo> toReplyTo(List<String> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return List.of();
		}
		return addresses.stream().map(ZeptoMailRequest.ReplyTo::of).toList();
	}

	private int sizeOf(List<String> list) {
		return list == null ? 0 : list.size();
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
record ZeptoMailErrorResponse(@JsonProperty("error") ZeptoMailError error) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record ZeptoMailError(@JsonProperty("code") String code) {
}