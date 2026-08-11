package formbox.auth.tenant;

import formbox.auth.TenantApi;
import formbox.shared.Entitlements;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class TenantService implements TenantApi {
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
	public void updateTenantEntitlementsInDb(UUID tenantId, Entitlements entitlements){
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

	@WithSpan
	@Override
	public List<String> getVerifiedEmails(UUID tenantId) {
		return tenantRepository.findById(tenantId)
			.map(Tenant::getVerifiedEmails)
			.orElse(List.of());
	}

	@WithSpan
	@Override
	public void addVerifiedEmail(UUID tenantId, String email) {
		var tenant = tenantRepository.findById(tenantId).orElse(null);
		if (tenant == null) return;
		List<String> emails = new ArrayList<>(tenant.getVerifiedEmails() != null ? tenant.getVerifiedEmails() : List.of());
		if (!emails.contains(email)) {
			emails.add(email);
			tenant.setVerifiedEmails(emails);
			tenantRepository.saveAndFlush(tenant);
		}
	}

	@WithSpan
	@Override
	public void removeVerifiedEmail(UUID tenantId, String email) {
		var tenant = tenantRepository.findById(tenantId).orElse(null);
		if (tenant == null) return;
		List<String> emails = new ArrayList<>(tenant.getVerifiedEmails() != null ? tenant.getVerifiedEmails() : List.of());
		emails.remove(email);
		tenant.setVerifiedEmails(emails);
		tenantRepository.saveAndFlush(tenant);
	}

	@WithSpan
	@Override
	public List<UUID> getAllTenantIds() {
		return tenantRepository.findAll().stream().map(Tenant::getId).toList();
	}
}
