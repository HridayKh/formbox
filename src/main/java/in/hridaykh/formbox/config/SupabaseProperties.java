package in.hridaykh.formbox.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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