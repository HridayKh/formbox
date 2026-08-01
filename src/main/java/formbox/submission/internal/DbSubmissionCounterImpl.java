package formbox.submission.internal;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class DbSubmissionCounterImpl implements formbox.billing.DbSubmissionCounter {

	private final SubmissionRepository submissionRepository;

	@Override
	@WithSpan
	public long countSubmissionsAfter(UUID tenantId, OffsetDateTime after) {
		return submissionRepository.countByTenantIdAndCreatedAtAfter(tenantId, after);
	}
}