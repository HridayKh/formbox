package in.hridaykh.formbox.billing.service;

import in.hridaykh.formbox.billing.PolarIdProperties;
import in.hridaykh.formbox.billing.model.ActiveMeters;
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
	private final PolarIdProperties polarIdProperties;

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
	 * Everything is derived from the webhook payload — no local table lookups:
	 * <ul>
	 *   <li>Tier identity: Feature Flag benefits with {@code tier_priority} + {@code tier_name} in metadata</li>
	 *   <li>Boolean feature flags: Feature Flag benefits with {@code feature_key} in metadata</li>
	 *   <li>Numeric limits (forms, rate limit, etc.): Feature Flag benefits with {@code limit_key} + {@code limit_value}</li>
	 *   <li>Meter limits (submissions): Native Polar meter data from {@code activeMeters} (creditedUnits from Credits benefit)</li>
	 *   <li>Refresh timing: Calculated from active subscription billing intervals</li>
	 * </ul>
	 */
	private Entitlements createEntitlements(CustomerStateChanged state) {
		var eb = Entitlements.builder();

		// --- 1. Parse granted benefits metadata ---
		String tierName = FreeTierDefaults.TIER_NAME;
		int highestPriority = FreeTierDefaults.TIER_PRIORITY;
		Set<String> enabledFeatures = new HashSet<>();
		Map<String, Long> numericLimits = new HashMap<>();

		for (GrantedBenefits benefit : nullSafe(state.grantedBenefits())) {
			Map<String, String> meta = benefit.benefitMetadata();
			if (meta == null || meta.isEmpty()) continue;

			// Tier identity benefit: metadata has tier_priority and tier_name
			if (meta.containsKey("tier_priority")) {
				try {
					int priority = Integer.parseInt(meta.get("tier_priority"));
					if (priority > highestPriority) {
						highestPriority = priority;
						tierName = meta.getOrDefault("tier_name", tierName);
					}
				} catch (NumberFormatException e) {
					log.warn("Invalid tier_priority in benefit {}: {}", benefit.benefitId(), meta.get("tier_priority"));
				}
			}

			// Boolean feature flag: metadata has feature_key
			String featureKey = meta.get("feature_key");
			if (featureKey != null && !featureKey.isBlank()) {
				enabledFeatures.add(featureKey);
			}

			// Numeric limit: metadata has limit_key + limit_value
			String limitKey = meta.get("limit_key");
			String limitValue = meta.get("limit_value");
			if (limitKey != null && limitValue != null) {
				try {
					long value = Long.parseLong(limitValue);
					numericLimits.merge(limitKey, value, Math::max);
				} catch (NumberFormatException e) {
					log.warn("Invalid limit_value for {} in benefit {}: {}", limitKey, benefit.benefitId(), limitValue);
				}
			}
		}

		// --- 2. Tier identity ---
		eb.tierName(tierName);
		eb.tierPriority(highestPriority);

		// --- 3. Refresh timing ---
		eb.refreshAt(calculateNextRefreshAt(state.activeSubscriptions()));
		eb.recurringInterval(calculateRecurringInterval(state.activeSubscriptions()));

		// --- 4. Submissions limit from Polar's native meter data ---
		// Credits benefit auto-credits the meter; we read creditedUnits from activeMeters.
		// The meter ID is matched via config (polar-ids.submission-meter-id).
		long submissionsLimit = FreeTierDefaults.SUBMISSIONS_LIMIT;
		for (ActiveMeters meter : nullSafe(state.activeMeters())) {
			if (meter.meterId() != null
				&& meter.meterId().toString().equalsIgnoreCase(polarIdProperties.getSubmissionMeterId())) {
				submissionsLimit = meter.creditedUnits() != null ? meter.creditedUnits().longValue() : submissionsLimit;
				log.debug("Submissions meter found: credited={}, consumed={}, balance={}",
					meter.creditedUnits(), meter.consumedUnits(), meter.balance());
				break;
			}
		}
		eb.submissionsLimit(submissionsLimit);

		// --- 5. Other limits from Feature Flag benefit metadata ---
		// (forms and storage aren't Polar meters — they're caps enforced by the app)
		eb.formsLimit(numericLimits.getOrDefault("forms_limit", FreeTierDefaults.FORMS_LIMIT));
		eb.storageLimitBytes(numericLimits.getOrDefault("storage_limit_bytes", FreeTierDefaults.STORAGE_LIMIT_BYTES));

		// --- 6. Boolean feature flags (present = enabled, absent = disabled) ---
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

		// --- 7. Numeric limits (take highest across all benefits) ---
		eb.maxRateLimitRpm((int) (long) numericLimits.getOrDefault(
			"max_rate_limit_rpm", (long) FreeTierDefaults.MAX_RATE_LIMIT_RPM));
		eb.maxFileSizeBytes(numericLimits.getOrDefault(
			"max_file_size_bytes", FreeTierDefaults.MAX_FILE_SIZE_BYTES));

		return eb.build();
	}

	/**
	 * Determines the next meter refresh timestamp.
	 * <p>
	 * Monthly subs: use {@code currentPeriodEnd} — Polar auto-refreshes Credits.
	 * Annual/LTD: set to the next monthly boundary from period start.
	 * The lazy refresh check at submission time will handle resetting the Redis counter
	 * and re-crediting Polar meters when this timestamp passes.
	 */
	private Instant calculateNextRefreshAt(List<ActiveSubscriptions> subs) {
		Instant earliest = null;

		for (var sub : nullSafe(subs)) {
			Instant refresh;
			if (sub.currentPeriodStart() != null && sub.currentPeriodEnd() != null) {
				long periodDays = Duration.between(sub.currentPeriodStart(), sub.currentPeriodEnd()).toDays();
				if (periodDays <= 35) {
					// Monthly — Polar handles meter refresh at period end
					refresh = sub.currentPeriodEnd();
				} else {
					// Annual or longer — need manual monthly refresh
					refresh = calculateNextMonthlyBoundary(sub.currentPeriodStart());
				}
			} else {
				// Lifetime / one-time — no period end, refresh monthly from now
				refresh = Instant.now().plus(30, ChronoUnit.DAYS);
			}

			if (earliest == null || refresh.isBefore(earliest)) {
				earliest = refresh;
			}
		}

		return earliest != null ? earliest : Instant.now().plus(30, ChronoUnit.DAYS);
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

	private String calculateRecurringInterval(List<ActiveSubscriptions> subs) {
		if (subs == null || subs.isEmpty()) {
			return "free";
		}
		String interval = "one_time";
		for (var sub : subs) {
			if (sub.currentPeriodStart() != null && sub.currentPeriodEnd() != null) {
				long periodDays = Duration.between(sub.currentPeriodStart(), sub.currentPeriodEnd()).toDays();
				if (periodDays <= 35) {
					return "month";
				} else {
					interval = "year";
				}
			}
		}
		return interval;
	}

	private <T> List<T> nullSafe(List<T> list) {
		return list != null ? list : List.of();
	}
}
