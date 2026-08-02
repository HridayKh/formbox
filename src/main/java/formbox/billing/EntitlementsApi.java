package formbox.billing;

import formbox.shared.Entitlements;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.UUID;

public interface EntitlementsApi {
	@WithSpan
	Entitlements getEntitlements(UUID tenantId);

	@WithSpan
	Entitlements updateEntitlementsCache(UUID tenantId, Entitlements entitlements);
}
