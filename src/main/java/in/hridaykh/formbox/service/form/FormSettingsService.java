package in.hridaykh.formbox.service.form;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.exception.FormNotFoundException;
import in.hridaykh.formbox.exception.auth.SessionExpiredException;
import in.hridaykh.formbox.model.dto.FormSettingsRequest;
import in.hridaykh.formbox.model.dto.TierValidationResult;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.service.cache.FormCacheService;
import in.hridaykh.formbox.service.cache.TenantCacheService;
import in.hridaykh.formbox.util.FormTierValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormSettingsService {

	private final FormRepository formRepository;
	private final TenantCacheService tenantCacheService;
	private final FormCacheService formCacheService;
	private final FormTierValidator tierValidator;

	@Transactional
	public TierValidationResult updateFormSettings(UUID formId, String userId, FormSettingsRequest request) {
		Form form = formRepository.findById(formId)
			.orElseThrow(() -> new FormNotFoundException(formId));

		// 1. Enforce Authorization Guard
		if (!form.getTenant().getId().toString().equals(userId)) {
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

		return validationResult;
	}
}