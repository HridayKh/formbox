package in.hridaykh.formbox.billing.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CustomerStateChanged(
	UUID id,
	Instant createdAt,
	Instant modifiedAt,
	Map<String, Object> metadata,
	String email,
	boolean emailVerified,
	String type, // individual or team
	String name,
	String billingName,
	Map<String, String> billingAddress,
	Map<String, String> taxId,
	String organizationId,
	Instant deletedAt,
	List<ActiveSubscriptions> activeSubscriptions,
	List<GrantedBenefits> grantedBenefits,
	List<ActiveMeters> activeMeters,
	String avatarUrl,
	String externalId,
	String locale,
	UUID defaultPaymentMethodId
) {
}
