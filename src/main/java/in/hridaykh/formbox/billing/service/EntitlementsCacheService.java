package in.hridaykh.formbox.billing.service;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntitlementsCacheService {

	private final TenantRepository tenantRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private static final String CACHE_NAME = "tenantEntitlements";

	@Cacheable(value = CACHE_NAME, key = "#tenantId.toString()")
	@WithSpan
	public Entitlements getEntitlements(UUID tenantId) {
		log.trace("Caffeine L1 cache MISS for tenant entitlements ID: {}", tenantId);

		String redisKey = String.format("formbox:%s:%s", CACHE_NAME, tenantId);
		String cachedJson = null;
		try {
			cachedJson = redisTemplate.opsForValue().get(redisKey);
		} catch (Exception e) {
			log.error("Failed to connect to Redis cluster while fetching entitlements key: {}", redisKey, e);
		}

		if (cachedJson != null) {
			try {
				log.trace("Redis L2 cache HIT for tenant entitlements ID: {}", tenantId);
				return objectMapper.readValue(cachedJson, Entitlements.class);
			} catch (Exception e) {
				log.error("Failed to deserialize Entitlements JSON payload from Redis for ID: {}", tenantId, e);
			}
		}

		log.debug("Redis L2 cache MISS for tenant entitlements ID: {}. Fetching from persistent database...", tenantId);
		Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
		Entitlements entitlements = tenant != null ? tenant.getEntitlementsOrDefaults() : Entitlements.freeDefaults();

		try {
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(entitlements), Duration.ofDays(2));
			log.trace("Successfully backfilled Redis L2 cache for tenant entitlements ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to serialize and save Entitlements to Redis for ID: {}", tenantId, e);
		}

		return entitlements;
	}

	@CachePut(value = CACHE_NAME, key = "#tenantId.toString()")
	@WithSpan
	public Entitlements updateEntitlementsCache(UUID tenantId, Entitlements entitlements) {
		log.debug("Updating entitlements cache for tenant ID: {}", tenantId);
		String redisKey = String.format("formbox:%s:%s", CACHE_NAME, tenantId);

		try {
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(entitlements), Duration.ofDays(2));
			log.trace("Redis L2 cache successfully updated for tenant entitlements ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to update Redis L2 cache for tenant entitlements ID: {}", tenantId, e);
		}

		return entitlements;
	}

	@CacheEvict(value = CACHE_NAME, key = "#tenantId.toString()")
	@WithSpan
	public void evictEntitlementsCache(UUID tenantId) {
		log.debug("Evicting entitlements cache for tenant ID: {}", tenantId);
		try {
			Boolean deleted = redisTemplate.delete(String.format("formbox:%s:%s", CACHE_NAME, tenantId));
			log.trace("Redis L2 cache eviction completion status for tenant entitlements ID {}: {}", tenantId, deleted);
		} catch (Exception e) {
			log.error("Failed to purge entitlements key from Redis L2 for ID: {}", tenantId, e);
		}
	}
}
