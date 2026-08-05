package formbox.submission;

import formbox.notifs.EmailStatus;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SubmissionItem(
	UUID id,
	Map<String, String> payload,
	OffsetDateTime createdAt,
	boolean isSpam,
	EmailStatus emailAutoresponseEmailStatus,
	EmailStatus emailNotifStatus
) implements Serializable {
}