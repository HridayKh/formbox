package formbox.core.service;

import formbox.auth.GenericAuthException;
import formbox.billing.model.Entitlements;
import formbox.core.FormNotFoundException;
import formbox.core.dto.FormSettingsRequest;
import formbox.core.dto.TierValidationResult;
import formbox.core.entity.Form;
import formbox.core.repository.FormRepository;
import formbox.core.cache.FormCacheService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormSettingsService {

	private final FormRepository formRepository;
	private final FormCacheService formCacheService;
	private final FormTierValidator tierValidator;

	@Transactional
	@WithSpan
	public TierValidationResult updateFormSettings(UUID formId, String userId, FormSettingsRequest request) {
		log.debug("Updating form settings for form ID: {} by user: {}", formId, userId);
		Form form = formRepository.findById(formId)
			.orElseThrow(() -> new FormNotFoundException(formId));

		// 1. Enforce Authorization Guard
		if (!form.getTenant().getId().toString().equals(userId)) {
			log.warn("Unauthorized settings update attempt for form ID: {} by user: {}", formId, userId);
			throw new GenericAuthException("Unauthorized access to form system.");
		}

		// 2. Validate and Sanitize inputs based on Subscription Tier
		Entitlements entitlements = form.getTenant().getEntitlementsOrDefaults();
		TierValidationResult validationResult = tierValidator.validateAndSanitize(request, entitlements);
		FormSettingsRequest sanitized = validationResult.sanitizedRequest();

		form.setName(sanitized.name());
		form.setRedirectUrl(sanitized.redirectUrl());
		form.setIsActive(sanitized.isActive());
		form.setTurnstileSecretKey(sanitized.turnstileSecretKey());
		form.setHoneypotName(sanitized.honeypotName());
		form.setRateLimitRpm(sanitized.rateLimitRpm());
		form.setAllowFiles(sanitized.allowFiles());
		form.setAllowHtmx(sanitized.allowHtmx());
		form.setAllowJson(sanitized.allowJson());
		form.setFieldValidations(sanitized.fieldValidations());

		Form savedForm = formRepository.save(form);

		formCacheService.updateFormCache(savedForm);
		formCacheService.evictTenantForms(savedForm.getTenant().getId());

		validationResult.setUpdatedForm(savedForm.toCachedFormDto());

		log.info("Successfully updated form settings for form ID: {} (tenant: {})", formId, savedForm.getTenant().getId());
		return validationResult;
	}
}