package formbox.form.internal;

import formbox.billing.EntitlementsApi;
import formbox.form.FormApi;
import formbox.shared.GenericAuthException;
import formbox.billing.Entitlements;
import formbox.shared.FormNotFoundException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class FormSettingsService {

	private final FormRepository formRepository;
	private final FormApi formApi;
	private final FormTierValidator tierValidator;
	private final EntitlementsApi entitlementsApi;

	@Transactional
	@WithSpan
	public FormTierValidationResult updateFormSettings(UUID formId, String userId, FormSettingsRequest request) {
		log.debug("Updating form settings for form ID: {} by user: {}", formId, userId);
		Form form = formRepository.findById(formId)
			.orElseThrow(() -> new FormNotFoundException(formId));

		// 1. Enforce Authorization Guard
		if (!form.getTenantId().toString().equals(userId)) {
			log.warn("Unauthorized settings update attempt for form ID: {} by user: {}", formId, userId);
			throw new GenericAuthException("Unauthorized access to form system.");
		}

		// 2. Validate and Sanitize inputs based on Subscription Tier
		Entitlements entitlements = entitlementsApi.getEntitlements(form.getTenantId());
		FormTierValidationResult validationResult = tierValidator.validateAndSanitize(request, entitlements);
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

		formApi.updateFormCache(savedForm.toFormDto());
		formApi.evictTenantForms(savedForm.getTenantId());

		validationResult.setUpdatedForm(savedForm.toFormDto());

		log.info("Successfully updated form settings for form ID: {} (tenant: {})", formId, savedForm.getTenantId());
		return validationResult;
	}
}