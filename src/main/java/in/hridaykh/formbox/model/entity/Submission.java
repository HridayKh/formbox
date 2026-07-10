package in.hridaykh.formbox.model.entity;

import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.dto.SubmissionItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "submissions", indexes = {@Index(name = "idx_submissions_form_id", columnList = "form_id")})
@Getter
@NoArgsConstructor
public class Submission {

	public Submission(Form form, Map<String, String> payload, String senderIp, Boolean isSpam) {
		this.form = form;
		this.payload = payload;
		this.senderIp = senderIp;
		this.isSpam = isSpam;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "form_id", nullable = false)
	private Form form;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", nullable = false)
	private Map<String, String> payload;

	@Column(name = "sender_ip")
	private String senderIp;

	@Column(name = "is_spam")
	private Boolean isSpam = false;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	public SubmissionItem toSubmissionItem() {
		return new SubmissionItem(this.id, this.payload, this.senderIp, this.createdAt, this.isSpam);
	}

}