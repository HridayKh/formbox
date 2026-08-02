package formbox.form.internal;

import formbox.form.FormDto;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "forms")
@SQLDelete(sql = "UPDATE forms SET is_deleted = true, is_active = false WHERE id = ?")
@SQLRestriction("is_deleted IS false")
@Data
class Form {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "folder_id", nullable = false)
	private UUID folderId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "redirect_url")
	private String redirectUrl;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@Column(name = "turnstile_secret_key")
	private String turnstileSecretKey;

	@Column(name = "honeypot_name", nullable = false)
	private String honeypotName = "_gotcha";

	@Column(name = "rate_limit_rpm", nullable = false)
	private Integer rateLimitRpm = 20;

	@Column(name = "allow_files", nullable = false)
	private Boolean allowFiles = false;

	@Column(name = "allow_htmx", nullable = false)
	private Boolean allowHtmx = true;

	@Column(name = "allow_json", nullable = false)
	private Boolean allowJson = true;

	@JdbcTypeCode(SqlTypes.JSON_ARRAY)
	@Column(name = "field_validations", nullable = false, columnDefinition = "jsonb[]")
	private List<String> fieldValidations = new ArrayList<>();

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	FormDto toFormDto() {
		return new FormDto(
			this.id,
			this.tenantId,
			this.name,
			this.redirectUrl,
			this.isActive,
			this.turnstileSecretKey,
			this.honeypotName,
			this.rateLimitRpm,
			this.allowFiles,
			this.allowHtmx,
			this.allowJson,
			List.copyOf(this.fieldValidations),
			this.folderId
		);
	}

	void fromFormSettingsRequest(FormSettingsRequest request) {
		this.setId(request.getId());
		this.setName(request.getName());
		this.setRedirectUrl(request.getRedirectUrl());
		this.setIsActive(request.isActive());
		this.setTurnstileSecretKey(request.getTurnstileSecretKey());
		this.setHoneypotName(request.getHoneypotName());
		this.setRateLimitRpm(request.getRateLimitRpm());
		this.setAllowFiles(request.isAllowFiles());
		this.setAllowHtmx(request.isAllowHtmx());
		this.setAllowJson(request.isAllowJson());
		this.setFieldValidations(request.getFieldValidations());
	}
}