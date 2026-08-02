package formbox.form.internal;

import formbox.form.FormDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
final class FormUpdateValidationRes {
	@Setter
	@Getter
	private FormDto updatedForm;
	private final FormSettingsRequest sanitizedRequest;
	private final List<String> warnings;

	public FormDto updatedForm() {
		return this.updatedForm;
	}

	public FormSettingsRequest sanitizedRequest() {
		return this.sanitizedRequest;
	}

	public List<String> warnings() {
		return this.warnings;
	}

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

}

record FormSettingsRequest(
	String name,
	String redirectUrl,
	Boolean isActive,
	String turnstileSecretKey,
	String honeypotName,
	Integer rateLimitRpm,
	Boolean allowFiles,
	Boolean allowHtmx,
	Boolean allowJson,
	List<String> fieldValidations
) {
	public FormSettingsRequest {
		isActive = isActive != null && isActive;
		allowFiles = allowFiles != null && allowFiles;
		allowHtmx = allowHtmx != null && allowHtmx;
		allowJson = allowJson != null && allowJson;
		fieldValidations = fieldValidations == null ? List.of() : fieldValidations;
	}
}