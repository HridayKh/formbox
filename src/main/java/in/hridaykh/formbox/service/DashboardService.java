package in.hridaykh.formbox.service;

import in.hridaykh.formbox.model.entity.PolarProducts;
import in.hridaykh.formbox.model.entity.Purchases;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.model.enums.SubscriptionState;
import in.hridaykh.formbox.repository.PolarProductsRepository;
import in.hridaykh.formbox.repository.PurchasesRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerResponse;
import sh.polar.sdk.models.subscription.PolarSubscriptionResponse;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DashboardService {

	private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

	private final TenantRepository tenantRepository;
	private final PolarProductsRepository polarProductsRepository;
	private final PurchasesRepository purchasesRepository;
	private final PolarHttpClient polarHttpClient;
	private final Polar polar;

	public DashboardService(TenantRepository tenantRepository, PolarProductsRepository polarProductsRepository, PurchasesRepository purchasesRepository, PolarHttpClient polarHttpClient, Polar polar) {
		this.tenantRepository = tenantRepository;
		this.polarProductsRepository = polarProductsRepository;
		this.purchasesRepository = purchasesRepository;
		this.polarHttpClient = polarHttpClient;
		this.polar = polar;
	}

	@Transactional
	public Tenant getOrCreateTenantWithFreeSubscription(JwtPayload userMetadata) {
		UUID userId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Tenant tenant = tenantRepository.findById(userId).orElseGet(() -> {
			Tenant newTenant = new Tenant();
			newTenant.setId(userId);
			newTenant.setEmail(userMetadata.getEmail());
			return tenantRepository.saveAndFlush(newTenant);
		});

		ensureFreeSubscriptionProvisioned(tenant);

		return tenant;
	}

	public String resolveHighestActiveTier(Tenant tenant) {
		return tenant.resolveHighestActiveTier(purchasesRepository);
	}

	private void ensureFreeSubscriptionProvisioned(Tenant tenant) {
		if (purchasesRepository.existsByUserId(tenant)) return;

		log.info("Provisioning Polar Free Subscription for: {}", tenant.getEmail());

		try {
			PolarProducts freeProduct = polarProductsRepository.findBySlug("free-v1")
				.orElseThrow(() -> new IllegalStateException("Database configuration missing for 'free-v1' slug product"));

			createCustomer(tenant.getId().toString(), tenant.getEmail());

			Map<String, Object> subscriptionRequest = new HashMap<>();
			subscriptionRequest.put("product_id", freeProduct.getPolarProductId());
			subscriptionRequest.put("external_customer_id", tenant.getId().toString());

			PolarSubscriptionResponse polarSubscription = polarHttpClient.post("/subscriptions/", subscriptionRequest, PolarSubscriptionResponse.class);

			Purchases freePurchase = new Purchases();
			freePurchase.setUserId(tenant);
			freePurchase.setProduct(freeProduct);
			freePurchase.setStatus(SubscriptionState.free);

			if (polarSubscription != null && polarSubscription.id() != null)
				freePurchase.setPolarOrderId(polarSubscription.id().toString());
			else
				freePurchase.setPolarOrderId("SUB_PROVISION_" + UUID.randomUUID().toString().substring(0, 8));

			if (polarSubscription != null && polarSubscription.currentPeriodEnd() != null)
				freePurchase.setCurrentPeriodEnd(polarSubscription.currentPeriodEnd());
			else
				freePurchase.setCurrentPeriodEnd(OffsetDateTime.now().plusMonths(1));

			purchasesRepository.save(freePurchase);
			log.info("Successfully provisioned Polar Free Subscription via backfill for: {}", tenant.getEmail());
		} catch (Exception e) {
			log.error("Failed to recover or assign dynamic Polar free subscription on active dashboard load: ", e);
		}
	}

	private void createCustomer(String userId, String email) {
		try {
			PolarCustomerResponse existingCustomer = polar.customers().getByExternalId(userId);
			if (existingCustomer != null && existingCustomer.id() != null) {
				return;
			}
		} catch (Exception e) {
			log.debug("Customer not found by external ID on Polar, proceeding to create one.");
		}

		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("external_id", userId);
		reqBody.put("email", email);

		Map<String, Object> billingAddress = new HashMap<>();
		billingAddress.put("country", "IN");
		reqBody.put("billing_address", billingAddress);

		polar.customers().create(reqBody);
	}
}