package formbox.auth;
import formbox.shared.Entitlements;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.UUID;

public interface TenantApi {
	@WithSpan
	Entitlements getTenantEntitlementsOrDefault(UUID tenantId);

	@WithSpan
	void updateTenantEntitlementsInDb(UUID tenantId, Entitlements entitlements);

	@WithSpan
	void createTenant(UUID tenantId, String email);
}
