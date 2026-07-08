package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.dto.SubmissionItem;
import in.hridaykh.formbox.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionCacheService {
	private final SubmissionRepository submissionRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId) {
		log.trace("Request received to fetch grouped form submissions for form ID: {}", formId);
		String cacheKey = String.format("formbox:%s:%s", CacheNames.FORM_SUBMISSIONS, formId);

		String cachedJson = null;
		try {
			cachedJson = redisTemplate.opsForValue().get(cacheKey);
		} catch (Exception e) {
			log.error("Failed to read from Redis during form submissions fetch for key: {}", cacheKey, e);
		}

		if (cachedJson != null) {
			try {
				log.trace("Redis cache HIT for form submissions on form ID: {}", formId);
				return objectMapper.readValue(cachedJson, FormSubmissionsResponse.class);
			} catch (Exception e) {
				log.error("Failed to deserialize form submissions from Redis cache payload for form ID: {}", formId, e);
			}
		}

		log.debug("Redis cache MISS for form submissions on form ID: {}. Executing database query...", formId);

		List<SubmissionItem> submissions;
		try {
			submissions = submissionRepository.findAllByFormId(formId);
			log.trace("Database query completed. Fetched {} total submissions for form ID: {}", submissions.size(), formId);
		} catch (Exception e) {
			log.error("Database execution failure when listing submissions for form ID: {}", formId, e);
			throw e;
		}

		Map<Boolean, List<SubmissionItem>> partitioned = submissions.stream()
			.collect(Collectors.partitioningBy(SubmissionItem::isSpam));

		List<SubmissionItem> validSubmissions = partitioned.getOrDefault(false, List.of());
		List<SubmissionItem> spamSubmissions = partitioned.getOrDefault(true, List.of());
		log.debug("Partitioning logic completed for form ID: {}. Valid submissions: {}, Spam submissions: {}", formId, validSubmissions.size(), spamSubmissions.size());

		FormSubmissionsResponse response = new FormSubmissionsResponse(validSubmissions, spamSubmissions);

		try {
			String jsonString = objectMapper.writeValueAsString(response);
			redisTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofHours(1));
			log.trace("Successfully populated Redis submission cache for form ID: {}", formId);
		} catch (Exception e) {
			log.error("Failed to serialize and update form submissions to Redis cache for form ID: {}", formId, e);
		}

		return response;
	}
}