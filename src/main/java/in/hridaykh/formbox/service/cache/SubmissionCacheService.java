package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.model.dto.FormSubmissionsResponse;
import in.hridaykh.formbox.model.dto.SubmissionItem;
import in.hridaykh.formbox.repository.SubmissionRepository;
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
public class SubmissionCacheService {
	public static final String CACHE_KEY_BASE = "formbox:formSubmissions:";

	private final SubmissionRepository submissionRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public SubmissionCacheService(SubmissionRepository submissionRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.submissionRepository = submissionRepository;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public FormSubmissionsResponse getFormSubmissionsGrouped(UUID formId) {
		String cacheKey = CACHE_KEY_BASE + formId;

		String cachedJson = redisTemplate.opsForValue().get(cacheKey);
		if (cachedJson != null) {
			try {
				return objectMapper.readValue(cachedJson, FormSubmissionsResponse.class);
			} catch (Exception e) {
				log.error("Failed to deserialize form submissions from cache", e);
			}
		}

		Map<Boolean, List<SubmissionItem>> partitioned = submissionRepository.findAllByFormId(formId).stream().collect(Collectors.partitioningBy(SubmissionItem::isSpam));
		FormSubmissionsResponse response = new FormSubmissionsResponse(partitioned.getOrDefault(false, List.of()), partitioned.getOrDefault(true, List.of()));

		try {
			String jsonString = objectMapper.writeValueAsString(response);
			redisTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofHours(1));
		} catch (Exception e) {
			log.error("Failed to serialize form submissions to cache", e);
		}

		return response;
	}

}
