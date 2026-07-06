package in.hridaykh.formbox.service.polar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.hridaykh.formbox.model.entity.PolarProducts;
import in.hridaykh.formbox.model.entity.Purchases;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.model.enums.SubscriptionState;
import in.hridaykh.formbox.repository.PolarProductsRepository;
import in.hridaykh.formbox.repository.PurchasesRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PolarWebhooksService {

	private static final Logger log = LoggerFactory.getLogger(PolarWebhooksService.class);

	private final TenantRepository tenantRepository;
	private final PolarProductsRepository polarProductsRepository;
	private final PurchasesRepository purchasesRepository;
	private final ObjectMapper objectMapper;
	private final PolarMeterService polarMeterService;

	public PolarWebhooksService(TenantRepository tenantRepository, PolarProductsRepository polarProductsRepository, PurchasesRepository purchasesRepository, PolarMeterService polarMeterService) {
		this.tenantRepository = tenantRepository;
		this.polarProductsRepository = polarProductsRepository;
		this.purchasesRepository = purchasesRepository;
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		this.polarMeterService = polarMeterService;
	}

	@Transactional
	public void processHook(String rawBody) {
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			String eventType = root.path("type").asText();
			JsonNode dataNode = root.path("data");

			log.info("Processing Polar Webhook event: {}", eventType);

			String customerEmail = dataNode.path("customer").path("email").asText();
			if (customerEmail.isBlank() || "null".equals(customerEmail)) {
				customerEmail = dataNode.path("user").path("email").asText();
			}

			if (customerEmail.isBlank()) {
				log.warn("Skipping webhook event {}: No customer email context found", eventType);
				return;
			}

			PolarProducts product = resolveProductFromDataNode(dataNode);
			String finalCustomerEmail = customerEmail;
			Tenant tenant = tenantRepository.findByEmailIgnoreCase(customerEmail).orElseThrow(() -> new IllegalStateException("Tenant missing for: " + finalCustomerEmail));

			switch (eventType) {
				case "benefit_grant.cycled":
				case "benefit_grant.updated":
				case "order.created":
				case "subscription.created":
				case "subscription.uncanceled":
					SubscriptionState activeState = (product != null && "free-v1".equals(product.getSlug())) ? SubscriptionState.free : SubscriptionState.active;
					handleDynamicSubscription(tenant, product, dataNode, activeState);
					break;

				case "subscription.canceled":
					if (product != null && "free-v1".equals(product.getSlug())) {
						log.info("Free plan cancellation webhook intercepted for {}. Keeping plan active/free.", customerEmail);
						handleDynamicSubscription(tenant, product, dataNode, SubscriptionState.free);
					} else {
						handleDynamicSubscription(tenant, product, dataNode, SubscriptionState.cancelled_grace_period);
					}
					break;

				case "subscription.past_due":
					handleDynamicSubscription(tenant, product, dataNode, SubscriptionState.past_due);
					break;

				case "benefit_grant.revoked":
				case "subscription.revoked":
					handleDynamicSubscription(tenant, product, dataNode, SubscriptionState.unpaid);
					break;

				default:
					log.warn("Unmanaged event passed validation: {}", eventType);
					break;
			}

		} catch (Exception e) {
			log.error("Failed to process Polar webhook payload", e);
		}
	}

	private void removeLowerTiersOnUpgrade(Tenant tenant, PolarProducts newProduct) {
		if ("pro-v1".equals(newProduct.getSlug())) {
			PolarProducts free = polarProductsRepository.findBySlug("free-v1").orElseThrow();
			PolarProducts starter = polarProductsRepository.findBySlug("starter-v1").orElseThrow();
			purchasesRepository.deleteByUserIdAndProduct(tenant, free);
			purchasesRepository.deleteByUserIdAndProduct(tenant, starter);
			log.info("Purged lower tiers (free and starter) for upgraded Pro user: {}", tenant.getEmail());
		} else if ("start-v1".equals(newProduct.getSlug())) {
			PolarProducts free = polarProductsRepository.findBySlug("free-v1").orElseThrow();
			purchasesRepository.deleteByUserIdAndProduct(tenant, free);
			log.info("Purged free tier for upgraded Starter user: {}", tenant.getEmail());
		}
	}

	private PolarProducts resolveProductFromDataNode(JsonNode dataNode) {
		String polarProductId = dataNode.path("product_id").asText();
		if (polarProductId.isBlank()) {
			polarProductId = dataNode.path("product").path("id").asText();
		}
		if (polarProductId.isBlank()) return null;

		return polarProductsRepository.findByPolarProductId(polarProductId).orElse(null);
	}

	@CacheEvict(value = "tenantTiers", key = "#tenant.id")
	public void handleDynamicSubscription(Tenant tenant, PolarProducts product, JsonNode dataNode, SubscriptionState newState) {
		if (product == null) {
			log.error("Webhook payload matching failed due to missing product context for tenant: {}", tenant.getEmail());
			return;
		}

		if (newState == SubscriptionState.active || newState == SubscriptionState.free) {
			removeLowerTiersOnUpgrade(tenant, product);
		}

		// Look up purchase database mapping or open new transaction row
		Purchases purchase = purchasesRepository.findByUserIdAndProduct(tenant, product).orElse(new Purchases());
		purchase.setUserId(tenant);
		purchase.setProduct(product);
		purchase.setStatus(newState);

		// Fulfill the DB constraint field if missing
		if (purchase.getPolarOrderId() == null) {
			String subId = dataNode.path("id").asText();
			purchase.setPolarOrderId(!subId.isBlank() ? subId : "WEBHOOK_PROVISION_" + tenant.getId());
		}

		String endPeriodStr = dataNode.path("current_period_end").asText();
		if (!endPeriodStr.isBlank() && !"null".equals(endPeriodStr)) {
			OffsetDateTime periodEnd = OffsetDateTime.parse(endPeriodStr);
			purchase.setCurrentPeriodEnd(periodEnd);

			tenant.setCurrentPeriodEnd(periodEnd);
			tenantRepository.save(tenant);
		}

		purchasesRepository.save(purchase);
		log.info("Successfully updated purchase state to {} for tenant: {} on product: {}", newState, tenant.getEmail(), product.getName());

		polarMeterService.getRemainingSubmissionsBalance(tenant);
	}
}