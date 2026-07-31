package formbox.auth.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
@Setter
class AuthConfig {
	private String supabaseUrl;
	private String supabaseSecretKey;

	@Getter
	private String turnstileSecretKey;


	public String getSupabaseUrl() {
		return supabaseUrl;
	}

	public String getSupabaseSecretKey() {
		return supabaseSecretKey;
	}
}