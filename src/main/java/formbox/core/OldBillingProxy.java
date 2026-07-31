package formbox.core;

import formbox.core.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OldBillingProxy {
	private final SubmissionRepository submissionRepository;

	public long submissionRepository_countByTenantIdAndCreatedAtAfter(@Param("tenantId") UUID tenantId, @Param("since") OffsetDateTime since) {
		return submissionRepository.countByTenantIdAndCreatedAtAfter(tenantId, since);
	}
}
