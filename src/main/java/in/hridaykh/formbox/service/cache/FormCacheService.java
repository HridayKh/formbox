package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormCacheService {

	private static final String TENANT_FORMS_BASE_KEY = "formbox:tenantForms:";

	private final FormRepository formRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final TenantRepository tenantRepository;

	@Transactional(readOnly = true)
	@Cacheable(value = "formMetadata", key = "#formId")
	public CachedForm getCachedForm(UUID formId) {
		log.info("Caffeine MISS for form {}", formId);
		Form form = formRepository.findById(formId).orElseThrow(() -> new IllegalArgumentException("Form not found for ID: " + formId));
		return form.toCachedFormDto();
	}

	@CachePut(value = "formMetadata", key = "#updatedForm.id")
	public CachedForm updateFormCache(Form updatedForm) {
		return updatedForm.toCachedFormDto();
	}

	@CacheEvict(value = "formMetadata", key = "#formId")
	public void evictFormCache(UUID formId) {
		log.info("Caffeine cache evicted for form {}", formId);
	}

	public List<CachedForm> getTenantForms(UUID tenantId) {
		String cacheKey = TENANT_FORMS_BASE_KEY + tenantId;

		String cachedJson = redisTemplate.opsForValue().get(cacheKey);
		if (cachedJson != null) {
			try {
				return objectMapper.readValue(cachedJson, new TypeReference<>() {
				});
			} catch (Exception e) {
				log.error("Failed to parse tenant forms from Redis", e);
			}
		}

		Tenant tenant = tenantRepository.getReferenceById(tenantId);
		List<CachedForm> dbForms = formRepository.findByTenantAndIsDeletedIsFalse(tenant).stream().map(Form::toCachedFormDto).toList();

		try {
			redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dbForms), Duration.ofDays(2));
		} catch (Exception e) {
			log.error("Failed to save tenant forms to Redis", e);
		}

		return dbForms;
	}

	public void evictTenantForms(UUID tenantId) {
		redisTemplate.delete(TENANT_FORMS_BASE_KEY + tenantId);
	}
}