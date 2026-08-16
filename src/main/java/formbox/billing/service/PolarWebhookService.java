package formbox.billing.service;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.billing.model.SubscriptionUpdated;
import formbox.shared.Entitlements;
import formbox.shared.FreeTierDefaults;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolarWebhookService {
	private final ObjectMapper objectMapper;
	private final EntitlementsApi entitlementsApi;
	private final TenantApi tenantApi;
	private final formbox.shared.RedisCache redisCache;

	/**
	 * Access-granting subscription statuses.
	 * active: normal paid state, trialing: free trial, past_due: payment failed but in grace period.
	 */
	private static final Set<String> ACTIVE_STATUSES = Set.of("active", "trialing", "past_due");

	@WithSpan
	public void processHook(String rawBody) {
		JsonNode root = objectMapper.readTree(rawBody);
		String eventType = root.path("type").asString();

		if (!"subscription.updated".equals(eventType)) {
			log.info("Ignoring unhandled webhook event type: {}", eventType);
			return;
		}

		JsonNode dataNode = root.path("data");
		if (dataNode.isMissingNode()) {
			log.warn("Data node missing in subscription.updated webhook: {}", rawBody);
			throw new IllegalArgumentException("Missing data node");
		}

		var subscription = objectMapper.convertValue(dataNode, new TypeReference<SubscriptionUpdated>() {});

		String externalId = subscription.customer() != null ? subscription.customer().externalId() : null;
		if (externalId == null || externalId.isBlank()) {
			log.error("No external_id on customer for subscription {}. Cannot identify tenant.", subscription.id());
			throw new IllegalArgumentException("Missing customer external_id");
		}

		UUID tenantId = UUID.fromString(externalId);
		Entitlements entitlements = createEntitlements(subscription);

		tenantApi.updateTenantEntitlementsInDb(tenantId, entitlements);
		entitlementsApi.updateEntitlementsCache(tenantId, entitlements);
		redisCache.delete(formbox.shared.CacheNames.METER_BALANCE, tenantId.toString());
		
		log.info("Entitlements updated for tenant {} (tier: {}, status: {})", tenantId, entitlements.tierName(), entitlements.subscriptionStatus());
	}

	/**
	 * Builds the full Entitlements snapshot from a Polar subscription.updated payload.
	 * <p>
	 * All plan configuration is derived from the product's metadata — no local product
	 * table or benefit-grant parsing needed. The product metadata keys are:
	 * <ul>
	 *   <li>{@code tier_name}, {@code tier_priority}: Tier identity</li>
	 *   <li>{@code forms_limit}, {@code submissions_limit}, {@code storage_limit_bytes}, etc.: Numeric limits</li>
	 *   <li>{@code discord_notifs_allowed}, {@code file_uploads_allowed}, etc.: Boolean feature flags</li>
	 * </ul>
	 * <p>
	 * For non-access-granting statuses (canceled, paused, revoked, unpaid), entitlements
	 * fall back to free tier defaults while preserving the subscription state for UI display.
	 */
	private Entitlements createEntitlements(SubscriptionUpdated sub) {
		String status = sub.status();

		// Non-access statuses → free tier defaults, but preserve subscription state for UI
		if (!ACTIVE_STATUSES.contains(status)) {
			log.info("Subscription {} has non-active status '{}'. Reverting to free tier defaults.", sub.id(), status);
			return Entitlements.freeDefaults().toBuilder()
				.subscriptionStatus(status)
				.subscriptionId(sub.id())
				.productId(sub.productId())
				.cancelAtPeriodEnd(Boolean.TRUE.equals(sub.cancelAtPeriodEnd()))
				.currentPeriodEnd(sub.currentPeriodEnd())
				.build();
		}

		// Active subscription → derive entitlements from product metadata
		Map<String, String> meta = sub.product() != null && sub.product().metadata() != null
			? sub.product().metadata()
			: Map.of();

		var eb = Entitlements.builder();

		// --- 1. Subscription state ---
		eb.subscriptionStatus(status);
		eb.subscriptionId(sub.id());
		eb.productId(sub.productId());
		eb.cancelAtPeriodEnd(Boolean.TRUE.equals(sub.cancelAtPeriodEnd()));
		eb.currentPeriodEnd(sub.currentPeriodEnd());

		// --- 2. Tier identity from product metadata ---
		eb.tierName(meta.getOrDefault("tier_name", FreeTierDefaults.TIER_NAME));
		eb.tierPriority(parseIntOrDefault(meta.get("tier_priority"), FreeTierDefaults.TIER_PRIORITY));

		// --- 3. Refresh timing ---
		eb.refreshAt(calculateRefreshAt(sub));
		eb.recurringInterval(sub.recurringInterval() != null ? sub.recurringInterval() : "free");

		// --- 4. Numeric limits from product metadata ---
		eb.submissionsLimit(parseLongOrDefault(meta.get("submissions_limit"), FreeTierDefaults.SUBMISSIONS_LIMIT));
		eb.formsLimit(parseLongOrDefault(meta.get("forms_limit"), FreeTierDefaults.FORMS_LIMIT));
		eb.storageLimitBytes(parseLongOrDefault(meta.get("storage_limit_bytes"), FreeTierDefaults.STORAGE_LIMIT_BYTES));
		eb.maxEmailNotifRecipients(parseLongOrDefault(meta.get("max_email_notif_recipients"), FreeTierDefaults.MAX_EMAIL_NOTIF_RECIPIENTS));
		eb.maxRateLimitRpm(parseIntOrDefault(meta.get("max_rate_limit_rpm"), FreeTierDefaults.MAX_RATE_LIMIT_RPM));
		eb.maxFileSizeBytes(parseLongOrDefault(meta.get("max_file_size_bytes"), FreeTierDefaults.MAX_FILE_SIZE_BYTES));
		eb.retentionDays(parseIntOrDefault(meta.get("retention_days"), FreeTierDefaults.RETENTION_DAYS));

		// --- 5. Boolean feature flags from product metadata ---
		eb.discordNotifsAllowed(parseBool(meta.get("discord_notifs_allowed")));
		eb.turnstileAllowed(parseBool(meta.get("turnstile_allowed")));
		eb.redirectUrlsAllowed(parseBool(meta.get("redirect_urls_allowed")));
		eb.jsonFormsAllowed(parseBool(meta.get("json_forms_allowed")));
		eb.fileUploadsAllowed(parseBool(meta.get("file_uploads_allowed")));
		eb.fieldValidationsAllowed(parseBool(meta.get("field_validations_allowed")));
		eb.slackNotifsAllowed(parseBool(meta.get("slack_notifs_allowed")));
		eb.telegramNotifsAllowed(parseBool(meta.get("telegram_notifs_allowed")));
		eb.customWebhooksAllowed(parseBool(meta.get("custom_webhooks_allowed")));
		eb.csvExportsAllowed(parseBool(meta.get("csv_exports_allowed")));
		eb.emailDigestsAllowed(parseBool(meta.get("email_digests_allowed")));
		eb.altchaAllowed(parseBool(meta.get("altcha_allowed")));

		return eb.build();
	}

	/**
	 * Determines the next meter refresh timestamp from the subscription's billing period.
	 * <p>
	 * Monthly subs: use {@code currentPeriodEnd} — Polar auto-refreshes Credits.
	 * Annual/LTD: set to the next monthly boundary from period start so usage
	 * cycles stay on a 30-day cadence regardless of billing interval.
	 */
	private Instant calculateRefreshAt(SubscriptionUpdated sub) {
		if (sub.currentPeriodStart() != null && sub.currentPeriodEnd() != null) {
			long periodDays = Duration.between(sub.currentPeriodStart(), sub.currentPeriodEnd()).toDays();
			if (periodDays <= 35) {
				// Monthly — Polar handles meter refresh at period end
				return sub.currentPeriodEnd();
			} else {
				// Annual or longer — need manual monthly refresh
				return calculateNextMonthlyBoundary(sub.currentPeriodStart());
			}
		}
		// No period (lifetime / one-time) — refresh monthly from now
		return Instant.now().plus(30, ChronoUnit.DAYS);
	}

	/**
	 * For annual/long subscriptions: find the next monthly boundary
	 * (periodStart + N*30 days) that falls in the future.
	 */
	private Instant calculateNextMonthlyBoundary(Instant periodStart) {
		Instant now = Instant.now();
		Instant boundary = periodStart;
		while (!boundary.isAfter(now)) {
			boundary = boundary.plus(30, ChronoUnit.DAYS);
		}
		return boundary;
	}

	// --- Parsing helpers ---

	private static boolean parseBool(String value) {
		return "true".equalsIgnoreCase(value);
	}

	private static int parseIntOrDefault(String value, int defaultValue) {
		if (value == null || value.isBlank()) return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static long parseLongOrDefault(String value, long defaultValue) {
		if (value == null || value.isBlank()) return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
