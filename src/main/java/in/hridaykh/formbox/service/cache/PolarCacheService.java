package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.service.polar.PolarMeterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PolarCacheService {

	private static final Logger log = LoggerFactory.getLogger(PolarCacheService.class);

	private static final String METER_BALANCE_KEY_PREFIX = "formbox:meterBalance:";
	private static final long CACHE_TTL_HOURS = 2;

	private final StringRedisTemplate redisTemplate;
	private final PolarMeterService polarMeterService;

	public PolarCacheService(StringRedisTemplate redisTemplate, PolarMeterService polarMeterService) {
		this.redisTemplate = redisTemplate;
		this.polarMeterService = polarMeterService;
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
}