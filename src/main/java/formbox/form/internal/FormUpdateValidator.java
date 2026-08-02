package formbox.form.internal;

import formbox.shared.Entitlements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
class FormUpdateValidator {

	public FormUpdateValidationRes validateAndSanitize(FormSettingsRequest request, Entitlements entitlements) {
		List<String> warnings = new ArrayList<>();
		String sanitizedRedirectUrl = request.redirectUrl();

		// Rule 1: Custom Redirects
		if (!entitlements.redirectUrlsAllowed() && StringUtils.hasText(sanitizedRedirectUrl)) {
			sanitizedRedirectUrl = null;
			warnings.add("Settings updated, but custom redirects require a premium upgrade!");
		}

		// Rule 2: Turnstile verification
		String sanitizedTurnstileSecretKey = request.turnstileSecretKey();
		if (!entitlements.turnstileAllowed() && StringUtils.hasText(sanitizedTurnstileSecretKey)) {
			sanitizedTurnstileSecretKey = null;
			warnings.add("Turnstile validation is not allowed on your current tier. Please upgrade!");
		}

		// Rule 3: JSON submissions
		boolean sanitizedAllowJson = request.allowJson();
		if (!entitlements.jsonFormsAllowed() && sanitizedAllowJson) {
			sanitizedAllowJson = false;
			warnings.add("JSON submission is not allowed on your current tier. Please upgrade!");
		}

		// Rule 4: File uploads
		boolean sanitizedAllowFiles = request.allowFiles();
		if (!entitlements.fileUploadsAllowed() && sanitizedAllowFiles) {
			sanitizedAllowFiles = false;
			warnings.add("File uploads are not allowed on your current tier. Please upgrade!");
		}

		// Rule 5: Rate limit RPM capping
		int sanitizedRateLimitRpm = request.rateLimitRpm();
		if (sanitizedRateLimitRpm > entitlements.maxRateLimitRpm()) {
			sanitizedRateLimitRpm = entitlements.maxRateLimitRpm();
			warnings.add("Rate limit cannot exceed " + entitlements.maxRateLimitRpm() + " RPM on your current tier.");
		}

		// Rule 6: Field validations
		List<String> sanitizedFieldValidations = request.fieldValidations();
		if (!entitlements.fieldValidationsAllowed() && !sanitizedFieldValidations.isEmpty()) {
			sanitizedFieldValidations = List.of();
			warnings.add("Field validations require a premium tier. Please upgrade!");
		}

		FormSettingsRequest sanitizedRequest = new FormSettingsRequest(
			request.name(),
			sanitizedRedirectUrl,
			request.isActive(),
			sanitizedTurnstileSecretKey,
			request.honeypotName(),
			sanitizedRateLimitRpm,
			sanitizedAllowFiles,
			request.allowHtmx(),
			sanitizedAllowJson,
			sanitizedFieldValidations
		);

		return new FormUpdateValidationRes(null, sanitizedRequest, warnings);
	}
}

