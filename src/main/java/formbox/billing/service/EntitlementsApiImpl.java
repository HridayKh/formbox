package formbox.billing.service;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.shared.CacheNames;
import formbox.shared.Entitlements;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EntitlementsApiImpl implements EntitlementsApi {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private final TenantApi tenantApi;

	@Cacheable(value = CacheNames.TENANT_ENTITLEMENTS, key = "#tenantId.toString()")
	@WithSpan
	@Override
	public Entitlements getEntitlements(UUID tenantId) {
		log.trace("Caffeine L1 cache MISS for tenant entitlements ID: {}", tenantId);

		String redisKey = String.format("formbox:%s:%s", CacheNames.TENANT_ENTITLEMENTS, tenantId);
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

		Entitlements entitlements = tenantApi.getTenantEntitlementsOrDefault(tenantId);

		try {
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(entitlements), Duration.ofDays(2));
			log.trace("Successfully backfilled Redis L2 cache for tenant entitlements ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to serialize and save Entitlements to Redis for ID: {}", tenantId, e);
		}

		return entitlements;
	}

	@CachePut(value = CacheNames.TENANT_ENTITLEMENTS, key = "#tenantId.toString()")
	@WithSpan
	@Override
	public void updateEntitlementsCache(UUID tenantId, Entitlements entitlements) {
		log.debug("Updating entitlements cache for tenant ID: {}", tenantId);
		String redisKey = String.format("formbox:%s:%s", CacheNames.TENANT_ENTITLEMENTS, tenantId);

		try {
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(entitlements), Duration.ofDays(2));
			log.trace("Redis L2 cache successfully updated for tenant entitlements ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to update Redis L2 cache for tenant entitlements ID: {}", tenantId, e);
		}
	}
}
