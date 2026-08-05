package formbox.submission.internal;

import formbox.notifs.EmailStatus;
import formbox.notifs.SubmissionNotifApi;
import formbox.submission.SubmissionApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionNotifApiImpl implements SubmissionNotifApi {
	private final SubmissionRepository submissionRepository;
	private final SubmissionApi submissionApi;

	@WithSpan
	@Override
	public void updateEmailStatus(String requestId, EmailStatus newStatus) {
		if (newStatus == null) return;

		Submission submission = submissionRepository.getSubmissionByEmailAutoresponseRequestId(requestId);
		if (submission == null) return;

		EmailStatus status = submission.getEmailAutoresponseEmailStatus();

		EmailStatus effectiveStatus = (status != null && status.isAfter(newStatus)) ? status : newStatus;

		submission.setEmailAutoresponseEmailStatus(effectiveStatus);
		submissionRepository.save(submission);
		submissionApi.updateFormSubmissionsCache(submission.getFormId(), submission.toSubmissionItem());
	}
}