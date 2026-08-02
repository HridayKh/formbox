package formbox.form.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
final class FormUpdateValidationRes {
	@Setter
	private UUID folderId;
	private final FormSettingsRequest sanitizedRequest;
	private final List<String> warnings;

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

}


