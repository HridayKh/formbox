package formbox.form.internal;

import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.shared.CacheNames;
import formbox.shared.FormNotFoundException;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class FormService implements FormApi {

	private final FormRepository formRepository;
	private final RedisCache redisCache;

	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.FORM_METADATA, key = "#formId.toString()")
	@WithSpan
	@Override
	public FormDto getCachedForm(UUID formId) {
		return redisCache.getOrCompute(CacheNames.FORM_METADATA, formId.toString(), FormDto.class, () -> {
			Form form = formRepository.findById(formId).orElseThrow(() -> {
				log.warn("Form retrieval failed. Record not found in database for ID: {}", formId);
				return new FormNotFoundException(formId);
			});
			return form.toCachedFormDto();
		});
	}

	@CachePut(value = CacheNames.FORM_METADATA, key = "#updatedForm.id.toString()")
	@WithSpan
	@Override
	public void updateFormCache(FormDto updatedForm) {
		redisCache.set(CacheNames.FORM_METADATA, updatedForm.id().toString(), updatedForm);
	}

	@CacheEvict(value = CacheNames.FORM_METADATA, key = "#formId.toString()")
	@WithSpan
	@Override
	public void evictFormCache(UUID formId) {
		redisCache.delete(CacheNames.FORM_METADATA, formId.toString());
	}

	@WithSpan
	@Override
	public List<FormDto> getTenantForms(UUID tenantId) {
		return redisCache.getOrCompute(CacheNames.TENANT_FORMS, tenantId.toString(), new TypeReference<>() {
		}, () -> formRepository.findByTenantIdAndIsDeletedIsFalse(tenantId).stream().map(Form::toCachedFormDto).toList());
	}

	@WithSpan
	@Override
	public void evictTenantForms(UUID tenantId) {
		redisCache.delete(CacheNames.TENANT_FORMS, tenantId.toString());
	}

}