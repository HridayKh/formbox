package formbox.billing;

import java.time.OffsetDateTime;
import java.util.UUID;

@FunctionalInterface
public interface DbSubmissionCounter {
	long countSubmissionsAfter(UUID tenantId, OffsetDateTime after);
}