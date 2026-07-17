package in.hridaykh.formbox.billing.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ActiveMeters(
	UUID id,
	Instant createdAt,
	Instant modifiedAt,
	UUID meterId,
	Double consumedUnits,
	Double creditedUnits,
	Double balance
) {
}
