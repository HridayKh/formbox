package formbox.notifs;

import formbox.form.FormNotifs;
import formbox.notifs.internal.EmailService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailAutoresponse {

	private final Pattern pattern = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");
	private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator(false, false, DomainValidator.getInstance(false));
	private final EmailService emailService;

	@Async
	@WithSpan
	public void sendEmailAutoresponse(FormNotifs formNotifs, Map<String, String> payload) {
		long start = System.nanoTime();
		try {
			if (formNotifs.autoresponderEmailFieldName() == null || formNotifs.autoresponderEmailFieldName().isBlank()
				|| formNotifs.autoresponderEmailBody() == null || formNotifs.autoresponderEmailBody().isBlank()
				|| !EMAIL_VALIDATOR.isValid(formNotifs.autoresponderReplyTo())) {
				start = -1;
				return;
			}
			String emailAddress = payload.getOrDefault(formNotifs.autoresponderEmailFieldName(), null);
			if (emailAddress == null || emailAddress.isBlank() || !EMAIL_VALIDATOR.isValid(emailAddress)) {
				start = -1;
				Sentry.metrics().count("email.autoresponse.invalidEmail");
				log.debug("Skipping email response for invalid email in submission.");
				return;
			}

			Sentry.metrics().count("email.autoresponse.started");


			var matcher = pattern.matcher(formNotifs.autoresponderEmailBody().strip());
			var sb = new StringBuilder();
			while (matcher.find())
				matcher.appendReplacement(sb, Matcher.quoteReplacement(payload.getOrDefault(matcher.group(1), matcher.group(0))));
			matcher.appendTail(sb);
			String body = sb.toString();
			emailService.sendEmail(emailAddress, null, null, List.of(formNotifs.autoresponderReplyTo()), formNotifs.autoresponderSubjectLine(), body);

			Sentry.metrics().count("email.autoresponse.succeeded");
		} catch (Exception e) {
			Sentry.metrics().count("email.autoresponse.failed");
			log.error("Email Autoresponse Failed!", e);
			Sentry.captureException(e);
		} finally {
			if (start >= 0)
				Sentry.metrics().distribution("email.autoresponse.time", start - System.nanoTime() * 1.0, "ns");
		}
	}
}
