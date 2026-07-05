package in.hridaykh.formbox.service;

import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.dto.SubmissionItem;
import in.hridaykh.formbox.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

	private final SubmissionRepository submissionRepository;

	public SubmissionService(SubmissionRepository submissionRepository) {
		this.submissionRepository = submissionRepository;
	}

	@Transactional(readOnly = true)
	public FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId) {
		List<SubmissionItem> cleanSubmissions = submissionRepository.findSubmissionsByFormIdAndIsSpam(formId, false);

		List<SubmissionItem> spamSubmissions = submissionRepository.findSubmissionsByFormIdAndIsSpam(formId, true);

		return new FormSubmissionsResponse(cleanSubmissions, spamSubmissions);
	}
}
