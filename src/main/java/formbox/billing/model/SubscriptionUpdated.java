package formbox.billing.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.Map;

/**
 * Deserializes the {@code data} node of a Polar {@code subscription.updated} webhook event.
 * <p>
 * Contains the full subscription state plus embedded customer and product objects.
 * Product metadata carries all plan configuration (tier, limits, feature flags).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SubscriptionUpdated(
	String id,
	String status,
	Long amount,
	String currency,
	String recurringInterval,
	Instant currentPeriodStart,
	Instant currentPeriodEnd,
	Boolean cancelAtPeriodEnd,
	Instant canceledAt,
	Instant startedAt,
	Instant endsAt,
	String customerId,
	String productId,
	String priceId,
	Map<String, Object> metadata,
	Customer customer,
	Product product
) {

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record Customer(
		String id,
		String email,
		String externalId,
		String name,
		Map<String, Object> metadata
	) {}

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record Product(
		String id,
		String name,
		Map<String, String> metadata
	) {}
}
