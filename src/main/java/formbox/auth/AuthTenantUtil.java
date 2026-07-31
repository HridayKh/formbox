package formbox.auth;

import formbox.billing.model.Entitlements;
import formbox.shared.Tenant;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthTenantUtil {

	private final TenantRepository tenantRepository;

	@Transactional
	@WithSpan
	public void getOrCreateTenantWithFreeSubscription(JwtPayload userMetadata) {
		UUID userId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		log.debug("Initiating onboarding after auth callback for: {}", userId);
		tenantRepository.findById(userId).orElseGet(() -> {
			log.info("Tenant workspace record missing from local database storage. Registering new tenant row for ID: {}", userId);
			Tenant newTenant = new Tenant();
			newTenant.setId(userId);
			newTenant.setEmail(userMetadata.getEmail());
			newTenant.setEntitlements(Entitlements.freeDefaults());
			return tenantRepository.saveAndFlush(newTenant);
		});
	}
}