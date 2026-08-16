package formbox.submission.internal;

import formbox.billing.StorageApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class StorageApiImpl implements StorageApi {

	private final SubmissionRepository submissionRepository;

	@Override
	@WithSpan
	public long getStorageBytesConsumed(UUID tenantId) {
		return submissionRepository.sumStorageBytesByTenantId(tenantId);
	}
}
