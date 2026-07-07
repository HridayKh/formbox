package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.model.entity.Purchases;
import in.hridaykh.formbox.model.enums.SubscriptionState;
import in.hridaykh.formbox.repository.PurchasesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantTierCacheService {

	private final PurchasesRepository purchasesRepository;
	private final StringRedisTemplate stringRedisTemplate;

	@Cacheable(value = "tenantTiers", key = "#tenantId.toString()")
	public String resolveHighestActiveTierNonNull(UUID tenantId) {
		String tier = resolveHighestActiveTierNullable(tenantId);
		return tier == null ? "free-v1" : tier;
	}

	private static final String TENANT_TIER_BASE_KEY = "formbox:tenantTiers:";

	@Cacheable(value = "tenantTiers", key = "#tenantId.toString()")
	public String resolveHighestActiveTierNullable(UUID tenantId) {
		log.info("Local L1 cache MISS for tenant tier: {}", tenantId);
		String redisKey = TENANT_TIER_BASE_KEY + tenantId.toString();

		String cachedTier = stringRedisTemplate.opsForValue().get(redisKey);
		if (cachedTier != null) {
			log.info("Redis HIT for tenant tier: {}", tenantId);
			return "NULL".equals(cachedTier) ? null : cachedTier;
		}

		log.info("Redis MISS for tenant tier: {}. Fetching from DB...", tenantId);
		List<Purchases> activePurchases = purchasesRepository.findValidUserPurchases(tenantId, OffsetDateTime.now(), SubscriptionState.free, SubscriptionState.active, SubscriptionState.cancelled_grace_period);

		String highestTier = "NULL";
		if (activePurchases != null && !activePurchases.isEmpty())
			highestTier = activePurchases.getFirst().getProduct().getSlug().toLowerCase();

		try {
			stringRedisTemplate.opsForValue().set(redisKey, highestTier, Duration.ofDays(2));
		} catch (Exception e) {
			log.error("Failed to save tenant tier to Redis for tenant: {}", tenantId, e);
		}

		return highestTier;
	}

	@CacheEvict(value = "tenantTiers", key = "#ignoredTenantId")
	public void evictTenantTierCache(String ignoredTenantId) {
	}
}