package in.hridaykh.formbox.model.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SubmissionItem(
	UUID id,
	Map<String, String> payload,
	String senderIp,
	OffsetDateTime createdAt
) {}