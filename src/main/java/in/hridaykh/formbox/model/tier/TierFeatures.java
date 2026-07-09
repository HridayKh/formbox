package in.hridaykh.formbox.model.tier;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TierFeatures(
	@JsonProperty("name") String name,
	@JsonProperty("max_forms") int maxForms,
	@JsonProperty("turnstile_allowed") boolean turnstileAllowed,
	@JsonProperty("redirect_url_allowed") boolean redirectUrlAllowed,
	@JsonProperty("max_rate_limit_rpm") int maxRateLimitRpm,
	@JsonProperty("htmx_allowed") boolean htmxAllowed,
	@JsonProperty("json_allowed") boolean jsonAllowed,
	@JsonProperty("files_allowed") boolean filesAllowed,
	@JsonProperty("max_file_size_mb") int maxFileSizeMb,
	@JsonProperty("total_storage_mb") long totalStorageMb,
	@JsonProperty("field_validations_allowed") boolean fieldValidationsAllowed,
	@JsonProperty("notifs") NotificationFeatures notifs,
	@JsonProperty("webhooks_allowed") boolean webhooksAllowed
) {
}
