package formbox.notifs.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoho.email")
public record EmailProperties(
	String apiUrl,
	String apiKey,
	String fromAddress,
	String fromName,
	String webhookAuthKey
) {
}