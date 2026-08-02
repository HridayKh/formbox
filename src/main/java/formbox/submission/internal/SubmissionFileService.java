package formbox.submission.internal;

import formbox.form.FormDto;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
class SubmissionFileService {

	private final RestTemplate restTemplate;

	@Async
	@WithSpan
	public void uploadFilesAndInitNotifsWebhooks(FormDto form, Map<String, String> payload) {
		// step 14: async start upload files/attachments
		// step 15: async 3rd party webhooks and notifs


		Matcher matcher = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}").matcher(form.formNotifs().discordBody());
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			String replacement = payload.getOrDefault(matcher.group(1), matcher.group(0));
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);

		DiscordPayload discordPayload = new DiscordPayload(sb.toString());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<DiscordPayload> request = new HttpEntity<>(discordPayload, headers);
		restTemplate.postForEntity(form.formNotifs().discordWebhookUrl(), request, Void.class);
	}
}

record DiscordPayload(String content) {
}