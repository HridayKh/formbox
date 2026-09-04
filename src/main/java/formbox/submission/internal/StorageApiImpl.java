package formbox.submission.internal;

import formbox.billing.StorageApi;
import formbox.shared.CacheNames;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class StorageApiImpl implements StorageApi {

	private final SubmissionRepository submissionRepository;
	private final RedisCache redisCache;

	@Override
	@WithSpan
	public long getStorageBytesConsumed(UUID tenantId) {
		return redisCache.getOrCompute(CacheNames.METER_BALANCE, tenantId.toString(), Long.class, () -> submissionRepository.sumStorageBytesByTenantId(tenantId));
	}
}
