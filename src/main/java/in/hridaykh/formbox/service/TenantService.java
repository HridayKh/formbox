package in.hridaykh.formbox.service;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sh.polar.sdk.Polar;
import sh.polar.sdk.models.customer.PolarCustomerResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

	private final TenantRepository tenantRepository;
	private final Polar polar;

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

	public void ensurePolarCustomerExists(String userId, String email) {
		log.trace("Verifying remote customer identity mirror layer presence with tracking external user index parameter: {}", userId);
		try {
			PolarCustomerResponse existingCustomer = polar.customers().getByExternalId(userId);
			if (existingCustomer != null && existingCustomer.id() != null) {
				log.trace("Polar customer mirror validation verified. Matching customer layout context exists for unique reference token: {}", existingCustomer.id());
				return;
			}
		} catch (Exception e) {
			log.debug("Customer matching constraint lookup by external ID failed on Polar system platform. Proceeding to instantiate automated record initialization.");
		}

		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("external_id", userId);
		reqBody.put("email", email);

		Map<String, Object> billingAddress = new HashMap<>();
		billingAddress.put("country", "IN");
		reqBody.put("billing_address", billingAddress);

		try {
			var createdCustomer = polar.customers().create(reqBody);
			log.info("Successfully created Polar customer with ID: {}", createdCustomer.id());
		} catch (Exception e) {
			log.error("Failed to push customer metadata mapping pipeline configuration record down to remote Polar payment engine architectures for target ID: {}", userId, e);
			throw e;
		}
	}
}