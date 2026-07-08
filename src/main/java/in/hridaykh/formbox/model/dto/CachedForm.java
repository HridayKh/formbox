package in.hridaykh.formbox.model.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record CachedForm(
	UUID id,
	UUID tenantId,
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
) implements Serializable {
	public CachedForm {
		fieldValidations = List.copyOf(fieldValidations);
	}
}