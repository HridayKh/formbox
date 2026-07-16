package in.hridaykh.formbox.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolarBenefitResponse(
	@JsonProperty("id") UUID id,
	@JsonProperty("created_at") OffsetDateTime createdAt,
	@JsonProperty("modified_at") OffsetDateTime modifiedAt,
	@JsonProperty("type") String type,
	@JsonProperty("description") String description,
	@JsonProperty("selectable") Boolean selectable,
	@JsonProperty("deletable") Boolean deletable,
	@JsonProperty("is_deleted") Boolean isDeleted,
	@JsonProperty("organization_id") UUID organizationId,
	@JsonProperty("metadata") Map<String, String> metadata,
	@JsonProperty("visibility") String visibility,
	@JsonProperty("properties") Map<String, Object> properties,
	@JsonProperty("visibility_configurable") Boolean visibilityConfigurable
) {
}

