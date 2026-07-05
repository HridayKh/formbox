package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.model.enums.BillingInterval;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "polar_products", schema = "public")
@Data
public class PolarProducts {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "polar_product_id", nullable = false, unique = true)
	private String polarProductId;

	@Column(name = "name", nullable = false, columnDefinition = "text")
	private String name;

	@Column(name = "price_cents", nullable = false)
	private Long priceCents = 0L;

	@Enumerated(EnumType.STRING)
	@Column(name = "billing_type", nullable = false, columnDefinition = "billing_interval_type")
	private BillingInterval billingType;

	@Column(name = "billing_interval")
	private Integer billingInterval;

	@Column(name = "slug")
	private String slug;
}