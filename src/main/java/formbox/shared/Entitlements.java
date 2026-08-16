package formbox.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Duration;
import java.time.Instant;

/**
 * Entitlements snapshot stored as JSONB on the tenant row.
 * <p>
 * Contains subscription state, tier identity, feature flags, and meter limits.
 * All values are derived from Polar product metadata and subscription state
 * via the {@code subscription.updated} webhook.
 * <p>
 * Hot counters (actual submission usage) live in Redis/Polar, not here.
 * This record stores only the MAX LIMITS, feature flags, and subscription state.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Entitlements(
	// Subscription state (tracked for DB persistence and UI)
	@JsonProperty("subscription_status") String subscriptionStatus,
	@JsonProperty("subscription_id") String subscriptionId,
	@JsonProperty("product_id") String productId,
	@JsonProperty("cancel_at_period_end") Boolean cancelAtPeriodEnd,
	@JsonProperty("current_period_end") Instant currentPeriodEnd,

	// Tier identity
	@JsonProperty("tier_name") String tierName,
	@JsonProperty("tier_priority") int tierPriority,
	@JsonProperty("refresh_at") Instant refreshAt,
	@JsonProperty("recurring_interval") String recurringInterval,

	// Meter limits (max caps, NOT live usage)
	@JsonProperty("submissions_limit") long submissionsLimit,
	@JsonProperty("forms_limit") long formsLimit,
	@JsonProperty("storage_limit_bytes") long storageLimitBytes,
	@JsonProperty("max_email_notif_recipients") long maxEmailNotifRecipients,

	// Boolean feature flags (driven by Polar product metadata)
	@JsonProperty("discord_notifs_allowed") boolean discordNotifsAllowed,
	@JsonProperty("turnstile_allowed") boolean turnstileAllowed,
	@JsonProperty("redirect_urls_allowed") boolean redirectUrlsAllowed,
	@JsonProperty("json_forms_allowed") boolean jsonFormsAllowed,
	@JsonProperty("file_uploads_allowed") boolean fileUploadsAllowed,
	@JsonProperty("field_validations_allowed") boolean fieldValidationsAllowed,
	@JsonProperty("slack_notifs_allowed") boolean slackNotifsAllowed,
	@JsonProperty("telegram_notifs_allowed") boolean telegramNotifsAllowed,
	@JsonProperty("custom_webhooks_allowed") boolean customWebhooksAllowed,
	@JsonProperty("csv_exports_allowed") boolean csvExportsAllowed,
	@JsonProperty("email_digests_allowed") boolean emailDigestsAllowed,
	@JsonProperty("altcha_allowed") boolean altchaAllowed,

	// Numeric limits (driven by Polar product metadata)
	@JsonProperty("max_rate_limit_rpm") int maxRateLimitRpm,
	@JsonProperty("max_file_size_bytes") long maxFileSizeBytes,
	@JsonProperty("retention_days") int retentionDays
) {

	/**
	 * Returns default entitlements matching the free tier.
	 * Used for new tenants before Polar provisions their subscription,
	 * or when a subscription is revoked/canceled/paused.
	 */
	public static Entitlements freeDefaults() {
		return Entitlements.builder()
			.subscriptionStatus(FreeTierDefaults.SUBSCRIPTION_STATUS)
			.tierName(FreeTierDefaults.TIER_NAME)
			.tierPriority(FreeTierDefaults.TIER_PRIORITY)
			.refreshAt(Instant.now().plus(Duration.ofDays(30)))
			.recurringInterval("free")
			.submissionsLimit(FreeTierDefaults.SUBMISSIONS_LIMIT)
			.formsLimit(FreeTierDefaults.FORMS_LIMIT)
			.storageLimitBytes(FreeTierDefaults.STORAGE_LIMIT_BYTES)
			.maxRateLimitRpm(FreeTierDefaults.MAX_RATE_LIMIT_RPM)
			.maxFileSizeBytes(FreeTierDefaults.MAX_FILE_SIZE_BYTES)
			.maxEmailNotifRecipients(FreeTierDefaults.MAX_EMAIL_NOTIF_RECIPIENTS)
			.retentionDays(FreeTierDefaults.RETENTION_DAYS)
			.build();
	}

	@JsonIgnore
	public int retentionDaysOrDefault() {
		return retentionDays > 0 ? retentionDays : FreeTierDefaults.RETENTION_DAYS;
	}

	/**
	 * Convenience: is this tenant on the free tier?
	 */
	@JsonIgnore
	public boolean isFree() {
		return FreeTierDefaults.TIER_NAME.equalsIgnoreCase(tierName);
	}

	/**
	 * Convenience: does the tenant have an active (access-granting) subscription?
	 * Active states are: active, trialing, past_due (grace period).
	 */
	@JsonIgnore
	public boolean hasActiveSubscription() {
		return "active".equals(subscriptionStatus)
			|| "trialing".equals(subscriptionStatus)
			|| "past_due".equals(subscriptionStatus);
	}
}