package in.hridaykh.formbox.model.dto;

import java.io.Serializable;
import java.util.List;

public record FormSubmissionsResponse(
	List<SubmissionItem> submissions,
	List<SubmissionItem> spam
) implements Serializable {
}