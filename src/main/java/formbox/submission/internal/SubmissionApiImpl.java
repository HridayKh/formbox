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

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionApiImpl implements SubmissionApi {
	private final SubmissionRepository submissionRepository;
	private final RedisCache redisCache;

	@WithSpan
	@Override
	public FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId) {
		return redisCache.getOrCompute(CacheNames.FORM_SUBMISSIONS, formId.toString(), FormSubmissionsResponse.class, () -> {
			var partitioned = submissionRepository.findAllByFormIdOrderByCreatedAtDesc(formId).stream()
				.collect(Collectors.partitioningBy(SubmissionItem::isSpam));
			return new FormSubmissionsResponse(partitioned.getOrDefault(false, List.of()), partitioned.getOrDefault(true, List.of()));
		});
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

}