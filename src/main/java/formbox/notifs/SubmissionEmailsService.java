package formbox.notifs;

import formbox.form.FormDto;
import formbox.form.FormNotifs;
import formbox.notifs.internal.ZeptomailService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionEmailsService {

	private final Pattern pattern = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");
	private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator(false, false, DomainValidator.getInstance(false));
	private final ZeptomailService zeptomailService;

	@WithSpan
	public ZeptoMailSuccessResponse sendEmailAutoresponse(FormNotifs formNotifs, Map<String, String> payload) {
		long start = System.nanoTime();
		try {
			if (formNotifs.autoresponderEmailFieldName() == null || formNotifs.autoresponderEmailFieldName().isBlank()
				|| formNotifs.autoresponderEmailBody() == null || formNotifs.autoresponderEmailBody().isBlank()
				|| !EMAIL_VALIDATOR.isValid(formNotifs.autoresponderReplyTo())) {
				start = -1;
				return null;
			}
			String emailAddress = payload.getOrDefault(formNotifs.autoresponderEmailFieldName(), null);
			if (emailAddress == null || emailAddress.isBlank() || !EMAIL_VALIDATOR.isValid(emailAddress)) {
				start = -1;
				Sentry.metrics().count("autoresponder.autoresponse.invalidEmail");
				log.debug("Skipping autoresponder response for invalid autoresponder in submission.");
				return null;
			}

			Sentry.metrics().count("autoresponder.autoresponse.started");


			var matcher = pattern.matcher(formNotifs.autoresponderEmailBody().strip());
			var sb = new StringBuilder();
			while (matcher.find())
				matcher.appendReplacement(sb, Matcher.quoteReplacement(payload.getOrDefault(matcher.group(1), matcher.group(0))));
			matcher.appendTail(sb);
			String body = sb.toString().replace("\n", "<br/>");

			var resp = zeptomailService.sendEmail(emailAddress, null, null, List.of(formNotifs.autoresponderReplyTo()), formNotifs.autoresponderSubjectLine(), body, "Formbox Autoresponder", "autoresponder");
			Sentry.metrics().count("autoresponder.autoresponse.succeeded");
			return resp;
		} catch (Exception e) {
			Sentry.metrics().count("autoresponder.autoresponse.failed");
			log.error("Email Autoresponse Failed!", e);
			Sentry.captureException(e);
		} finally {
			if (start >= 0)
				Sentry.metrics().distribution("autoresponder.autoresponse.time", start - System.nanoTime() * 1.0, "ns");
		}
		return null;
	}

	@WithSpan
	public ZeptoMailSuccessResponse sendEmailNotif(FormNotifs formNotifs, Map<String, String> payload, FormDto form, OffsetDateTime submitTime) {
		long start = System.nanoTime();
		try {
			if (formNotifs.emailNotifTo() == null || formNotifs.emailNotifTo().isBlank()
				|| !EMAIL_VALIDATOR.isValid(formNotifs.emailNotifTo())) {
				start = -1;
				return null;
			}

			Sentry.metrics().count("email.notifs.started");

			String dashUrl = "https://formbox.hridaykh.in/forms/" + form.folderId() + "/" + form.id();

			String subject = "[Formbox] New submission for " + form.name();
			String body = "You received a new form submission via Formbox.\n" +
				"\n" +
				"Form Name: " + form.name() + "\n" +
				"Submitted At: " + submitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
				"\n" +
				"--- SUBMISSION DATA ---\n" +
				"\n" +
				formatEmailNotifKeys(payload) +
				"\n-----------------------\n" +
				"\n" +
				"View and manage your submissions in your Formbox dashboard:\n" +
				"<a href=\"" + dashUrl + "\">" + dashUrl + "</a>\n" +
				"\n" +
				"--\n" +
				"Formbox - Headless Form Backend";

			var resp = zeptomailService.sendEmail(formNotifs.emailNotifTo(), formNotifs.emailNotifCc(),
				formNotifs.emailNotifBcc(), null, subject, body.replace("\n", "<br/>"), "Formbox Email Notification", "no-reply");
			Sentry.metrics().count("email.notifs.succeeded");
			return resp;
		} catch (Exception e) {
			Sentry.metrics().count("email.notifs.failed");
			log.error("Email Autoresponse Failed!", e);
			Sentry.captureException(e);
		} finally {
			if (start >= 0)
				Sentry.metrics().distribution("email.notifs.time", start - System.nanoTime() * 1.0, "ns");
		}
		return null;
	}

	public static String formatEmailNotifKeys(Map<String, String> payload) {
		return payload.entrySet().stream()
			.filter(entry -> !entry.getKey().endsWith("__url"))
			.map(entry -> {
				String key = entry.getKey();
				String val = entry.getValue();
				String urlKey = key + "__url";
				if (payload.containsKey(urlKey))
					return String.format("%s: <a href=\"%s\">%s</a>", key, payload.get(urlKey), val);
				else
					return String.format("%s: %s", key, val);
			})
			.collect(Collectors.joining("\n"));
	}
}
