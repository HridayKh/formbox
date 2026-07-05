package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.model.enums.SubscriptionState;
import in.hridaykh.formbox.repository.PurchasesRepository;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "tenants")
@Data
public class Tenant {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "current_period_end")
	private OffsetDateTime currentPeriodEnd;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public String resolveHighestActiveTier(PurchasesRepository purchasesRepository) {
		List<Purchases> activePurchases = purchasesRepository.findActiveOrGracePurchasesByUserId(this, SubscriptionState.active, SubscriptionState.cancelled_grace_period, OffsetDateTime.now());
		if (activePurchases.isEmpty())
			return "free-v1";
		PolarProducts product = activePurchases.getFirst().getProduct();
		return product.getSlug().toLowerCase();
	}
}