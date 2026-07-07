package in.hridaykh.formbox.service.polar;

import in.hridaykh.formbox.model.entity.PolarProducts;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.PolarProductsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PolarCacheService {

	private static final Logger log = LoggerFactory.getLogger(PolarCacheService.class);

	private static final String METER_BALANCE_KEY_PREFIX = "formbox:meterBalance:";
	private static final String PRODUCT_CACHE_BASE_KEY = "formbox:pojoByPolarProductId:";
	private static final String PRODUCT_SLUG_CACHE_BASE_KEY = "formbox:productBySlug:";
	private static final long CACHE_TTL_HOURS = 2;

	private final StringRedisTemplate redisTemplate;
	private final PolarMeterService polarMeterService;
	private final PolarProductsRepository polarProductsRepository;
	private final ObjectMapper objectMapper;

	public PolarCacheService(StringRedisTemplate redisTemplate, PolarMeterService polarMeterService, PolarProductsRepository polarProductsRepository, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.polarMeterService = polarMeterService;
		this.polarProductsRepository = polarProductsRepository;
		this.objectMapper = objectMapper;
	}

	public long getCachedSubmissionBalance(Tenant tenant) {
		String key = getRedisKey(tenant);
		String cachedValue = redisTemplate.opsForValue().get(key);

		if (cachedValue != null) {
			try {
				return Long.parseLong(cachedValue);
			} catch (NumberFormatException e) {
				log.error("Corrupted meter cache value for key: {}", key, e);
			}
		}
		log.info("Cache miss for tenant meter balance: {}. Syncing live state...", tenant.getId());
		return syncAndCacheMeterBalance(tenant);
	}

	public void decrementCachedSubmissionBalance(Tenant tenant) {
		String key = getRedisKey(tenant);
		getCachedSubmissionBalance(tenant);
		Long remaining = redisTemplate.opsForValue().decrement(key, 1L);
		if (remaining == null)
			return;
		log.debug("Atomically consumed 1 submission locally. Remaining: {} for tenant: {}", remaining, tenant.getId());
		CompletableFuture.runAsync(() -> polarMeterService.reportSubmissionUsageEvent(tenant)).exceptionally(ex -> {
			log.error("Async usage reporting failed for tenant: {}", tenant.getId(), ex);
			return null;
		});
	}

	public long syncAndCacheMeterBalance(Tenant tenant) {
		String key = getRedisKey(tenant);
		try {
			long liveBalance = polarMeterService.getRemainingSubmissionsBalance(tenant);

			redisTemplate.opsForValue().set(key, String.valueOf(liveBalance), Expiration.from(CACHE_TTL_HOURS, TimeUnit.HOURS));

			log.info("Synchronized Redis meter balance cache to Polar ground-truth ({}) for tenant: {}", liveBalance, tenant.getId());
			return liveBalance;
		} catch (Exception e) {
			log.error("Failed to sync updated Polar meter balance to Redis cache for tenant: {}", tenant.getId(), e);
			return 0L;
		}
	}

	private String getRedisKey(Tenant tenant) {
		return METER_BALANCE_KEY_PREFIX + tenant.getId().toString();
	}


	@Cacheable(value = "pojoByPolarProductId", key = "#polarProductId")
	public PolarProducts productByPolarProductId(String polarProductId) {
		log.info("pojoByPolarProductId cache MISS for product ID: {}", polarProductId);
		String redisKey = PRODUCT_CACHE_BASE_KEY + polarProductId;

		String cachedJson = redisTemplate.opsForValue().get(redisKey);
		if (cachedJson != null) {
			try {
				log.info("pojoByPolarProductId Redis HIT for product ID: {}", polarProductId);
				return objectMapper.readValue(cachedJson, PolarProducts.class);
			} catch (Exception e) {
				log.error("pojoByPolarProductId Failed to deserialize PolarProducts from Redis for ID: {}", polarProductId, e);
			}
		}

		log.info("pojoByPolarProductId Redis MISS for product ID: {}. Fetching from DB...", polarProductId);
		PolarProducts product = polarProductsRepository.findByPolarProductId(polarProductId).orElse(null);

		if (product != null) {
			try {
				redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(product), Duration.ofDays(2));
			} catch (Exception e) {
				log.error("pojoByPolarProductId Failed to serialize and save PolarProducts to Redis for ID: {}", polarProductId, e);
			}
		}

		return product;
	}

	@Cacheable(value = "productBySlug", key = "#slug")
	public PolarProducts productBySlug(String slug) {
		log.info("Local L1 cache MISS for product slug: {}", slug);
		String redisKey = PRODUCT_SLUG_CACHE_BASE_KEY + slug;

		// 1. Check Redis (L2 Cache)
		String cachedJson = redisTemplate.opsForValue().get(redisKey);
		if (cachedJson != null) {
			try {
				log.info("Redis HIT for product slug: {}", slug);
				return objectMapper.readValue(cachedJson, PolarProducts.class);
			} catch (Exception e) {
				log.error("Failed to deserialize PolarProducts from Redis for slug: {}", slug, e);
			}
		}

		log.info("Redis MISS for product slug: {}. Fetching from DB...", slug);
		PolarProducts product = polarProductsRepository.findBySlug(slug).orElse(null);

		if (product != null) {
			try {
				redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(product), Duration.ofDays(2));
			} catch (Exception e) {
				log.error("Failed to serialize and save PolarProducts to Redis for slug: {}", slug, e);
			}
		}

		return product;
	}

}