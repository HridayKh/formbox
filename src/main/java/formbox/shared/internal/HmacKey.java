package formbox.shared.internal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hmac")
@Data
public class HmacKey {
	private String key;
}
