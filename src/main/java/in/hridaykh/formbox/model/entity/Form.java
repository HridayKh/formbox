package in.hridaykh.formbox.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "forms")
@SQLDelete(sql = "UPDATE forms SET is_deleted = true, is_active = false WHERE id = ?")
@SQLRestriction("is_deleted IS false")
public class Form {
	public Form() {
	}

	public Form(Tenant tenant, String name, String redirectUrl) {
		this.tenant = tenant;
		this.name = name;
		this.redirectUrl = redirectUrl;
	}

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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "current_version_id", referencedColumnName = "id")
	private FormVersion currentVersion;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	public boolean compareTenant(Tenant tenant) {
		return this.tenant.getId().equals(tenant.getId());
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Tenant getTenant() {
		return tenant;
	}

	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public String getTurnstileSecretKey() throws IllegalAccessException {
		throw new IllegalAccessException("TURNSTILE NOW ALLOWED BRUH");
	}

	public void setTurnstileSecretKey(String turnstileSecretKey) throws IllegalAccessException {
		throw new IllegalAccessException("TURNSTILE NOW ALLOWED BRUH");
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public FormVersion getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(FormVersion currentVersion) {
		this.currentVersion = currentVersion;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Boolean getDeleted() {
		return isDeleted;
	}

	public void setDeleted(Boolean deleted) {
		isDeleted = deleted;
	}
}