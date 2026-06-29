package in.hridaykh.formbox.model.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

enum SubscriptionState {
	free, active, cancelled_grace_period, past_due, unpaid
}

@Entity
@Table(name = "tenants")
public class Tenant {

	public Tenant() {
	}

	public Tenant(UUID id) {
		this.id = id;
	}

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "subscription_status", nullable = false)
	private SubscriptionState subscriptionStatus = SubscriptionState.free;

	@Column(name = "current_period_end")
	private OffsetDateTime currentPeriodEnd;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public String resolveCurrentTier() {
		return (subscriptionStatus == SubscriptionState.active || subscriptionStatus == SubscriptionState.cancelled_grace_period) ? "paid" : "free";
	}

	public void givePaidSubscription() {
		this.subscriptionStatus = SubscriptionState.active;
	}

	public void giveFreeSubscription() {
		this.subscriptionStatus = SubscriptionState.free;
	}
}