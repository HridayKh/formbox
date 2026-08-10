package formbox.billing.service;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.billing.PolarSubmissionApi;
import formbox.billing.DbSubmissionCounter;
import formbox.shared.Entitlements;
import formbox.shared.CacheNames;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class PolarSubmissionApiImpl implements PolarSubmissionApi {

	private final PolarMeterService polarMeterService;
	private final EntitlementsApi entitlementsApi;
	private final DbSubmissionCounter dbSubmissionCounter;
	private final TenantApi tenantApi;
	private final RedisCache redisCache;

	@WithSpan
	@Cacheable(value = CacheNames.METER_BALANCE, key = "#tenantId.toString()")
	public long getCachedSubmissionBalance(UUID tenantId) {
		ensureEntitlementsRefresh(tenantId);
		return redisCache.getOrCompute(CacheNames.METER_BALANCE, tenantId.toString(), Long.class, () -> getLiveSubmissionBalance(tenantId));
	}

	private long getLiveSubmissionBalance(UUID tenantId) {
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		if (entitlements.isFree()) {
			Instant cycleStart = entitlements.refreshAt() != null ? entitlements.refreshAt().minus(30, ChronoUnit.DAYS) : Instant.now().minus(30, ChronoUnit.DAYS);
			OffsetDateTime since = OffsetDateTime.ofInstant(cycleStart, ZoneOffset.UTC);
			long consumed = dbSubmissionCounter.countSubmissionsAfter(tenantId, since);
			return Math.max(0, entitlements.submissionsLimit() - consumed);
		} else {
			return polarMeterService.getRemainingSubmissionsBalance(tenantId);
		}
	}

	@CacheEvict(value = CacheNames.METER_BALANCE, key = "#tenantId.toString()")
//	@Async
	@WithSpan
	public void decrementSubmissionBalance(UUID tenantId) {
		if (redisCache.decrement(CacheNames.METER_BALANCE, tenantId.toString()).isEmpty()) {
			log.warn("Redis meter balance decrement failed for tenant Id: {}", tenantId);
			return;
		}
		polarMeterService.reportSubmissionUsageEvent(tenantId);
	}

	private void ensureEntitlementsRefresh(UUID tenantId) {
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		if (entitlements.refreshAt() != null && Instant.now().isAfter(entitlements.refreshAt())) {
			Instant nextRefresh = entitlements.refreshAt();
			Instant now = Instant.now();
			while (!nextRefresh.isAfter(now)) nextRefresh = nextRefresh.plus(30, ChronoUnit.DAYS);

			Entitlements updatedEntitlements = entitlements.toBuilder().refreshAt(nextRefresh).build();

			tenantApi.updateTenantEntitlementsInDb(tenantId, updatedEntitlements);
			entitlementsApi.updateEntitlementsCache(tenantId, updatedEntitlements);
			redisCache.set(CacheNames.METER_BALANCE, tenantId.toString(), updatedEntitlements.submissionsLimit());
			log.info("Entitlements monthly refresh boundary crossed. Reset submission counter to {} and refreshAt to {} for tenant: {}", updatedEntitlements.submissionsLimit(), nextRefresh, tenantId);
		}
	}

}