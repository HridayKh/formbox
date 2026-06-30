package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.model.json.AllowedField;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "form_versions", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"form_id", "version_number"})
})
public class FormVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "form_id", nullable = false)
	private Form form;

	@Generated
	@Column(name = "version_number", insertable = false, updatable = false, nullable = false)
	private Integer versionNumber;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "allowed_fields")
	private AllowedField[] allowedFields;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

}