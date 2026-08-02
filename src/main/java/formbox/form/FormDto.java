package formbox.form;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record FormDto(
	UUID id,
	UUID tenantId,
	String name,
	String redirectUrl,
	Boolean isActive,
	String turnstileSecretKey,
	@DefaultValue("_gotcha") String honeypotName,
	Integer rateLimitRpm,
	Boolean allowFiles,
	Boolean allowHtmx,
	Boolean allowJson,
	List<String> fieldValidations,
	UUID folderId,
	FormNotifs formNotifs
) implements Serializable {
	public FormDto {
		fieldValidations = List.copyOf(fieldValidations);
	}
}