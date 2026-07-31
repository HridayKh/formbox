package formbox.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	// ==========================================
	// SET METHODS
	// ==========================================

	public <T> void set(String cacheName, String key, T value, Duration timeout) {
		String fullKey = KEY(cacheName, key);
		try {
			String jsonValue = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(fullKey, jsonValue, timeout);
		} catch (Exception e) {
			log.error("Unable to set key {} in redis", fullKey, e);
		}
	}

	public <T> void set(String cacheName, String key, T value) {
		set(cacheName, key, value, Duration.ofDays(7));
	}

	// ==========================================
	// GET & READ METHODS
	// ==========================================

	public <T> Optional<T> get(String cacheName, String key, Class<T> clazz) {
		String fullKey = KEY(cacheName, key);
		String jsonValue = redisTemplate.opsForValue().get(fullKey);
		if (jsonValue == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(jsonValue, clazz));
		} catch (JacksonException e) {
			throw new RuntimeException("Error deserializing JSON from Redis for key: " + fullKey, e);
		}
	}

	public <T> T getOrCompute(String cacheName, String key, Class<T> clazz, Supplier<T> supplier, Duration timeout) {
		Optional<T> cachedValue = get(cacheName, key, clazz);
		if (cachedValue.isPresent()) {
			return cachedValue.get();
		}

		T freshValue = supplier.get();
		if (freshValue != null) {
			set(cacheName, key, freshValue, timeout);
		}
		return freshValue;
	}

	public <T> T getOrCompute(String cacheName, String key, Class<T> clazz, Supplier<T> supplier) {
		return getOrCompute(cacheName, key, clazz, supplier, Duration.ofDays(7));
	}

	// ==========================================
	// KEY MANAGEMENT & UTILITIES
	// ==========================================

	public boolean hasKey(String cacheName, String key) {
		String fullKey = KEY(cacheName, key);
		return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
	}

	public boolean delete(String cacheName, String key) {
		String fullKey = KEY(cacheName, key);
		return Boolean.TRUE.equals(redisTemplate.delete(fullKey));
	}

	public Long delete(String cacheName, Collection<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return 0L;
		}
		List<String> fullKeys = keys.stream()
			.map(k -> KEY(cacheName, k))
			.toList();
		return redisTemplate.delete(fullKeys);
	}

	public boolean expire(String cacheName, String key, Duration timeout) {
		String fullKey = KEY(cacheName, key);
		return Boolean.TRUE.equals(redisTemplate.expire(fullKey, timeout));
	}

	// ==========================================
	// HELPER
	// ==========================================

	private String KEY(String cacheName, String key) {
		return String.format("f:%s:%s", cacheName, key);
	}
}