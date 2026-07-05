package in.hridaykh.formbox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.spring.PolarProperties;

@Configuration
public class Beans {

	private final PolarProperties polarProperties;
	private final RedisProperties redisProperties;

	public Beans(PolarProperties polarProperties, RedisProperties redisProperties) {
		this.polarProperties = polarProperties;
		this.redisProperties = redisProperties;
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

	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		var redisConfig = new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
		redisConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
		return new JedisConnectionFactory(redisConfig);
	}

	@Bean
	public StringRedisTemplate redisTemplate() {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(jedisConnectionFactory());
		return template;
	}


}
