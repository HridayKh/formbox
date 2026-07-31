package formbox.billing.service;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.billing.PolarSubmissionApi;
import formbox.billing.DbSubmissionCounter;
import formbox.shared.Entitlements;
import formbox.shared.CacheNames;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
class PolarSubmissionApiImpl implements PolarSubmissionApi {

	private static final long CACHE_TTL_HOURS = 2;

	private final StringRedisTemplate redisTemplate;
	private final PolarMeterService polarMeterService;
	private final EntitlementsApi entitlementsApi;
	private final DbSubmissionCounter dbSubmissionCounter;
	private final TenantApi tenantApi;

	@WithSpan
	@Cacheable(value = CacheNames.METER_BALANCE, key = "#tenantId.toString()")
	public long getCachedSubmissionBalance(UUID tenantId) {
		ensureEntitlementsRefresh(tenantId);
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

	@WithSpan
	@CacheEvict(value = CacheNames.METER_BALANCE, key = "#tenantId.toString()")
	public void asyncDecrementCachedSubmissionBalance(UUID tenantId) {
		String key = getRedisKey(tenantId);
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

	private void ensureEntitlementsRefresh(UUID tenantId) {
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		if (entitlements.refreshAt() != null && Instant.now().isAfter(entitlements.refreshAt())) {
			Instant nextRefresh = entitlements.refreshAt();
			Instant now = Instant.now();
			while (!nextRefresh.isAfter(now)) {
				nextRefresh = nextRefresh.plus(30, ChronoUnit.DAYS);
			}

			Entitlements updatedEntitlements = Entitlements.builder().tierName(entitlements.tierName()).tierPriority(entitlements.tierPriority()).refreshAt(nextRefresh).recurringInterval(entitlements.recurringInterval()).submissionsLimit(entitlements.submissionsLimit()).formsLimit(entitlements.formsLimit()).storageLimitBytes(entitlements.storageLimitBytes()).discordNotifsAllowed(entitlements.discordNotifsAllowed()).turnstileAllowed(entitlements.turnstileAllowed()).redirectUrlsAllowed(entitlements.redirectUrlsAllowed()).jsonFormsAllowed(entitlements.jsonFormsAllowed()).fileUploadsAllowed(entitlements.fileUploadsAllowed()).fieldValidationsAllowed(entitlements.fieldValidationsAllowed()).slackNotifsAllowed(entitlements.slackNotifsAllowed()).telegramNotifsAllowed(entitlements.telegramNotifsAllowed()).customWebhooksAllowed(entitlements.customWebhooksAllowed()).csvExportsAllowed(entitlements.csvExportsAllowed()).emailDigestsAllowed(entitlements.emailDigestsAllowed()).altchaAllowed(entitlements.altchaAllowed()).maxRateLimitRpm(entitlements.maxRateLimitRpm()).maxFileSizeBytes(entitlements.maxFileSizeBytes()).build();

			tenantApi.updateTenantEntitlements(tenantId, updatedEntitlements);

			entitlementsApi.updateEntitlementsCache(tenantId, updatedEntitlements);

			// Reset submissions balance cache in Redis
			String key = getRedisKey(tenantId);
			redisTemplate.opsForValue().set(key, String.valueOf(updatedEntitlements.submissionsLimit()), Expiration.from(CACHE_TTL_HOURS, TimeUnit.HOURS));
			log.info("Entitlements monthly refresh boundary crossed. Reset submission counter to {} and refreshAt to {} for tenant: {}", updatedEntitlements.submissionsLimit(), nextRefresh, tenantId);
		}
	}

	private long syncAndCacheMeterBalance(UUID tenantId) {
		String key = getRedisKey(tenantId);
		try {
			Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
			long liveBalance;

			if (entitlements.isFree()) {
				Instant cycleStart = entitlements.refreshAt() != null ? entitlements.refreshAt().minus(30, ChronoUnit.DAYS) : Instant.now().minus(30, ChronoUnit.DAYS);
				OffsetDateTime since = OffsetDateTime.ofInstant(cycleStart, ZoneOffset.UTC);
				long consumed = dbSubmissionCounter.countSubmissionsAfter(tenantId, since);
				liveBalance = Math.max(0, entitlements.submissionsLimit() - consumed);
				log.debug("Free-tier tenant local submissions balance evaluated. Limit: {}, Consumed: {}, Remaining: {}", entitlements.submissionsLimit(), consumed, liveBalance);
			} else {
				liveBalance = polarMeterService.getRemainingSubmissionsBalance(tenantId);
			}

			redisTemplate.opsForValue().set(key, String.valueOf(liveBalance), Expiration.from(CACHE_TTL_HOURS, TimeUnit.HOURS));

			log.debug("Synchronized Redis meter balance cache ({}) for tenant: {}", liveBalance, tenantId);
			return liveBalance;
		} catch (Exception e) {
			log.error("Failed to sync updated Polar meter/local balance to Redis cache for tenant: {}", tenantId, e);
			return 0L;
		}
	}

	private String getRedisKey(UUID tenantId) {
		return String.format("formbox:%s:%s", CacheNames.METER_BALANCE, tenantId);
	}
}