package formbox.auth.tenant;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import formbox.shared.Entitlements;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "tenants")
@Data
@ToString
class Tenant {

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

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "verified_emails", columnDefinition = "jsonb")
	@JsonDeserialize(using = StringListJsonDeserializer.class)
	private List<String> verifiedEmails = new ArrayList<>();

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Entitlements getEntitlementsOrDefaults() {
		return entitlements != null ? entitlements : Entitlements.freeDefaults();
	}

}