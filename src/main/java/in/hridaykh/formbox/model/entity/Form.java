package in.hridaykh.formbox.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "forms")
public class Form {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = false)
	private Tenant tenant;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "redirect_url")
	private String redirectUrl;

	@Column(name = "turnstile_secret_key")
	private String turnstileSecretKey;

	@Column(name = "is_active")
	private Boolean isActive = true;

	// Circular reference breaker: Map to the ID or use a dedicated OneToOne mapping
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "current_version_id", referencedColumnName = "id")
	private FormVersion currentVersion;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	// Getters, Setters, Constructors
}