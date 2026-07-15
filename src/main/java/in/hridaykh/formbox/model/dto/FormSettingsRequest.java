package in.hridaykh.formbox.model.dto;

import java.util.List;

public record FormSettingsRequest(
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