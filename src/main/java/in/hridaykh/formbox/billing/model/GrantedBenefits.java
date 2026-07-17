package in.hridaykh.formbox.billing.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GrantedBenefits(
	UUID id,
	Instant createdAt,
	Instant modifiedAt,
	Instant grantedAt,
	UUID benefitId,
	Map<String, String> benefitMetadata,
	Map<String, String> properties
) {
}
