package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.constant.FreeTierDefaults;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.spring7.tracing.SentrySpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantCacheService {

	private final TenantRepository tenantRepository;
	private final StringRedisTemplate stringRedisTemplate;

	@Cacheable(value = CacheNames.TENANT_TIERS, key = "#tenantId.toString()")
	@SentrySpan
	public String resolveHighestActiveTierNonNull(UUID tenantId) {
		log.trace("Resolving guaranteed non-null tier context for tenant ID: {}", tenantId);
		String tier = resolveHighestActiveTierNullable(tenantId);
		return tier == null ? FreeTierDefaults.TIER_NAME : tier;
	}

	@Cacheable(value = CacheNames.TENANT_TIERS, key = "#tenantId.toString()")
	public String resolveHighestActiveTierNullable(UUID tenantId) {
		log.trace("Local L1 cache MISS for tenant tier resolution: {}", tenantId);
		String redisKey = String.format("formbox:%s:%s", CacheNames.TENANT_TIERS, tenantId.toString());

		String cachedTier = null;
		try {
			cachedTier = stringRedisTemplate.opsForValue().get(redisKey);
		} catch (Exception e) {
			log.error("Failed to fetch tenant tier string from Redis cluster configuration for tenant ID: {}", tenantId, e);
		}

		if (cachedTier != null) {
			log.trace("Redis L2 cache HIT for tenant tier data on tenant ID: {}", tenantId);
			return "NULL".equals(cachedTier) ? null : cachedTier;
		}

		log.debug("Redis L2 cache MISS for tenant tier mapping on tenant ID: {}. Resolving from JSONB entitlements...", tenantId);
		Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
		String highestTier = "NULL";

		if (tenant != null) {
			highestTier = tenant.getEntitlementsOrDefaults().tierName();
			log.debug("Found tenant record. Assigned tier: {} for tenant ID: {}", highestTier, tenantId);
		} else {
			log.debug("No tenant record found in database storage layers for tenant ID: {}", tenantId);
		}

		try {
			stringRedisTemplate.opsForValue().set(redisKey, highestTier, Duration.ofDays(2));
			log.trace("Successfully updated Redis L2 backfill placeholder allocation state for tenant ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to serialize and save resolved tenant tier indicator string to Redis for tenant ID: {}", tenantId, e);
		}

		return "NULL".equals(highestTier) ? null : highestTier;
	}

	@CacheEvict(value = CacheNames.TENANT_TIERS, key = "#tenantId")
	public void evictTenantTierCache(String tenantId) {
		log.debug("Evicting multi-layer tenant tier records for structural identifier key payload: {}", tenantId);
		try {
			Boolean deleted = stringRedisTemplate.delete(String.format("formbox:%s:%s", CacheNames.TENANT_TIERS, tenantId));
			log.trace("Explicit removal execution confirmation results mapping inside Redis cluster context for key metadata execution task: {}", deleted);
		} catch (Exception e) {
			log.error("Failed to issue string deletion pipeline drop sequence directly into Redis cache cluster layer for target key representation value: {}", tenantId, e);
		}
	}
}