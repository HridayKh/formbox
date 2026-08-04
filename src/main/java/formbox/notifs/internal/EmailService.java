package formbox.notifs.internal;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties(EmailProperties.class)
public class EmailService {

	private final RestClient restClient;
	private final EmailProperties properties;

	public EmailService(EmailProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder().baseUrl(properties.apiUrl()).defaultHeader("Accept", "application/json").defaultHeader("Authorization", "Zoho-enczapikey " + properties.apiKey()).build();
	}

	@WithSpan
	public void sendEmail(String to, List<String> cc, List<String> bcc, List<String> replyTo, String subject, String htmlBody) {

		int ccCount = sizeOf(cc);
		int bccCount = sizeOf(bcc);
		int replyToCount = sizeOf(replyTo);

		log.info("Sending email with {} cc recipients, {} bcc recipients, {} reply-to addresses, subject length {}", ccCount, bccCount, replyToCount, subject == null ? 0 : subject.length());

		ISpan httpSpan = Sentry.getSpan();

		if (subject == null || subject.isBlank()) {
			subject = htmlBody.split("\n", 2)[0];
		}

		long startedAtNanos = System.nanoTime();

		try {
			var request = new ZeptoMailRequest(new ZeptoMailRequest.From(properties.fromAddress(), properties.fromName()), List.of(ZeptoMailRequest.Recipient.of(to)), toRecipients(cc), toRecipients(bcc), toReplyTo(replyTo), subject, htmlBody);

			restClient.post().contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toEntity(String.class);

			long durationNs = System.nanoTime() - startedAtNanos;
			if (httpSpan != null) httpSpan.setStatus(SpanStatus.OK);
			recordMetrics(durationNs, true);
			log.info("Email sent successfully in {} ms", durationNs / 1_000_000);

		} catch (RestClientResponseException e) {
			long durationNs = System.nanoTime() - startedAtNanos;

			if (httpSpan != null) httpSpan.setStatus(SpanStatus.INTERNAL_ERROR);
			recordMetrics(durationNs, false);

			log.error("Failed to send email after {} ms, http status {}", durationNs / 1_000_000, e.getStatusCode(), e);

			Sentry.withScope(scope -> {
				scope.setTag("http_status", e.getStatusCode().toString());
				Sentry.captureException(e);
			});

		} catch (Exception e) {
			long durationNs = System.nanoTime() - startedAtNanos;

			if (httpSpan != null) httpSpan.setStatus(SpanStatus.INTERNAL_ERROR);
			recordMetrics(durationNs, false);

			log.error("Failed to send email after {} ms", durationNs, e);
			Sentry.captureException(e);

		} finally {
			if (httpSpan != null) httpSpan.finish();
		}
	}

	private void recordMetrics(long durationNs, boolean success) {
		if (success) Sentry.metrics().count("email.autoresponse.failure", 1.0);
		else Sentry.metrics().count("email.autoresponse.success", 1.0);
		Sentry.metrics().count("email.sent", 1.0);
		Sentry.metrics().distribution("email.sendTimeMs", (double) durationNs, "ns");
	}

	private List<ZeptoMailRequest.Recipient> toRecipients(List<String> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return List.of();
		}
		return addresses.stream().map(ZeptoMailRequest.Recipient::of).toList();
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