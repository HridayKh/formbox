package in.hridaykh.formbox.billing.model;

import java.time.Instant;
import java.util.UUID;

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
