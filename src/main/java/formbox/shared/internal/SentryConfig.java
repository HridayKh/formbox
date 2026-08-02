package formbox.shared.internal;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SentryConfig {

	@Bean
	public Sentry.OptionsConfiguration<SentryOptions> sentryOptionsConfiguration(BuildProperties buildProperties) {
		log.info("Configuring sentry options for release version");
		return options -> options.setRelease(buildProperties.getVersion());
	}
}
