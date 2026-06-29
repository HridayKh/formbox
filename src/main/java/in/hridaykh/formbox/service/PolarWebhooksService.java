package in.hridaykh.formbox.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.common.PolarListResponse;
import sh.polar.sdk.models.meter.PolarCustomerMeterResponse;

import java.util.UUID;

@Service
public class PolarWebhooksService {

	private static final Logger log = LoggerFactory.getLogger(PolarWebhooksService.class);
	private static final long FREE_TIER_LIMIT = 100L; // Fallback constant for local free tier

	private final PolarHttpClient polarHttpClient;
	private final TenantRepository tenantRepository;
	private final ICacheService cacheService;
	private final ObjectMapper objectMapper;

	public PolarWebhooksService(PolarHttpClient polarHttpClient, TenantRepository tenantRepository, ICacheService cacheService) {
		this.polarHttpClient = polarHttpClient;
		this.tenantRepository = tenantRepository;
		this.cacheService = cacheService;
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	}

	public void processHook(String rawBody) {
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			String eventType = root.path("event").asText();
			JsonNode dataNode = root.path("data");

			log.info("Processing Polar Webhook event: {}", eventType);

			switch (eventType) {
				case "benefit_grant.created":
				case "benefit_grant.updated":
					handleBenefitGrant(dataNode);
					break;

				case "benefit_grant.revoked":
					handleBenefitRevoked(dataNode);
					break;

				case "order.created":
					handleOrderCreated(dataNode);
					break;

				default:
					log.debug("Unmanaged webhook event received: {}", eventType);
					break;
			}

		} catch (Exception e) {
			log.error("Failed to process Polar webhook payload", e);
		}
	}

	private void handleBenefitGrant(JsonNode dataNode) {
		String status = dataNode.path("status").asText();
		String externalUserId = dataNode.path("customer").path("external_id").asText();

		if (externalUserId.isEmpty() || !"granted".equals(status)) return;

		UUID tenantId = UUID.fromString(externalUserId);

		Tenant tenant = tenantRepository.findById(tenantId).orElseGet(() -> new Tenant(tenantId));

		tenant.givePaidSubscription();
		tenantRepository.save(tenant);

		cacheService.set("user:" + externalUserId + ":tier", tenant.resolveCurrentTier());

		syncUserMeterWithPolar(externalUserId);
	}

	private void handleBenefitRevoked(JsonNode dataNode) {
		JsonNode customer = dataNode.path("customer");
		String externalUserId = customer.path("external_id").asText();

		if (externalUserId.isEmpty()) return;

		log.info("Downgrading user {} to free tier.", externalUserId);

		UUID tenantId = UUID.fromString(externalUserId);
		Tenant tenant = tenantRepository.findById(tenantId).orElseGet(() -> new Tenant(tenantId));

		tenant.giveFreeSubscription();
		tenantRepository.save(tenant);

		cacheService.set("user:" + externalUserId + ":tier", tenant.resolveCurrentTier());
		cacheService.set("user:" + externalUserId + ":meter_balance", String.valueOf(FREE_TIER_LIMIT));
	}

	private void handleOrderCreated(JsonNode dataNode) {
		String billingReason = dataNode.path("billing_reason").asText();
		JsonNode customer = dataNode.path("customer");
		String externalUserId = customer.path("external_id").asText();

		if (externalUserId.isEmpty()) return;

		if ("subscription_cycle".equals(billingReason)) {
			log.info("Subscription renewal cycle hit for user {}. Refreshing meter balance.", externalUserId);
			syncUserMeterWithPolar(externalUserId);
		}
	}

	public void syncUserMeterWithPolar(String externalUserId) {
		try {
			String url = "/customer-meters/?external_customer_id=" + externalUserId;
			PolarListResponse<PolarCustomerMeterResponse> response = polarHttpClient.get(url, new TypeReference<>() {
			});

			if (response != null && response.items() != null && !response.items().isEmpty()) {
				PolarCustomerMeterResponse matchedMeter = response.items().getFirst();

				Double actualPolarBalance = matchedMeter.balance();
				log.info("Found matching user {} inside Polar. Balance: {}", externalUserId, actualPolarBalance);

				// Syncing usage value straight to the fast-lookup layer
				cacheService.set("user:" + externalUserId + ":meter_balance", String.valueOf(actualPolarBalance.longValue()));
			} else {
				log.warn("No active meter records found in Polar for external user ID: {}", externalUserId);
			}

		} catch (Exception e) {
			log.error("Failed to query Polar CustomerMeters API for user: {}", externalUserId, e);
		}
	}
}