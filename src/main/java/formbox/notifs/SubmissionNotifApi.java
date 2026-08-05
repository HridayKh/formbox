package formbox.notifs;

import io.opentelemetry.instrumentation.annotations.WithSpan;

public interface SubmissionNotifApi {

	@WithSpan
	void updateEmailStatus(String requestId, EmailStatus status);
}
