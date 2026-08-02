package formbox.billing;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.UUID;

public interface EntitlementsApi {
	@WithSpan
	Entitlements getEntitlements(UUID tenantId);

	@WithSpan
	void updateEntitlementsCache(UUID tenantId, Entitlements entitlements);
}
