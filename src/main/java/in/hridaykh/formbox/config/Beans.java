package in.hridaykh.formbox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.spring.PolarProperties;

@Configuration
public class Beans {

	private final PolarProperties polarProperties;

	public Beans(PolarProperties polarProperties) {
		this.polarProperties = polarProperties;
	}

	@Bean
	public PolarHttpClient polarHttpClient() {
		var builder = PolarHttpClient.builder(polarProperties.accessToken());
		builder.baseUrl(polarProperties.apiUrlOrDefault());
		builder.connectTimeout(polarProperties.connectTimeoutOrDefault());
		builder.maxRetryAttempts(polarProperties.maxRetryAttemptsOrDefault());
		builder.retryBackoff(polarProperties.retryBackoffMillisOrDefault());
		return builder.build();
	}
}
