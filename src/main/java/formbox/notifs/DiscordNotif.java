package formbox.notifs;

import formbox.form.FormDto;
import formbox.form.FormNotifs;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordNotif {

	private final RestTemplate restTemplate;
	private final Pattern pattern = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");

	@Async
	@WithSpan
	public void sendDiscordNotif(FormNotifs formNotifs, Map<String, String> payload) {
		Sentry.metrics().count("discord.webhooks.started");
		long start = System.nanoTime();
		try {
			var matcher = pattern.matcher(formNotifs.discordBody());
			var sb = new StringBuilder();
			while (matcher.find())
				matcher.appendReplacement(sb, Matcher.quoteReplacement(payload.getOrDefault(matcher.group(1), matcher.group(0))));
			matcher.appendTail(sb);
			var headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			restTemplate.postForEntity(formNotifs.discordWebhookUrl(), new HttpEntity<>(new DiscordPayload(sb.toString()), headers), Void.class);

			Sentry.metrics().count("discord.webhooks.succeeded");
		} catch (Exception e) {
			Sentry.metrics().count("discord.webhooks.failed");
			log.error("Discord webhook failed!", e);
			Sentry.captureException(e);
		} finally {
			Sentry.metrics().distribution("discord.webhooks.time", start - System.nanoTime() * 1.0, "ns");
		}
	}
}

record DiscordPayload(String content) {
}