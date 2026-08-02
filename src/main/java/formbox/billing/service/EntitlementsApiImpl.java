package formbox.billing.service;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.shared.CacheNames;
import formbox.shared.Entitlements;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntitlementsApiImpl implements EntitlementsApi {

	private final TenantApi tenantApi;
	private final RedisCache redisCache;

	@Cacheable(value = CacheNames.TENANT_ENTITLEMENTS, key = "#tenantId.toString()")
	@WithSpan
	@Override
	public Entitlements getEntitlements(UUID tenantId) {
		log.debug("Caffeine L1 cache MISS for tenant entitlements ID: {}", tenantId);
		return redisCache.getOrCompute(CacheNames.TENANT_ENTITLEMENTS, tenantId.toString(), Entitlements.class, () -> tenantApi.getTenantEntitlementsOrDefault(tenantId));
	}

	@CachePut(value = CacheNames.TENANT_ENTITLEMENTS, key = "#tenantId.toString()")
	@WithSpan
	@Override
	public void updateEntitlementsCache(UUID tenantId, Entitlements entitlements) {
		log.info("Updating entitlements cache for tenant ID: {}", tenantId);
		redisCache.set(CacheNames.TENANT_ENTITLEMENTS, tenantId.toString(), entitlements);
	}
}
