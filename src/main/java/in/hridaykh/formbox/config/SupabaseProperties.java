package in.hridaykh.formbox.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Setter
public class SupabaseProperties {
	private String url;
	private String secretKey;

	public String getUrl() {
		return url;
	}

	public String getSecretKey() {
		return secretKey;
	}
}