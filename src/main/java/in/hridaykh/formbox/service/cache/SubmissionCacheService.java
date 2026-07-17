package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.dto.SubmissionItem;
import in.hridaykh.formbox.repository.SubmissionRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionCacheService {
	private final SubmissionRepository submissionRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@WithSpan
	public FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId) {
		log.trace("Fetching grouped form submissions for form ID: {}", formId);
		String cacheKey = String.format("formbox:%s:%s", CacheNames.FORM_SUBMISSIONS, formId);

		try {
			String cachedJson = redisTemplate.opsForValue().get(cacheKey);
			if (cachedJson != null) {
				log.trace("Redis cache HIT for form ID: {}", formId);
				return objectMapper.readValue(cachedJson, FormSubmissionsResponse.class);
			}
		} catch (Exception e) {
			log.error("Redis operation failed for form ID: {} (Falling back to DB)", formId, e);
		}

		log.debug("Redis cache MISS for form ID: {}. Querying database...", formId);

		List<SubmissionItem> submissions;
		try {
			submissions = submissionRepository.findAllByFormId(formId);
		} catch (Exception e) {
			log.error("Database execution failure for form ID: {}", formId, e);
			throw e;
		}

		var partitioned = submissions.stream().collect(Collectors.partitioningBy(SubmissionItem::isSpam));
		var resp = new FormSubmissionsResponse(partitioned.getOrDefault(false, List.of()), partitioned.getOrDefault(true, List.of()));

		log.debug("Partitioned form ID: {}. Valid: {}, Spam: {}", formId, resp.submissions().size(), resp.spam().size());

		try {
			redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(resp), Duration.ofDays(2));
		} catch (Exception e) {
			log.error("Failed to update Redis cache for form ID: {}", formId, e);
		}

		return resp;
	}

	@WithSpan
	public void updateFormSubmissionsCache(UUID formId, SubmissionItem newSubmission) {
		String cacheKey = String.format("formbox:%s:%s", CacheNames.FORM_SUBMISSIONS, formId);

		try {
			String cachedJson = redisTemplate.opsForValue().get(cacheKey);
			if (cachedJson == null) {
				log.debug("Cache MISS for form ID: {}. Skipping partial update (will be built on next read).", formId);
				return;
			}

			FormSubmissionsResponse response = objectMapper.readValue(cachedJson, FormSubmissionsResponse.class);

			List<SubmissionItem> validList = new ArrayList<>(response.submissions());
			List<SubmissionItem> spamList = new ArrayList<>(response.spam());

			if (newSubmission.isSpam())
				spamList.addFirst(newSubmission);
			else
				validList.addFirst(newSubmission);

			FormSubmissionsResponse updatedResponse = new FormSubmissionsResponse(validList, spamList);
			redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(updatedResponse), Duration.ofDays(2));
			log.debug("Successfully appended new submission to cache for form ID: {}", formId);
		} catch (Exception e) {
			log.error("Failed to update Redis cache inline for form ID: {}", formId, e);
		}
	}
}