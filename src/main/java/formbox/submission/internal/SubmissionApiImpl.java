package formbox.submission.internal;

import formbox.shared.CacheNames;
import formbox.shared.RedisCache;
import formbox.submission.FormSubmissionsResponse;
import formbox.submission.SubmissionApi;
import formbox.submission.SubmissionItem;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import formbox.notifs.UploadService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionApiImpl implements SubmissionApi {
	private final SubmissionRepository submissionRepository;
	private final RedisCache redisCache;
	private final UploadService uploadService;

	private SubmissionItem attachPresignedUrls(SubmissionItem item) {
		if (item == null || item.payload() == null || item.payload().isEmpty()) return item;
		java.util.Map<String, String> updatedPayload = new java.util.HashMap<>(item.payload());
		boolean modified = false;
		for (java.util.Map.Entry<String, String> entry : item.payload().entrySet()) {
			if (entry.getKey() != null && entry.getKey().endsWith("__url") && entry.getValue() != null && !entry.getValue().isBlank()) {
				updatedPayload.put(entry.getKey(), uploadService.generatePresignedUrl(entry.getValue()));
				modified = true;
			}
		}
		if (!modified) return item;
		return new SubmissionItem(item.id(), updatedPayload, item.createdAt(), item.isSpam(), item.emailAutoresponseEmailStatus(), item.emailNotifStatus());
	}

	@WithSpan
	@Override
	public FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId) {
		FormSubmissionsResponse cached = redisCache.getOrCompute(CacheNames.FORM_SUBMISSIONS, formId.toString(), FormSubmissionsResponse.class, () -> {
			var partitioned = submissionRepository.findAllByFormIdOrderByCreatedAtDesc(formId).stream()
				.collect(Collectors.partitioningBy(SubmissionItem::isSpam));
			return new FormSubmissionsResponse(partitioned.getOrDefault(false, List.of()), partitioned.getOrDefault(true, List.of()));
		});

		List<SubmissionItem> validWithPresigned = cached.submissions().stream().map(this::attachPresignedUrls).toList();
		List<SubmissionItem> spamWithPresigned = cached.spam().stream().map(this::attachPresignedUrls).toList();
		return new FormSubmissionsResponse(validWithPresigned, spamWithPresigned);
	}

	@WithSpan
	@Override
	public void updateFormSubmissionsCache(UUID formId, SubmissionItem newSubmission) {
		Optional<FormSubmissionsResponse> cachedResponseOpt = redisCache.get(CacheNames.FORM_SUBMISSIONS, formId.toString(), FormSubmissionsResponse.class);
		if (cachedResponseOpt.isEmpty()) {
			log.debug("Cache MISS for form ID: {}. Skipping partial update (will be built on next read).", formId);
			return;
		}
		FormSubmissionsResponse response = cachedResponseOpt.get();

		if (newSubmission.isSpam()) {
			List<SubmissionItem> subs = response.spam();
			boolean replaced = false;

			for (ListIterator<SubmissionItem> it = subs.listIterator(); it.hasNext(); ) {
				SubmissionItem s = it.next();
				if (s.id().equals(newSubmission.id())) {
					it.set(newSubmission);
					replaced = true;
					break;
				}
			}

			if (!replaced) {
				subs.addFirst(newSubmission);
			}
		} else {
			List<SubmissionItem> subs = response.submissions();
			boolean replaced = false;

			for (ListIterator<SubmissionItem> it = subs.listIterator(); it.hasNext(); ) {
				SubmissionItem s = it.next();
				if (s.id().equals(newSubmission.id())) {
					it.set(newSubmission);
					replaced = true;
					break;
				}
			}

			if (!replaced) {
				subs.addFirst(newSubmission);
			}
		}
		redisCache.set(CacheNames.FORM_SUBMISSIONS, formId.toString(), new FormSubmissionsResponse(response.submissions(), response.spam()));
	}

	@WithSpan
	@Transactional
	@Override
	public void deleteSubmission(UUID tenantId, UUID submissionId) {
		Optional<Submission> subOpt = submissionRepository.findById(submissionId);
		if (subOpt.isEmpty()) {
			log.warn("Submission deletion requested for non-existent ID: {}", submissionId);
			return;
		}
		Submission sub = subOpt.get();

		if (!tenantId.equals(sub.getTenantId())) {
			log.warn("Unauthorized submission deletion attempt by tenant {} for submission {}", tenantId, submissionId);
			throw new IllegalArgumentException("Unauthorized deletion attempt.");
		}

		Map<String, String> payload = sub.getPayload();
		if (payload != null && !payload.isEmpty()) {
			for (Map.Entry<String, String> entry : payload.entrySet()) {
				if (entry.getKey() != null && entry.getKey().endsWith("__url")) {
					String fileUrl = entry.getValue();
					if (fileUrl != null && !fileUrl.isBlank()) {
						try {
							uploadService.deleteFileByUrl(fileUrl.strip());
						} catch (Exception e) {
							log.error("Failed to delete attachment S3 object for URL: {}", fileUrl, e);
						}
					}
				}
			}
		}

		UUID formId = sub.getFormId();
		submissionRepository.delete(sub);
		log.info("Successfully deleted submission ID: {} for tenant ID: {}", submissionId, tenantId);

		redisCache.delete(CacheNames.FORM_SUBMISSIONS, formId.toString());
	}

}