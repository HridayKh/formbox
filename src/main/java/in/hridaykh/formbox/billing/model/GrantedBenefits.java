package in.hridaykh.formbox.billing.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
