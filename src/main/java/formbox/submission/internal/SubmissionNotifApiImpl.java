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

		boolean autoResponse = true;

		Submission submission = submissionRepository.findByEmailAutoresponseRequestId(requestId);
		if (submission == null) {
			autoResponse = false;
			submission = submissionRepository.findByEmailNotifRequestId(requestId);
			if (submission == null)
				return;
		}

		EmailStatus status = autoResponse ? submission.getEmailAutoresponseEmailStatus() : submission.getEmailNotifStatus();

		EmailStatus effectiveStatus = (status != null && status.isAfter(newStatus)) ? status : newStatus;

		if (autoResponse)
			submission.setEmailAutoresponseEmailStatus(effectiveStatus);
		else
			submission.setEmailNotifStatus(effectiveStatus);
		submissionRepository.save(submission);
		submissionApi.updateFormSubmissionsCache(submission.getFormId(), submission.toSubmissionItem());
	}
}