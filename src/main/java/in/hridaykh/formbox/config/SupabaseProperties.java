package in.hridaykh.formbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {
	private String url;
	private String publishableKey;
	private String secretKey;
	private String jwksUrl;

	// Getters and Setters
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPublishableKey() {
		return publishableKey;
	}

	public void setPublishableKey(String publishableKey) {
		this.publishableKey = publishableKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getJwksUrl() {
		return jwksUrl;
	}

	public void setJwksUrl(String jwksUrl) {
		this.jwksUrl = jwksUrl;
	}
}