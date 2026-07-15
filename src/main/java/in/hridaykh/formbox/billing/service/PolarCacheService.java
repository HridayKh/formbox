package in.hridaykh.formbox.billing.service;

import in.hridaykh.formbox.billing.model.PolarProducts;
import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.billing.PolarProductsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolarCacheService {

	private static final long CACHE_TTL_HOURS = 2;

	private final StringRedisTemplate redisTemplate;
	private final PolarMeterService polarMeterService;
	private final PolarProductsRepository polarProductsRepository;
	private final ObjectMapper objectMapper;

	public long getCachedSubmissionBalance(UUID tenantId) {
		String key = getRedisKey(tenantId);
		String cachedValue = redisTemplate.opsForValue().get(key);

		if (cachedValue != null) {
			try {
				long balance = Long.parseLong(cachedValue);
				log.trace("Redis meter balance cache HIT for tenant ID: {}. Balance: {}", tenantId, balance);
				return balance;
			} catch (NumberFormatException e) {
				log.error("Corrupted meter cache value discovered for key: {}", key, e);
			}
		}
		log.debug("Redis meter balance cache MISS for tenant ID: {}. Syncing live state...", tenantId);
		return syncAndCacheMeterBalance(tenantId);
	}

	public void decrementCachedSubmissionBalance(UUID tenantId) {
		String key = getRedisKey(tenantId);
		getCachedSubmissionBalance(tenantId);
		Long remaining = redisTemplate.opsForValue().decrement(key, 1L);
		if (remaining == null) {
			log.warn("Atomically requested decrement failed to return a value for key: {}", key);
			return;
		}
		log.debug("Atomically consumed 1 submission locally. Remaining: {} for tenant: {}", remaining, tenantId);
		CompletableFuture.runAsync(() -> polarMeterService.reportSubmissionUsageEvent(tenantId)).exceptionally(ex -> {
			log.error("Async usage reporting failed for tenant: {}", tenantId, ex);
			return null;
		});
	}

	public long syncAndCacheMeterBalance(UUID tenantId) {
		String key = getRedisKey(tenantId);
		try {
			long liveBalance = polarMeterService.getRemainingSubmissionsBalance(tenantId);

			redisTemplate.opsForValue().set(key, String.valueOf(liveBalance), Expiration.from(CACHE_TTL_HOURS, TimeUnit.HOURS));

			log.debug("Synchronized Redis meter balance cache to Polar ground-truth ({}) for tenant: {}", liveBalance, tenantId);
			return liveBalance;
		} catch (Exception e) {
			log.error("Failed to sync updated Polar meter balance to Redis cache for tenant: {}", tenantId, e);
			return 0L;
		}
	}

	private String getRedisKey(UUID tenantId) {
		return String.format("formbox:%s:%s", CacheNames.METER_BALANCE, tenantId);
	}

	// ================================ UN-CHANGING POLAR PRODUCT METADATA ================================

	@Cacheable(value = CacheNames.POJO_BY_POLAR_PRODUCT_ID, key = "#polarProductId")
	public PolarProducts productByPolarProductId(String polarProductId) {
		log.trace("Local L1 cache MISS for product ID: {}", polarProductId);
		String redisKey = String.format("formbox:%s:%s", CacheNames.POJO_BY_POLAR_PRODUCT_ID, polarProductId);

		String cachedJson = redisTemplate.opsForValue().get(redisKey);
		if (cachedJson != null) {
			try {
				log.trace("Redis L2 cache HIT for product ID: {}", polarProductId);
				return objectMapper.readValue(cachedJson, PolarProducts.class);
			} catch (Exception e) {
				log.error("Failed to deserialize PolarProducts from Redis L2 for ID: {}", polarProductId, e);
			}
		}

		log.debug("Redis L2 cache MISS for product ID: {}. Fetching configuration details from persistent database repository.", polarProductId);
		PolarProducts product = polarProductsRepository.findByPolarProductId(polarProductId).orElse(null);

		if (product != null) {
			try {
				redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(product), Duration.ofDays(2));
			} catch (Exception e) {
				log.error("Failed to serialize and cache PolarProducts entity to Redis L2 for ID: {}", polarProductId, e);
			}
		}

		return product;
	}

	@Cacheable(value = CacheNames.PRODUCT_BY_SLUG, key = "#slug")
	public PolarProducts productBySlug(String slug) {
		log.trace("Local L1 cache MISS for product slug: {}", slug);
		String redisKey = String.format("formbox:%s:%s", CacheNames.PRODUCT_BY_SLUG, slug);

		String cachedJson = redisTemplate.opsForValue().get(redisKey);
		if (cachedJson != null) {
			try {
				log.trace("Redis L2 cache HIT for product slug: {}", slug);
				return objectMapper.readValue(cachedJson, PolarProducts.class);
			} catch (Exception e) {
				log.error("Failed to deserialize PolarProducts from Redis L2 for slug: {}", slug, e);
			}
		}

		log.debug("Redis L2 cache MISS for product slug: {}. Fetching entity structure from database context.", slug);
		PolarProducts product = polarProductsRepository.findBySlug(slug).orElse(null);

		if (product != null) {
			try {
				redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(product), Duration.ofDays(2));
			} catch (Exception e) {
				log.error("Failed to serialize and cache PolarProducts entity to Redis L2 for slug: {}", slug, e);
			}
		}

		return product;
	}
}