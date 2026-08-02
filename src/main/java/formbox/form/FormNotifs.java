package formbox.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FormNotifs(
	@JsonProperty("discord_webhook_url") String discordWebhookUrl,
	@JsonProperty("discord_body") String discordBody
) {
}
