package in.hridaykh.formbox.service;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

	private final TenantRepository tenantRepository;
	private final PolarHttpClient polarHttpClient;
	private final Polar polar;
	private final ObjectMapper objectMapper;

	@Transactional
	public void getOrCreateTenantWithFreeSubscription(JwtPayload userMetadata) {
		UUID userId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		log.debug("Initiating onboarding after auth callback for: {}", userId);
		Tenant tenant = tenantRepository.findById(userId).orElseGet(() -> {
			log.info("Tenant workspace record missing from local database storage. Registering new tenant row for ID: {}", userId);
			Tenant newTenant = new Tenant();
			newTenant.setId(userId);
			newTenant.setEmail(userMetadata.getEmail());
			newTenant.setEntitlements(Entitlements.freeDefaults());
			return tenantRepository.saveAndFlush(newTenant);
		});

		ensureFreeSubscriptionProvisioned(tenant);
	}

	private void ensureFreeSubscriptionProvisioned(Tenant tenant) {
		log.info("Provisioning baseline Polar Free Subscription context for account email: {}", tenant.getEmail());

		try {
			createCustomer(tenant.getId().toString(), tenant.getEmail());

			// Retrieve products from Polar to locate the "Free Plan"
			String productsJson = polarHttpClient.get("/products/", String.class);
			JsonNode productsNode = objectMapper.readTree(productsJson);
			String freeProductId = null;
			for (JsonNode item : productsNode.path("items")) {
				if ("Free Plan".equalsIgnoreCase(item.path("name").asString())) {
					freeProductId = item.path("id").asString();
					break;
				}
			}

			if (freeProductId == null) {
				log.error("Baseline Free Plan product not found on Polar Sandbox platform.");
				throw new IllegalStateException("Free Plan product not configured on Polar");
			}

			Map<String, Object> subscriptionRequest = new HashMap<>();
			subscriptionRequest.put("product_id", freeProductId);
			subscriptionRequest.put("external_customer_id", tenant.getId().toString());

			log.debug("Dispatching request downstream to Polar infrastructure API to create free baseline subscription mapping.");
			polarHttpClient.post("/subscriptions/", subscriptionRequest, Void.class);

			log.info("Successfully provisioned Polar Free Subscription for: {}", tenant.getEmail());
		} catch (Exception e) {
			log.error("Failed to recover or assign dynamic Polar free subscription on active dashboard load for email: {}", tenant.getEmail(), e);
		}
	}

	private void createCustomer(String userId, String email) {
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
			log.info("Successfully established remote customer identifier reference tracking mirror within Polar cloud infrastructure framework layout. Internal target mapping token: {}", createdCustomer.id());
		} catch (Exception e) {
			log.error("Failed to push customer metadata mapping pipeline configuration record down to remote Polar payment engine architectures for target ID: {}", userId, e);
			throw e;
		}
	}
}