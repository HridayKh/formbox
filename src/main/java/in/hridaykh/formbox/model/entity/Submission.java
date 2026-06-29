package in.hridaykh.formbox.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "submissions", indexes = {@Index(name = "idx_submissions_form_id", columnList = "form_id")})
public class Submission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "form_id", nullable = false)
	private Form form;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "version_id")
	private FormVersion version;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", nullable = false)
	private Map<String, Object> payload;

	@Column(name = "sender_ip")
	private String senderIp;

	@Column(name = "is_spam")
	private Boolean isSpam = false;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	// Getters, Setters, Constructors
}