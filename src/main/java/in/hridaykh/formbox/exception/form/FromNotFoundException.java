package in.hridaykh.formbox.exception.form;

import java.util.UUID;

public class FromNotFoundException extends RuntimeException {
	public FromNotFoundException(UUID formId) {
		super("Form with id: " + formId + " not found!");
	}
}
