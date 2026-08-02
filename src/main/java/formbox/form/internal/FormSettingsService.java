package formbox.form.internal;

import formbox.billing.EntitlementsApi;
import formbox.form.FormApi;
import formbox.shared.GenericAuthException;
import formbox.shared.Entitlements;
import formbox.shared.FormNotFoundException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class FormSettingsService {

	private final FormRepository formRepository;
	private final FormApi formApi;
	private final EntitlementsApi entitlementsApi;

	@Transactional
	@WithSpan
	public FormUpdateValidationRes updateFormSettings(UUID formId, UUID tenantId, FormSettingsRequest request) {
		log.debug("Updating form settings for form ID: {} by user: {}", formId, tenantId);
		Form form = formRepository.findById(formId).orElseThrow(() -> new FormNotFoundException(formId));

		if (!form.getTenantId().equals(tenantId)) {
			log.warn("Unauthorized settings update attempt for form ID: {} by user: {}", formId, tenantId);
			throw new GenericAuthException("Unauthorized access to form system.");
		}

		var validationResult = validateAndSanitize(request, entitlementsApi.getEntitlements(tenantId));

		form.fromFormSettingsRequest(validationResult.getSanitizedRequest());

		Form savedForm = formRepository.save(form);
		formApi.updateFormCache(savedForm.toFormDto());
		formApi.evictTenantForms(savedForm.getTenantId());

		validationResult.setFolderId(savedForm.getFolderId());

		log.info("Successfully updated form settings for form ID: {} (tenant: {})", formId, savedForm.getTenantId());
		return validationResult;
	}

	FormUpdateValidationRes validateAndSanitize(FormSettingsRequest request, Entitlements entitlements) {
		List<String> warnings = new ArrayList<>();

		// Rule 1: Custom Redirects
		if (!entitlements.redirectUrlsAllowed() && StringUtils.hasText(request.getRedirectUrl())) {
			request.setRedirectUrl(null);
			warnings.add("Settings updated, but custom redirects require a premium upgrade!");
		}

		// Rule 2: Turnstile verification
		if (!entitlements.turnstileAllowed() && StringUtils.hasText(request.getTurnstileSecretKey())) {
			request.setTurnstileSecretKey(null);
			warnings.add("Turnstile validation is not allowed on your current tier. Please upgrade!");
		}

		// Rule 3: JSON submissions
		if (!entitlements.jsonFormsAllowed() && request.isAllowJson()) {
			request.setAllowJson(false);
			warnings.add("JSON submission is not allowed on your current tier. Please upgrade!");
		}

		// Rule 4: File uploads
		if (!entitlements.fileUploadsAllowed() && request.isAllowFiles()) {
			request.setAllowFiles(false);
			warnings.add("File uploads are not allowed on your current tier. Please upgrade!");
		}

		// Rule 5: Rate limit RPM capping
		if (request.getRateLimitRpm() > entitlements.maxRateLimitRpm()) {
			request.setRateLimitRpm(entitlements.maxRateLimitRpm());
			warnings.add("Rate limit cannot exceed " + entitlements.maxRateLimitRpm() + " RPM on your current tier.");
		}

		// Rule 6: Field validations
		if (!entitlements.fieldValidationsAllowed()) {
			request.setFieldValidations(List.of());
			warnings.add("Field validations require a higher tier. Please upgrade!");
		}

		return new FormUpdateValidationRes(null, request, warnings);
	}
}