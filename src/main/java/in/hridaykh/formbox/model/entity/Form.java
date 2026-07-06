package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.model.dto.CachedForm;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "forms")
@SQLDelete(sql = "UPDATE forms SET is_deleted = true, is_active = false WHERE id = ?")
@SQLRestriction("is_deleted IS false")
@Data
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

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	public CachedForm toCachedFormDto() {
		return new CachedForm(this.id, this.tenant != null ? this.tenant.getId() : null, this.name, this.redirectUrl, this.turnstileSecretKey, this.isActive);
	}
}