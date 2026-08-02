package formbox.shared;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCache {

	@Setter
	private static Duration DEFAULT_TIMEOUT = Duration.ofDays(7);

	@Setter
	private static Duration NEGATIVE_CACHE_TIMEOUT = Duration.ofMinutes(1);

	private static final String NULL_SENTINEL = "\u0000NULL\u0000";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

	// ==========================================
	// SET METHODS
	// ==========================================

	@WithSpan
	public <T> void set(String cacheName, String key, T value, Duration timeout) {
		String fullKey = KEY(cacheName, key);
		try {
			if (value == null) {
				long start = System.nanoTime();
				stringRedisTemplate.opsForValue().set(fullKey, NULL_SENTINEL, timeout);
				recordRedisTime(start);
				return;
			}
			String jsonValue = objectMapper.writeValueAsString(value);
			long start = System.nanoTime();
			stringRedisTemplate.opsForValue().set(fullKey, jsonValue, timeout);
			recordRedisTime(start);
		} catch (Exception e) {
			log.error("Unable to set key {} in redis", fullKey, e);
		}
	}

	@WithSpan
	public <T> void set(String cacheName, String key, T value) {
		set(cacheName, key, value, DEFAULT_TIMEOUT);
	}

	// ==========================================
	// GET METHODS
	// ==========================================

	@WithSpan
	public <T> Optional<T> get(String cacheName, String key, Class<T> type) {
		return lookup(cacheName, key, jsonValue -> objectMapper.readValue(jsonValue, type)).toOptional();
	}

	@WithSpan
	public <T> Optional<T> get(String cacheName, String key, TypeReference<T> type) {
		return lookup(cacheName, key, jsonValue -> objectMapper.readValue(jsonValue, type)).toOptional();
	}

	// ==========================================
	// GET-OR-COMPUTE METHODS
	// ==========================================

	@WithSpan
	public <T> T getOrCompute(String cacheName, String key, Class<T> type, Supplier<T> supplier, Duration timeout) {
		return getOrCompute(cacheName, key, timeout, () -> lookup(cacheName, key, jsonValue -> objectMapper.readValue(jsonValue, type)), supplier);
	}

	@WithSpan
	public <T> T getOrCompute(String cacheName, String key, Class<T> type, Supplier<T> supplier) {
		return getOrCompute(cacheName, key, type, supplier, DEFAULT_TIMEOUT);
	}

	@WithSpan
	public <T> T getOrCompute(String cacheName, String key, TypeReference<T> type, Supplier<T> supplier, Duration timeout) {
		return getOrCompute(cacheName, key, timeout, () -> lookup(cacheName, key, jsonValue -> objectMapper.readValue(jsonValue, type)), supplier);
	}

	@WithSpan
	public <T> T getOrCompute(String cacheName, String key, TypeReference<T> type, Supplier<T> supplier) {
		return getOrCompute(cacheName, key, type, supplier, DEFAULT_TIMEOUT);
	}

	/**
	 * Shared single-flight implementation. `lookupFn` is called (potentially twice, due to
	 * double-checked locking) to read the current cache state; `supplier` is only ever invoked
	 * by the thread that wins the per-key lock on a genuine miss.
	 */
	@WithSpan
	private <T> T getOrCompute(String cacheName, String key, Duration timeout, Supplier<CacheLookup<T>> lookupFn, Supplier<T> supplier) {
		CacheLookup<T> cached = lookupFn.get();
		if (cached.present()) return cached.value();

		log.debug("Redis miss for {}:{}", cacheName, key);

		String fullKey = KEY(cacheName, key);
		Object lock = keyLocks.computeIfAbsent(fullKey, _ -> new Object());
		synchronized (lock) {
			try {
				/* I have pretty low to no traffic rn so no double lookups for now*/
				// cached = lookupFn.get();
				// if (cached.present()) return cached.value();
				T freshValue = supplier.get();
				set(cacheName, key, freshValue, freshValue == null ? NEGATIVE_CACHE_TIMEOUT : timeout);
				return freshValue;
			} finally {
				keyLocks.remove(fullKey, lock);
			}
		}
	}

	// ==========================================
	// KEY MANAGEMENT & UTILITIES
	// ==========================================

	@WithSpan
	public boolean delete(String cacheName, String key) {
		String fullKey = KEY(cacheName, key);
		long start = System.nanoTime();
		Boolean result = stringRedisTemplate.delete(fullKey);
		recordRedisTime(start);
		return Boolean.TRUE.equals(result);
	}

	@WithSpan
	public Long delete(String cacheName, Collection<String> keys) {
		if (keys == null || keys.isEmpty()) return 0L;
		List<String> fullKeys = keys.stream().map(k -> KEY(cacheName, k)).toList();
		long start = System.nanoTime();
		Long result = stringRedisTemplate.delete(fullKeys);
		recordRedisTime(start);
		return result;
	}

	@WithSpan
	public Optional<Long> decrement(String cacheName, String key, long delta) {
		String fullKey = KEY(cacheName, key);
		try {
			long start = System.nanoTime();
			Long newValue = stringRedisTemplate.opsForValue().decrement(fullKey, delta);
			recordRedisTime(start);
			return Optional.ofNullable(newValue);
		} catch (Exception e) {
			log.error("Unable to decrement key {} in redis", fullKey, e);
			return Optional.empty();
		}
	}

	@WithSpan
	public Optional<Long> decrement(String cacheName, String key) {
		return decrement(cacheName, key, 1L);
	}

	@WithSpan
	public Optional<Long> increment(String cacheName, String key, Duration ttl) {
		String fullKey = KEY(cacheName, key);
		try {
			long start = System.nanoTime();
			Long currentCount = stringRedisTemplate.opsForValue().increment(fullKey);
			recordRedisTime(start);
			if (currentCount == null) {
				log.error("Redis increment returned null for key: {}", fullKey);
				return Optional.empty();
			}
			if (currentCount == 1 && ttl != null) {
				long expireStart = System.nanoTime();
				stringRedisTemplate.expire(fullKey, ttl);
				recordRedisTime(expireStart);
			}
			return Optional.of(currentCount);
		} catch (Exception e) {
			log.error("Failed to execute increment logic in Redis for key: {}", fullKey, e);
			return Optional.empty();
		}
	}

	// ==========================================
	// HELPERS
	// ==========================================

	@WithSpan
	private String KEY(String cacheName, String key) {
		return String.format("f:%s:%s", cacheName, key);
	}

	@WithSpan
	private void recordRedisTime(long startNanos) {
		Sentry.metrics().distribution("redis.time_ns", System.nanoTime() - startNanos * 1.0, "ns");
	}

	@WithSpan
	private <T> CacheLookup<T> lookup(String cacheName, String key, JsonParser<T> parser) {
		String fullKey = KEY(cacheName, key);
		String jsonValue;
		try {
			long start = System.nanoTime();
			jsonValue = stringRedisTemplate.opsForValue().get(fullKey);
			recordRedisTime(start);
		} catch (Exception e) {
			log.warn("Redis unavailable while reading key {}, treating as cache miss", fullKey, e);
			return CacheLookup.miss();
		}

		if (jsonValue == null) {
			return CacheLookup.miss();
		}
		if (NULL_SENTINEL.equals(jsonValue)) {
			return CacheLookup.hit(null);
		}
		try {
			return CacheLookup.hit(parser.parse(jsonValue));
		} catch (JacksonException e) {
			log.error("Failed to deserialize cached value for key: {}, treating as cache miss", fullKey, e);
			return CacheLookup.miss();
		}
	}

	@FunctionalInterface
	private interface JsonParser<T> {
		@WithSpan
		T parse(String jsonValue) throws JacksonException;
	}

	/**
	 * present=false -> cache miss (or Redis unavailable); present=true, value=null -> cached negative result.
	 */
	private record CacheLookup<T>(boolean present, T value) {
		static <T> CacheLookup<T> miss() {
			return new CacheLookup<>(false, null);
		}

		static <T> CacheLookup<T> hit(T value) {
			return new CacheLookup<>(true, value);
		}

		Optional<T> toOptional() {
			return present ? Optional.ofNullable(value) : Optional.empty();
		}
	}
}