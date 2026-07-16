package in.hridaykh.formbox.billing.model;


import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ActiveSubscriptions(
	String id,
	Instant createdAt,
	Instant modifiedAt,
	Map<String, Object> metadata,
	Long amount,
	String currency,
	Instant currentPeriodStart,
	Instant currentPeriodEnd,
	Instant trialStart,
	Instant trialEnd,
	Boolean cancelAtPeriodEnd,
	Instant canceledAt,
	Instant startedAt,
	Instant endsAt,
	String productId,
	String discountId,
	List<Meter> meters,
	Map<String, Object> customFieldData
) {
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record Meter(
		Instant createdAt,
		Instant modifiedAt,
		String id,
		Long consumedUnits,
		Long creditedUnits,
		Long amount,
		String meterId
	) {
	}
}