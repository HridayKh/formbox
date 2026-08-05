package formbox.submission.internal;

import formbox.notifs.EmailStatus;
import formbox.submission.SubmissionItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "submissions", indexes = {@Index(name = "idx_submissions_form_id", columnList = "form_id")})
@Getter
@NoArgsConstructor
class Submission {

	public Submission(UUID formId, UUID tenantId, Map<String, String> payload, String senderIp, Boolean isSpam) {
		this.formId = formId;
		this.tenantId = tenantId;
		this.payload = payload;
		this.senderIp = senderIp;
		this.isSpam = isSpam;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "form_id", nullable = false)
	private UUID formId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Setter
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", nullable = false)
	private Map<String, String> payload;

	@Column(name = "sender_ip")
	private String senderIp;

	@Column(name = "is_spam")
	private Boolean isSpam = false;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private final OffsetDateTime createdAt = OffsetDateTime.now();

	@Setter
	@Column(name = "email_autoresponse_request_id")
	private String emailAutoresponseRequestId;

	@Setter
	@JdbcTypeCode(SqlTypes.ENUM)
	@Enumerated(EnumType.STRING)
	@Column(name = "email_autoresponse_email_status")
	private EmailStatus emailAutoresponseEmailStatus;

	public SubmissionItem toSubmissionItem() {
		return new SubmissionItem(this.id, this.payload, this.senderIp, this.createdAt, this.isSpam);
	}

}