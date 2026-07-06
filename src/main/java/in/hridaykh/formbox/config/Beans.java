package in.hridaykh.formbox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.spring.PolarProperties;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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

	@Bean
	public RedisCacheConfiguration redisCacheConfiguration() {
		ObjectMapper redisObjectMapper = JsonMapper.builder().deactivateDefaultTyping().build();
		JavaType javaType = redisObjectMapper.getTypeFactory().constructType(Object.class);
		var serializer = RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(redisObjectMapper, javaType));
		return RedisCacheConfiguration.defaultCacheConfig().computePrefixWith(c -> "formbox:" + c + ":").serializeValuesWith(serializer);
	}

}
