package formbox.form.internal;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
class FormSettingsRequest {

	private UUID id;

	// General
	private String name;
	private boolean active;
	private String redirectUrl;

	// Security / spam
	private String honeypotName;
	private String turnstileSecretKey;
	private boolean altchaEnabled;
	private int rateLimitRpm;

	// Capabilities
	private boolean allowFiles;
	private boolean allowHtmx;
	private boolean allowJson;
	private boolean emailDigestsEnabled;

	// Integrations
	private boolean discordNotifs;
	private boolean slackNotifs;
	private boolean telegramNotifs;
	private boolean customWebhooks;

	// Field validation rules, raw textarea content (one rule per line)
	private String fieldValidationsRaw;

	public List<String> getFieldValidations() {
		if (fieldValidationsRaw == null || fieldValidationsRaw.isBlank()) return List.of();
		return Arrays.stream(fieldValidationsRaw.split("\\r?\\n")).map(String::trim).filter(line -> !line.isEmpty()).toList();
	}

	public void setFieldValidations(List<String> validations) {
		this.fieldValidationsRaw = String.join("\n", validations);
	}

}
