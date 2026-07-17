package in.hridaykh.formbox.service.form;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.exception.FormNotFoundException;
import in.hridaykh.formbox.exception.auth.SessionExpiredException;
import in.hridaykh.formbox.model.dto.FormSettingsRequest;
import in.hridaykh.formbox.model.dto.TierValidationResult;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.util.FormTierValidator;
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
			throw new SessionExpiredException("Unauthorized access to form system.");
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