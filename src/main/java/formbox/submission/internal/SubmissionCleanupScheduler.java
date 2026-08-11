package formbox.submission.internal;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.notifs.UploadService;
import formbox.shared.Entitlements;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class SubmissionCleanupScheduler {

	private final TenantApi tenantApi;
	private final EntitlementsApi entitlementsApi;
	private final SubmissionRepository submissionRepository;
	private final UploadService uploadService;

	/**
	 * Scheduled task running every 24 hours to clean up expired submissions and associated S3 attachment files
	 * according to each tenant's retentionDays entitlement (Free: 7d, Starter: 30d, Pro: 90d).
	 */
	@Scheduled(cron = "0 0 */24 * * *")
	@WithSpan
	@Transactional
	public void cleanupExpiredSubmissions() {
		log.info("Starting scheduled submission retention cleanup task...");
		List<UUID> tenantIds = tenantApi.getAllTenantIds();

		int deletedSubmissionsCount = 0;
		int deletedFilesCount = 0;

		for (UUID tenantId : tenantIds) {
			try {
				Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
				int retentionDays = entitlements != null ? entitlements.retentionDaysOrDefault() : 7;

				OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
				List<Submission> expiredSubmissions = submissionRepository.findAllByTenantIdAndCreatedAtBefore(tenantId, cutoff);

				if (expiredSubmissions.isEmpty()) {
					continue;
				}

				for (Submission submission : expiredSubmissions) {
					Map<String, String> payload = submission.getPayload();
					if (payload != null && !payload.isEmpty()) {
						for (Map.Entry<String, String> entry : payload.entrySet()) {
							if (entry.getKey() != null && entry.getKey().endsWith("__url")) {
								String fileUrl = entry.getValue();
								if (fileUrl != null && !fileUrl.isBlank()) {
									uploadService.deleteFileByUrl(fileUrl.strip());
									deletedFilesCount++;
								}
							}
						}
					}
					submissionRepository.delete(submission);
					deletedSubmissionsCount++;
				}

			} catch (Exception e) {
				log.error("Error cleaning up expired submissions for tenant ID: {}", tenantId, e);
			}
		}

		log.info("Completed submission retention cleanup: deleted {} expired submissions and {} S3 attachments.",
			deletedSubmissionsCount, deletedFilesCount);
	}
}
