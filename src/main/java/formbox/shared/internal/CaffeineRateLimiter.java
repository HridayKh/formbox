package formbox.shared.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

class CaffeineRateLimiter {

	private final double capacity;
	private final double refillRatePerSecond;
	private final Cache<String, TokenBucket> cache;

	CaffeineRateLimiter(long capacity, double refillRatePerSecond) {
		this.capacity = capacity;
		this.refillRatePerSecond = refillRatePerSecond;

		long ttlSeconds = (long) Math.ceil(capacity / refillRatePerSecond);

		this.cache = Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(ttlSeconds)).maximumSize(100_000).build();
	}

	boolean tryConsume(String key) {
		TokenBucket bucket = cache.get(key, _ -> new TokenBucket(capacity, refillRatePerSecond));
		return bucket.tryConsume();
	}

	private static class TokenBucket {
		private final double capacity;
		private final double refillRatePerNanos;
		private double tokens;
		private long lastUpdatedNanos;

		TokenBucket(double capacity, double refillRatePerSecond) {
			this.capacity = capacity;
			this.refillRatePerNanos = refillRatePerSecond / 1_000_000_000.0;
			this.tokens = capacity;
			this.lastUpdatedNanos = System.nanoTime();
		}

		synchronized boolean tryConsume() {
			long now = System.nanoTime();
			long elapsedNanos = Math.max(0, now - lastUpdatedNanos);

			tokens = Math.min(capacity, tokens + (elapsedNanos * refillRatePerNanos));
			lastUpdatedNanos = now;

			if (tokens >= 1.0) {
				tokens -= 1;
				return true;
			}
			return false;
		}
	}
}