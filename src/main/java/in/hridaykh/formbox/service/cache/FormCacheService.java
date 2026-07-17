package in.hridaykh.formbox.service.cache;

import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.exception.FormNotFoundException;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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

	private final FormRepository formRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final TenantRepository tenantRepository;

	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.FORM_METADATA, key = "#formId")
	@WithSpan
	public CachedForm getCachedForm(UUID formId) {
		log.trace("Caffeine L1 cache MISS for form ID: {}", formId);

		String redisKey = String.format("formbox:%s:%s", CacheNames.FORM_METADATA, formId);
		String cachedJson = null;
		try {
			cachedJson = redisTemplate.opsForValue().get(redisKey);
		} catch (Exception e) {
			log.error("Failed to connect to Redis cluster while fetching key: {}", redisKey, e);
		}

		if (cachedJson != null) {
			try {
				log.trace("Redis L2 cache HIT for form ID: {}", formId);
				return objectMapper.readValue(cachedJson, CachedForm.class);
			} catch (Exception e) {
				log.error("Failed to deserialize CachedForm JSON payload from Redis for ID: {}", formId, e);
			}
		}

		log.debug("Redis L2 cache MISS for form ID: {}. Fetching from persistent database...", formId);
		Form form = formRepository.findById(formId).orElseThrow(() -> {
			log.warn("Form retrieval failed. Record not found in database for ID: {}", formId);
			return new FormNotFoundException(formId);
		});

		CachedForm cachedFormDto = form.toCachedFormDto();

		try {
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(cachedFormDto), Duration.ofDays(2));
			log.trace("Successfully backfilled Redis L2 cache for form ID: {}", formId);
		} catch (Exception e) {
			log.error("Failed to serialize and save CachedForm metadata to Redis for ID: {}", formId, e);
		}

		return cachedFormDto;
	}

	@CachePut(value = CacheNames.FORM_METADATA, key = "#updatedForm.id")
	@WithSpan
	public CachedForm updateFormCache(Form updatedForm) {
		UUID formId = updatedForm.getId();
		log.debug("Synchronizing state updates to cache layers for form ID: {}", formId);

		CachedForm cachedFormDto = updatedForm.toCachedFormDto();
		String redisKey = String.format("formbox:%s:%s", CacheNames.FORM_METADATA, formId);

		try {
			redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(cachedFormDto), Duration.ofDays(2));
			log.trace("Redis L2 cache successfully updated for form ID: {}", formId);
		} catch (Exception e) {
			log.error("Failed to update execution write-through to Redis L2 cache for form ID: {}", formId, e);
		}

		return cachedFormDto;
	}

	@CacheEvict(value = CacheNames.FORM_METADATA, key = "#formId")
	@WithSpan
	public void evictFormCache(UUID formId) {
		log.debug("Evicting multi-layer form metadata caches for form ID: {}", formId);
		try {
			Boolean deleted = redisTemplate.delete(String.format("formbox:%s:%s", CacheNames.FORM_METADATA, formId));
			log.trace("Redis L2 cache eviction completion status for form ID {}: {}", formId, deleted);
		} catch (Exception e) {
			log.error("Failed to purge key from Redis L2 layer during explicit eviction for form ID: {}", formId, e);
		}
	}

	@WithSpan
	public List<CachedForm> getTenantForms(UUID tenantId) {
		String cacheKey = String.format("formbox:%s:%s", CacheNames.TENANT_FORMS, tenantId);

		String cachedJson = redisTemplate.opsForValue().get(cacheKey);
		if (cachedJson != null) {
			try {
				log.trace("Redis L2 cache HIT for tenant forms list on tenant ID: {}", tenantId);
				return objectMapper.readValue(cachedJson, new TypeReference<>() {
				});
			} catch (Exception e) {
				log.error("Failed to parse tenant forms collection payload from Redis context for tenant: {}", tenantId, e);
			}
		}

		log.debug("Redis L2 cache MISS for tenant forms on tenant ID: {}. Loading relations from database...", tenantId);
		Tenant tenant = tenantRepository.getReferenceById(tenantId);
		List<CachedForm> dbForms = formRepository.findByTenantAndIsDeletedIsFalse(tenant).stream().map(Form::toCachedFormDto).toList();
		log.trace("Database query completed. Found {} active forms for tenant ID: {}", dbForms.size(), tenantId);

		try {
			redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dbForms), Duration.ofDays(2));
			log.trace("Tenant forms cache array backfilled successfully for tenant ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to populate tenant forms payload buffer inside Redis for tenant: {}", tenantId, e);
		}

		return dbForms;
	}

	@WithSpan
	public void evictTenantForms(UUID tenantId) {
		log.debug("Request received to drop tenant forms collection cache for tenant ID: {}", tenantId);
		try {
			Boolean deleted = redisTemplate.delete(String.format("formbox:%s:%s", CacheNames.TENANT_FORMS, tenantId));
			log.trace("Tenant forms clear task evaluated for tenant ID {}: {}", tenantId, deleted);
		} catch (Exception e) {
			log.error("Failed to purge tenant forms cache collection tracker from Redis cluster for tenant: {}", tenantId, e);
		}
	}
}