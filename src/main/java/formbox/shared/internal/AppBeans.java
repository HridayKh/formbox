package formbox.shared.internal;

import com.github.benmanes.caffeine.cache.Caffeine;
import formbox.shared.CacheNames;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.registerCustomCache(CacheNames.JWT_TOKEN, Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5)).build());
		return cacheManager;
	}
}
