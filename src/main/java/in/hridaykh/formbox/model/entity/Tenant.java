package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.billing.model.Entitlements;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "tenants")
@Data
@ToString
public class Tenant {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "entitlements", columnDefinition = "jsonb")
	private Entitlements entitlements;

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	/**
	 * Returns entitlements with free tier fallback.
	 * Safe to call even when entitlements is null (pre-webhook tenants).
	 */
	public Entitlements getEntitlementsOrDefaults() {
		return entitlements != null ? entitlements : Entitlements.freeDefaults();
	}

}