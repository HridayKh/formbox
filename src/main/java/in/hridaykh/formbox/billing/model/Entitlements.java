package in.hridaykh.formbox.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import in.hridaykh.formbox.constant.FreeTierDefaults;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Duration;
import java.time.Instant;

/**
 * Entitlements snapshot stored as JSONB on the tenant row.
 * <p>
 * Contains tier identity, feature flags, and meter limits.
 * All values are derived from Polar's granted benefits and active meters
 * via the {@code customer.state_changed} webhook.
 * <p>
 * Hot counters (actual submission usage) live in Redis/Polar, not here.
 * This record stores only the MAX LIMITS and feature flags.
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Entitlements(
	@JsonProperty("tier_name") String tierName,
	@JsonProperty("tier_priority") int tierPriority,
	@JsonProperty("refresh_at") Instant refreshAt,
	@JsonProperty("recurring_interval") String recurringInterval,

	// Meter limits (max caps, NOT live usage)
	@JsonProperty("submissions_limit") long submissionsLimit,
	@JsonProperty("forms_limit") long formsLimit,
	@JsonProperty("storage_limit_bytes") long storageLimitBytes,

	// Boolean feature flags (driven by Polar Feature Flag benefits)
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

	// Numeric limits (driven by Polar Feature Flag benefit metadata)
	@JsonProperty("max_rate_limit_rpm") int maxRateLimitRpm,
	@JsonProperty("max_file_size_bytes") long maxFileSizeBytes
) {

	/**
	 * Returns default entitlements matching the free tier.
	 * Used for new tenants before Polar provisions their subscription.
	 */
	public static Entitlements freeDefaults() {
		return Entitlements.builder()
			.tierName(FreeTierDefaults.TIER_NAME)
			.tierPriority(FreeTierDefaults.TIER_PRIORITY)
			.refreshAt(Instant.now().plus(Duration.ofDays(30)))
			.recurringInterval("free")
			.submissionsLimit(FreeTierDefaults.SUBMISSIONS_LIMIT)
			.formsLimit(FreeTierDefaults.FORMS_LIMIT)
			.storageLimitBytes(FreeTierDefaults.STORAGE_LIMIT_BYTES)
			.maxRateLimitRpm(FreeTierDefaults.MAX_RATE_LIMIT_RPM)
			.maxFileSizeBytes(FreeTierDefaults.MAX_FILE_SIZE_BYTES)
			.build();
	}

	/**
	 * Convenience: is this tenant on the free tier?
	 */
	@JsonIgnore
	public boolean isFree() {
		return FreeTierDefaults.TIER_NAME.equalsIgnoreCase(tierName);
	}
}