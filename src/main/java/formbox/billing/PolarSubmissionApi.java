package formbox.billing;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.BaggageHeader;
import io.sentry.SentryTraceHeader;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface PolarSubmissionApi {

//	@Async
	@WithSpan
	void decrementSubmissionBalance(UUID tenantId);

	@WithSpan
	long getCachedSubmissionBalance(UUID tenantId);
}
