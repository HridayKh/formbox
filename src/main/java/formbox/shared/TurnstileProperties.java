package formbox.shared;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "turnstile")
@Data
public class TurnstileProperties {
	private String secretKey;
}