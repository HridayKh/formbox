package formbox.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FormNotifs(String discordWebhookUrl, String discordBody,
                         String autoresponderEmailFieldName, String autoresponderEmailBody,
                         String autoresponderReplyTo, String autoresponderSubjectLine) {
}
