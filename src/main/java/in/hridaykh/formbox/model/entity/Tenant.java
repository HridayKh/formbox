package in.hridaykh.formbox.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	public Tenant(UUID id, String email) {
		this.id = id;
		this.email = email;
	}

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
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

	// Returns 'paid' if they are active OR in their grace period window
	public String resolveCurrentTier() {
		return (subscriptionStatus == SubscriptionState.active || subscriptionStatus == SubscriptionState.cancelled_grace_period) ? "paid" : "free";
	}

	public void givePaidSubscription() {
		this.subscriptionStatus = SubscriptionState.active;
	}

	public void giveFreeSubscription() {
		this.subscriptionStatus = SubscriptionState.free;
		this.currentPeriodEnd = null;
	}

	public void setGracePeriodSubscription(OffsetDateTime periodEnd) {
		this.subscriptionStatus = SubscriptionState.cancelled_grace_period;
		this.currentPeriodEnd = periodEnd;
	}

	public void setPastDueSubscription() {
		this.subscriptionStatus = SubscriptionState.past_due;
	}

	public SubscriptionState getSubscriptionStatusEnum() {
		return this.subscriptionStatus;
	}

	public String getSubscriptionStatus() {
		return this.subscriptionStatus.toString();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public OffsetDateTime getCurrentPeriodEnd() {
		return currentPeriodEnd;
	}

	public void setCurrentPeriodEnd(OffsetDateTime currentPeriodEnd) {
		this.currentPeriodEnd = currentPeriodEnd;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}