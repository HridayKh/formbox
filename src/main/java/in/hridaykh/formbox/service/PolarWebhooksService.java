package in.hridaykh.formbox.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.hridaykh.formbox.config.PolarIdProperties;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.common.PolarListResponse;
import sh.polar.sdk.models.meter.PolarCustomerMeterResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PolarWebhooksService {

	private static final Logger log = LoggerFactory.getLogger(PolarWebhooksService.class);

	private final PolarHttpClient polarHttpClient;
	private final TenantRepository tenantRepository;
	private final ICacheService cacheService;
	private final PolarIdProperties polarIdProperties;

	private final ObjectMapper objectMapper;

	public PolarWebhooksService(PolarHttpClient polarHttpClient, TenantRepository tenantRepository, ICacheService cacheService, PolarIdProperties polarIdProperties) {
		this.polarHttpClient = polarHttpClient;
		this.tenantRepository = tenantRepository;
		this.cacheService = cacheService;
		this.polarIdProperties = polarIdProperties;
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	}

	public void processHook(String rawBody) {
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			String eventType = root.path("type").asText();
			JsonNode dataNode = root.path("data");

			log.info("Processing Polar Webhook event: {}", eventType);

			String customerEmail = dataNode.path("customer").path("email").asText();
			if (customerEmail.isEmpty() || "null".equals(customerEmail)) {
				customerEmail = dataNode.path("user").path("email").asText();
			}

			if (customerEmail.isEmpty()) {
				log.warn("Skipping webhook event {}: No customer email context found", eventType);
				return;
			}

			switch (eventType) {
				// Subscriptions lifecycle mutations
				case "subscription.created":
				case "subscription.active":
				case "subscription.uncanceled":
					handleSubscriptionActive(customerEmail, dataNode);
					break;

				case "subscription.canceled":
					handleSubscriptionCanceled(customerEmail, dataNode);
					break;

				case "subscription.past_due":
					handleSubscriptionPastDue(customerEmail);
					break;

				case "subscription.revoked":
				case "benefit_grant.revoked":
					handleSubscriptionEnded(customerEmail);
					break;

				// Fallback benefits logic
				case "benefit_grant.created":
				case "benefit_grant.updated":
					handleBenefitGrantFallback(customerEmail, dataNode);
					break;

				default:
					log.debug("Unmanaged event passed validation: {}", eventType);
					break;
			}

		} catch (Exception e) {
			log.error("Failed to process Polar webhook payload", e);
		}
	}

	private void handleSubscriptionActive(String email, JsonNode dataNode) {
		Tenant tenant = tenantRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalStateException("Tenant missing for: " + email));
		tenant.givePaidSubscription();
		String endPeriodStr = dataNode.path("current_period_end").asText();
		if (!endPeriodStr.isEmpty()) tenant.setCurrentPeriodEnd(OffsetDateTime.parse(endPeriodStr));
		tenantRepository.save(tenant);
		syncCache(tenant);
		log.info("Subscription marked ACTIVE for: {}", email);
	}

	private void handleSubscriptionCanceled(String email, JsonNode dataNode) {
		Tenant tenant = tenantRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalStateException("Tenant missing for: " + email));

		OffsetDateTime periodEnd = OffsetDateTime.now();
		String endPeriodStr = dataNode.path("current_period_end").asText();
		if (!endPeriodStr.isEmpty()) {
			periodEnd = OffsetDateTime.parse(endPeriodStr);
		}

		// Set status to grace period so they retain premium access
		tenant.setGracePeriodSubscription(periodEnd);
		tenantRepository.save(tenant);

		syncCache(tenant);
		log.info("Subscription marked CANCELLED_GRACE_PERIOD until {} for: {}", periodEnd, email);
	}

	private void handleSubscriptionPastDue(String email) {
		Tenant tenant = tenantRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalStateException("Tenant missing for: " + email));

		tenant.setPastDueSubscription();
		tenantRepository.save(tenant);

		syncCache(tenant);
		log.warn("Subscription marked PAST_DUE for: {}", email);
	}

	private void handleSubscriptionEnded(String email) {
		Tenant tenant = tenantRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalStateException("Tenant missing for: " + email));

		tenant.giveFreeSubscription();
		tenantRepository.save(tenant);

		syncCache(tenant);
		log.info("Subscription fully REVOKED/ENDED. Dropped to Free Tier for: {}", email);
	}

	private void handleBenefitGrantFallback(String email, JsonNode dataNode) {
		String status = dataNode.path("status").asText();
		if (status.isEmpty() && dataNode.has("is_granted")) {
			status = dataNode.path("is_granted").asBoolean() ? "granted" : "pending";
		}

		if (!"granted".equals(status)) return;

		Tenant tenant = tenantRepository.findByEmailIgnoreCase(email).orElse(null);
		if (tenant != null && "free".equals(tenant.resolveCurrentTier())) {
			tenant.givePaidSubscription();
			tenantRepository.save(tenant);
			syncCache(tenant);
		}
	}

	private void syncCache(Tenant tenant) {
		String externalUserId = tenant.getId().toString();
		String balanceKey = "user:" + externalUserId + ":meter_balance";
		cacheService.set("user:" + externalUserId + ":tier", tenant.resolveCurrentTier());
		try {
			String url = "/customer-meters/?external_customer_id=" + externalUserId;
			PolarListResponse<PolarCustomerMeterResponse> response = polarHttpClient.get(url, new TypeReference<>() {
			});

			Optional<PolarCustomerMeterResponse> matchedMeter = Optional.ofNullable(response)
				.map(PolarListResponse::items)
				.stream()
				.flatMap(List::stream)
				.filter(meter -> meter.meterId().toString().equalsIgnoreCase(polarIdProperties.getSubmissionMeterId()))
				.filter(meter -> meter.balance() != null)
				.findFirst();

			if (matchedMeter.isPresent()) {
				long longBalance = matchedMeter.get().balance().longValue();
				cacheService.set(balanceKey, String.valueOf(longBalance));
			} else {
				log.warn("Target meter not found or has a null balance for user: {}", externalUserId);
				cacheService.delete(balanceKey);
			}

		} catch (Exception e) {
			log.error("Failed to query Polar CustomerMeters API for user: {}", externalUserId, e);
		}
	}
}