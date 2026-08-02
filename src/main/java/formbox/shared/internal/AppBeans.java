package formbox.shared.internal;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class AppBeans {

	@Bean
	public Sentry.OptionsConfiguration<SentryOptions> sentryOptionsConfiguration(BuildProperties buildProperties) {
		log.info("Configuring sentry options for release version");
		return options -> options.setRelease(buildProperties.getVersion());
	}
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
