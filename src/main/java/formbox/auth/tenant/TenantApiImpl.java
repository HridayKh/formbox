package formbox.auth.tenant;

import formbox.auth.TenantApi;
import formbox.billing.Entitlements;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class TenantApiImpl implements TenantApi {
	private final TenantRepository tenantRepository;

	@WithSpan
	@Override
	public Entitlements getTenantEntitlementsOrDefault(UUID tenantId){
		Optional<Tenant> tenant = tenantRepository.findById(tenantId);
		if (tenant.isEmpty())
			return Entitlements.freeDefaults();
		return tenant.get().getEntitlementsOrDefaults();
	}

	@WithSpan
	@Override
	public void updateTenantEntitlements(UUID tenantId, Entitlements entitlements){
		var tenant = tenantRepository.findById(tenantId).orElse(null);
		if (tenant == null)
			return;
		tenant.setEntitlements(entitlements);
		tenantRepository.saveAndFlush(tenant);
	}

	@WithSpan
	@Override
	public void createTenant(UUID tenantId, String email){
		tenantRepository.findById(tenantId).orElseGet(() -> {
			log.info("Registering new tenant row for ID: {}", tenantId);
			Tenant newTenant = new Tenant();
			newTenant.setId(tenantId);
			newTenant.setEmail(email);
			newTenant.setEntitlements(Entitlements.freeDefaults());
			return tenantRepository.saveAndFlush(newTenant);
		});
	}
}
