package formbox.shared.exception;

import java.util.UUID;

public class FormNotFoundException extends IllegalArgumentException {
	public FormNotFoundException(UUID formId) {
		super("Form " + formId + "not found!");
	}
}
