package formbox.submission;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.UUID;

public interface SubmissionApi {
	@WithSpan
	FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId);

	@WithSpan
	void updateFormSubmissionsCache(UUID formId, SubmissionItem newSubmission);
}
