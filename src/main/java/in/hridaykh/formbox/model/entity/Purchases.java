package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.model.enums.SubscriptionState;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchases", schema = "public")
@Data
public class Purchases {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "updated_at", nullable = false, updatable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@Column(name = "polar_order_id", nullable = false)
	private String polarOrderId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "polar_product_id", referencedColumnName = "polar_product_id", nullable = false)
	private PolarProducts product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private Tenant userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false, columnDefinition = "tenant_subscription_state")
	private SubscriptionState status;

	@Column(name = "current_period_end")
	private OffsetDateTime currentPeriodEnd;

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

}