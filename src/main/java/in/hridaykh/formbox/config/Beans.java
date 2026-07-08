package in.hridaykh.formbox.config;

import in.hridaykh.formbox.filter.SupabaseSessionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.spring.PolarProperties;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class Beans {

	private final PolarProperties polarProperties;

	public Beans(PolarProperties polarProperties) {
		this.polarProperties = polarProperties;
	}

	@Bean
	public PolarHttpClient polarHttpClient() {
		log.info("Initializing PolarHttpClient bean configuration...");

		log.debug("Configuring PolarHttpClient -> Base URL: [{}], Connect Timeout: [{}ms], Max Retries: [{}], Backoff: [{}ms]",
			polarProperties.apiUrlOrDefault(),
			polarProperties.connectTimeoutOrDefault(),
			polarProperties.maxRetryAttemptsOrDefault(),
			polarProperties.retryBackoffMillisOrDefault()
		);

		try {
			var builder = PolarHttpClient.builder(polarProperties.accessToken());
			builder.baseUrl(polarProperties.apiUrlOrDefault());
			builder.connectTimeout(polarProperties.connectTimeoutOrDefault());
			builder.maxRetryAttempts(polarProperties.maxRetryAttemptsOrDefault());
			builder.retryBackoff(polarProperties.retryBackoffMillisOrDefault());

			PolarHttpClient client = builder.build();
			log.info("PolarHttpClient successfully initialized.");
			return client;

		} catch (Exception e) {
			log.error("Failed to construct PolarHttpClient. Critical property validation failure.", e);
			throw e;
		}
	}

	@Bean
	public FilterRegistrationBean<SupabaseSessionFilter> loggingFilter(SupabaseSessionFilter sessionFilter) {
		log.info("Initializing SupabaseSessionFilter registration...");

		FilterRegistrationBean<SupabaseSessionFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(sessionFilter);

		List<String> urlPatterns = Arrays.asList("/", "/*", "/**");
		registrationBean.setUrlPatterns(urlPatterns);

		log.debug("SupabaseSessionFilter mapped to URL patterns: {}", urlPatterns);
		log.info("SupabaseSessionFilter registration complete.");
		return registrationBean;
	}
}