package in.hridaykh.formbox.billing.service;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import sh.polar.sdk.http.PolarHttpClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
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
public class PolarCacheService {

	private static final long CACHE_TTL_HOURS = 2;

	private final StringRedisTemplate redisTemplate;
	private final PolarMeterService polarMeterService;
	private final TenantRepository tenantRepository;
	private final SubmissionRepository submissionRepository;
	private final PolarHttpClient polarHttpClient;
	private final ObjectMapper objectMapper;
	private final EntitlementsCacheService entitlementsCacheService;

	@WithSpan
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

	private void ensureEntitlementsRefresh(UUID tenantId) {
		Entitlements entitlements = entitlementsCacheService.getEntitlements(tenantId);
		if (entitlements.refreshAt() != null && Instant.now().isAfter(entitlements.refreshAt())) {
			Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
			if (tenant == null) {
				return;
			}
			Instant nextRefresh = entitlements.refreshAt();
			Instant now = Instant.now();
			while (!nextRefresh.isAfter(now)) {
				nextRefresh = nextRefresh.plus(30, ChronoUnit.DAYS);
			}

			Entitlements updated = Entitlements.builder()
				.tierName(entitlements.tierName())
				.tierPriority(entitlements.tierPriority())
				.refreshAt(nextRefresh)
				.recurringInterval(entitlements.recurringInterval())
				.submissionsLimit(entitlements.submissionsLimit())
				.formsLimit(entitlements.formsLimit())
				.storageLimitBytes(entitlements.storageLimitBytes())
				.discordNotifsAllowed(entitlements.discordNotifsAllowed())
				.turnstileAllowed(entitlements.turnstileAllowed())
				.redirectUrlsAllowed(entitlements.redirectUrlsAllowed())
				.jsonFormsAllowed(entitlements.jsonFormsAllowed())
				.fileUploadsAllowed(entitlements.fileUploadsAllowed())
				.fieldValidationsAllowed(entitlements.fieldValidationsAllowed())
				.slackNotifsAllowed(entitlements.slackNotifsAllowed())
				.telegramNotifsAllowed(entitlements.telegramNotifsAllowed())
				.customWebhooksAllowed(entitlements.customWebhooksAllowed())
				.csvExportsAllowed(entitlements.csvExportsAllowed())
				.emailDigestsAllowed(entitlements.emailDigestsAllowed())
				.altchaAllowed(entitlements.altchaAllowed())
				.maxRateLimitRpm(entitlements.maxRateLimitRpm())
				.maxFileSizeBytes(entitlements.maxFileSizeBytes())
				.build();

			tenant.setEntitlements(updated);
			tenantRepository.saveAndFlush(tenant);
			entitlementsCacheService.updateEntitlementsCache(tenantId, updated);

			// Reset submissions balance cache in Redis
			String key = getRedisKey(tenantId);
			redisTemplate.opsForValue().set(key, String.valueOf(updated.submissionsLimit()), Expiration.from(CACHE_TTL_HOURS, TimeUnit.HOURS));
			log.info("Entitlements monthly refresh boundary crossed. Reset submission counter to {} and refreshAt to {} for tenant: {}", 
				updated.submissionsLimit(), nextRefresh, tenantId);
		}
	}

	@WithSpan
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

	@WithSpan
	public long syncAndCacheMeterBalance(UUID tenantId) {
		String key = getRedisKey(tenantId);
		try {
			Entitlements entitlements = entitlementsCacheService.getEntitlements(tenantId);
			long liveBalance;

			if (entitlements.isFree()) {
				// Calculate local free-tier balance
				Instant cycleStart = entitlements.refreshAt() != null 
					? entitlements.refreshAt().minus(30, ChronoUnit.DAYS) 
					: Instant.now().minus(30, ChronoUnit.DAYS);
				OffsetDateTime since = OffsetDateTime.ofInstant(cycleStart, ZoneOffset.UTC);
				long consumed = submissionRepository.countByTenantIdAndCreatedAtAfter(tenantId, since);
				liveBalance = Math.max(0, entitlements.submissionsLimit() - consumed);
				log.debug("Free-tier tenant local submissions balance evaluated. Limit: {}, Consumed: {}, Remaining: {}", 
					entitlements.submissionsLimit(), consumed, liveBalance);
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

	@WithSpan
	public String getPolarProductIdBySlug(String slug) {
		String targetName = slug;
		if ("starter-v1".equalsIgnoreCase(slug)) {
			targetName = "Starter Monthly";
		} else if ("pro-v1".equalsIgnoreCase(slug)) {
			targetName = "Pro Monthly";
		}

		String redisKey = "formbox:product-slug-id:" + targetName.toLowerCase().replace(" ", "-");
		String cachedId = redisTemplate.opsForValue().get(redisKey);
		if (cachedId != null) {
			return cachedId;
		}

		try {
			log.debug("Cache miss for product name/slug {}. Fetching live products list from Polar...", targetName);
			String responseJson = polarHttpClient.get("/products/", String.class);
			JsonNode root = objectMapper.readTree(responseJson);
			String targetId = null;

			for (JsonNode item : root.path("items")) {
				String itemSlug = item.path("name").asString().toLowerCase().replace(" ", "-");
				String itemName = item.path("name").asString();
				String itemId = item.path("id").asString();

				// Cache resolved product ID
				redisTemplate.opsForValue().set("formbox:product-slug-id:" + itemSlug, itemId, Duration.ofDays(7));

				if (itemName.equalsIgnoreCase(targetName) || itemSlug.equalsIgnoreCase(targetName.replace(" ", "-"))) {
					targetId = itemId;
				}
			}
			return targetId;
		} catch (Exception e) {
			log.error("Failed to retrieve or parse Polar products list for target: {}", targetName, e);
			return null;
		}
	}
}