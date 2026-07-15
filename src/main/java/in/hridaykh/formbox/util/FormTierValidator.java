package in.hridaykh.formbox.util;

import in.hridaykh.formbox.constant.Tiers;
import in.hridaykh.formbox.model.dto.FormSettingsRequest;
import in.hridaykh.formbox.model.dto.TierValidationResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class FormTierValidator {

	public TierValidationResult validateAndSanitize(FormSettingsRequest request, String subscriptionTier) {
		List<String> warnings = new ArrayList<>();
		String sanitizedRedirectUrl = request.redirectUrl();

		// Rule 1: Custom Redirects
		if (!Tiers.t(subscriptionTier).redirectUrlAllowed() && StringUtils.hasText(sanitizedRedirectUrl)) {
			sanitizedRedirectUrl = null;
			warnings.add("Settings updated, but custom redirects require a premium upgrade!");
		}

		// Rule 2: Future rules become trivial to add here:
		// if (!Tiers.t(subscriptionTier).fileUploadsAllowed() && request.allowFiles()) { ... }

		FormSettingsRequest sanitizedRequest = new FormSettingsRequest(
			request.name(),
			sanitizedRedirectUrl,
			request.isActive(),
			request.turnstileSecretKey(),
			request.honeypotName(),
			request.rateLimitRpm(),
			request.allowFiles(),
			request.allowHtmx(),
			request.allowJson(),
			request.fieldValidations()
		);

		return new TierValidationResult(sanitizedRequest, warnings, null);
	}
}

