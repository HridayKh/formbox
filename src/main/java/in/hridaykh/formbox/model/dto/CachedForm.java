package in.hridaykh.formbox.model.dto;

import java.io.Serializable;
import java.util.UUID;

public record CachedForm(
	UUID id,
	UUID tenantId,
	String name,
	String redirectUrl,
	String turnstileSecretKey,
	Boolean isActive
) implements Serializable {

}