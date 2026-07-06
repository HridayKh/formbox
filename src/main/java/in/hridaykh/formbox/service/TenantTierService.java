package in.hridaykh.formbox.service;

import in.hridaykh.formbox.model.entity.Purchases;
import in.hridaykh.formbox.model.enums.SubscriptionState;
import in.hridaykh.formbox.repository.PurchasesRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TenantTierService {

	private final PurchasesRepository purchasesRepository;

	public TenantTierService(PurchasesRepository purchasesRepository) {
		this.purchasesRepository = purchasesRepository;
	}

	@Cacheable(value = "tenantTiers", key = "#tenantId.toString()")
	public String resolveHighestActiveTierNonNull(UUID tenantId) {
		List<Purchases> activePurchases = purchasesRepository.findActiveOrGracePurchasesByUserId(tenantId, SubscriptionState.active, SubscriptionState.cancelled_grace_period, OffsetDateTime.now());
		if (activePurchases == null || activePurchases.isEmpty())
			return "free-v1";
		return activePurchases.getFirst().getProduct().getSlug().toLowerCase();
	}

	@Cacheable(value = "tenantTiers", key = "#tenantId.toString()")
	public String resolveHighestActiveTierNullable(UUID tenantId) {
		List<Purchases> activePurchases = purchasesRepository.findActiveOrGracePurchasesByUserId(tenantId, SubscriptionState.active, SubscriptionState.cancelled_grace_period, OffsetDateTime.now());
		if (activePurchases == null || activePurchases.isEmpty())
			return null;
		return activePurchases.getFirst().getProduct().getSlug().toLowerCase();
	}

	@Cacheable(value = "tenantTiers", key = "#tenantId")
	public String resolveHighestActiveTierNonNull(String tenantId) {
		return resolveHighestActiveTierNonNull(UUID.fromString(tenantId));
	}
}