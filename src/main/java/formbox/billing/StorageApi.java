package formbox.billing;

import java.util.UUID;

/**
 * Reads total storage consumption for a tenant from the DB.
 * Storage is tracked per-submission via the {@code storage_bytes} column
 * and summed on demand.
 */
@FunctionalInterface
public interface StorageApi {
	/**
	 * Returns the total storage bytes consumed across all submissions for this tenant.
	 */
	long getStorageBytesConsumed(UUID tenantId);
}
