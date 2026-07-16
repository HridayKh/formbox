package in.hridaykh.formbox.billing.service;

import in.hridaykh.formbox.billing.model.ActiveSubscriptions;
import in.hridaykh.formbox.billing.model.CustomerStateChanged;
import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.billing.model.GrantedBenefits;
import in.hridaykh.formbox.constant.FreeTierDefaults;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
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
public class WebhookService {
	private final ObjectMapper objectMapper;
	private final TenantRepository tenantRepository;

	public void processHook(String rawBody) {
		JsonNode root = objectMapper.readTree(rawBody);
		if (!"customer.state_changed".equals(root.path("type").asString())) {
			log.error("Invalid event type received\n{}", rawBody);
			throw new IllegalArgumentException();
		}
		JsonNode dataNode = root.path("data");
		if (dataNode.isMissingNode()) {
			log.error("Data node missing in webhook\n{}", rawBody);
			throw new IllegalArgumentException();
		}
		var state = objectMapper.convertValue(dataNode, new TypeReference<CustomerStateChanged>() {
		});
		UUID tenantId = UUID.fromString(state.externalId());
		var tenantOptional = tenantRepository.findById(tenantId);
		if (tenantOptional.isEmpty()) {
			log.error("tenant given in webhook not found\n{}", rawBody);
			throw new IllegalArgumentException();
		}
		Tenant tenant = tenantOptional.get();
		tenant.setEntitlements(createEntitlements(state));
		tenantRepository.save(tenant);
		log.info("Entitlements updated for tenant {} (tier: {})", tenantId, tenant.getEntitlements().tierName());
	}

	/**
	 * Builds the full Entitlements snapshot from the Polar customer state.
	 * <p>
	 * Everything is derived from the webhook payload:
	 * <ul>
	 *   <li>Tier identity: from Feature Flag benefits with {@code tier_priority} + {@code tier_name} in metadata</li>
	 *   <li>Boolean feature flags: from Feature Flag benefits with {@code feature_key} in metadata</li>
	 *   <li>Numeric limits: from Feature Flag benefits with {@code limit_key} + {@code limit_value} in metadata</li>
	 *   <li>Meter limits: from Feature Flag benefits with {@code meter_key} + {@code meter_limit} in metadata</li>
	 *   <li>Refresh timing: calculated from active subscription billing intervals</li>
	 * </ul>
	 */
	private Entitlements createEntitlements(CustomerStateChanged state) {
		var eb = Entitlements.builder();

		// --- 1. Parse all granted benefits metadata ---
		String tierName = FreeTierDefaults.TIER_NAME;
		int highestPriority = FreeTierDefaults.TIER_PRIORITY;
		Set<String> enabledFeatures = new HashSet<>();
		Map<String, Long> numericLimits = new HashMap<>();
		Map<String, Long> meterLimits = new HashMap<>();

		for (GrantedBenefits benefit : nullSafe(state.grantedBenefits())) {
			Map<String, String> meta = benefit.benefitMetadata();
			if (meta == null || meta.isEmpty()) continue;

			// Tier identity benefit: metadata contains tier_priority and tier_name
			if (meta.containsKey("tier_priority")) {
				try {
					int priority = Integer.parseInt(meta.get("tier_priority"));
					if (priority > highestPriority) {
						highestPriority = priority;
						tierName = meta.getOrDefault("tier_name", tierName);
					}
				} catch (NumberFormatException e) {
					log.warn("Invalid tier_priority in benefit {}: {}", benefit.benefitId(), meta.get("tier_priority"), e);
				}
			}

			// Boolean feature flag: metadata contains feature_key
			String featureKey = meta.get("feature_key");
			if (featureKey != null && !featureKey.isBlank()) {
				enabledFeatures.add(featureKey);
			}

			// Numeric limit: metadata contains limit_key + limit_value
			String limitKey = meta.get("limit_key");
			String limitValue = meta.get("limit_value");
			if (limitKey != null && limitValue != null) {
				try {
					long value = Long.parseLong(limitValue);
					numericLimits.merge(limitKey, value, Math::max);
				} catch (NumberFormatException e) {
					log.warn("Invalid limit_value for {} in benefit {}: {}", limitKey, benefit.benefitId(), limitValue, e);
				}
			}

			// Meter limit: metadata contains meter_key + meter_limit
			String meterKey = meta.get("meter_key");
			String meterLimit = meta.get("meter_limit");
			if (meterKey != null && meterLimit != null) {
				try {
					long limit = Long.parseLong(meterLimit);
					meterLimits.merge(meterKey, limit, Math::max);
				} catch (NumberFormatException e) {
					log.warn("Invalid meter_limit for {} in benefit {}: {}", meterKey, benefit.benefitId(), meterLimit);
				}
			}
		}

		// --- 2. Tier identity ---
		eb.tierName(tierName);
		eb.tierPriority(highestPriority);

		// --- 3. Refresh timing ---
		eb.refreshAt(calculateNextRefreshAt(state.activeSubscriptions()));

		// --- 4. Meter limits (max caps, defaults from FreeTierDefaults) ---
		eb.submissionsLimit(meterLimits.getOrDefault("submissions", FreeTierDefaults.SUBMISSIONS_LIMIT));
		eb.formsLimit(meterLimits.getOrDefault("forms", FreeTierDefaults.FORMS_LIMIT));
		eb.storageLimitBytes(meterLimits.getOrDefault("storage", FreeTierDefaults.STORAGE_LIMIT_BYTES));

		// --- 5. Boolean feature flags (present in granted benefits = enabled) ---
		eb.discordNotifsAllowed(enabledFeatures.contains("discord_notifs_allowed"));
		eb.turnstileAllowed(enabledFeatures.contains("turnstile_allowed"));
		eb.redirectUrlsAllowed(enabledFeatures.contains("redirect_urls_allowed"));
		eb.jsonFormsAllowed(enabledFeatures.contains("json_forms_allowed"));
		eb.fileUploadsAllowed(enabledFeatures.contains("file_uploads_allowed"));
		eb.fieldValidationsAllowed(enabledFeatures.contains("field_validations_allowed"));
		eb.slackNotifsAllowed(enabledFeatures.contains("slack_notifs_allowed"));
		eb.telegramNotifsAllowed(enabledFeatures.contains("telegram_notifs_allowed"));
		eb.customWebhooksAllowed(enabledFeatures.contains("custom_webhooks_allowed"));
		eb.csvExportsAllowed(enabledFeatures.contains("csv_exports_allowed"));
		eb.emailDigestsAllowed(enabledFeatures.contains("email_digests_allowed"));
		eb.altchaAllowed(enabledFeatures.contains("altcha_allowed"));

		// --- 6. Numeric limits (take the highest across all benefits, default to free tier) ---
		eb.maxRateLimitRpm((int) (long) numericLimits.getOrDefault("max_rate_limit_rpm", (long) FreeTierDefaults.MAX_RATE_LIMIT_RPM));
		eb.maxFileSizeBytes(numericLimits.getOrDefault("max_file_size_bytes", FreeTierDefaults.MAX_FILE_SIZE_BYTES));

		return eb.build();
	}

	/**
	 * Determines the next meter refresh timestamp.
	 * <ul>
	 *   <li>Monthly subscriptions (period ≤ 35 days): use {@code currentPeriodEnd} — Polar auto-refreshes</li>
	 *   <li>Annual/LTD (period > 35 days or no period): set to now + 30 days — scheduler will reset Polar meters manually</li>
	 * </ul>
	 * Takes the EARLIEST across all active subscriptions.
	 */
	private Instant calculateNextRefreshAt(List<ActiveSubscriptions> subscriptions) {
		Instant earliest = null;

		for (var subscription : nullSafe(subscriptions)) {
			Instant refresh;
			if (subscription.currentPeriodStart() != null && subscription.currentPeriodEnd() != null) {
				long periodDays = Duration.between(subscription.currentPeriodStart(), subscription.currentPeriodEnd()).toDays();
				if (periodDays <= 35) {
					// Monthly subscription — Polar handles meter refresh at period end
					refresh = subscription.currentPeriodEnd();
				} else {
					// Annual or longer — need manual monthly refresh
					refresh = calculateNextMonthlyBoundary(subscription.currentPeriodStart());
				}
			} else {
				// Lifetime / one-time — no period end, refresh monthly from now
				refresh = Instant.now().plus(30, ChronoUnit.DAYS);
			}

			if (earliest == null || refresh.isBefore(earliest)) {
				earliest = refresh;
			}
		}

		// Fallback: 30 days from now (new free tier user with no subscriptions)
		return earliest != null ? earliest : Instant.now().plus(30, ChronoUnit.DAYS);
	}

	/**
	 * For annual/long subscriptions: find the next monthly boundary
	 * (periodStart + N months) that falls in the future.
	 */
	private Instant calculateNextMonthlyBoundary(Instant periodStart) {
		Instant now = Instant.now();
		Instant boundary = periodStart;
		// Walk forward month by month until we find one in the future
		while (!boundary.isAfter(now)) {
			boundary = boundary.plus(30, ChronoUnit.DAYS);
		}
		return boundary;
	}

	private <T> List<T> nullSafe(List<T> list) {
		return list != null ? list : List.of();
	}
}
