package formbox.auth.internal;

import formbox.auth.GenericAuthException;

class InvalidCredentialsException extends GenericAuthException {
	public InvalidCredentialsException(String message) {
		super(message);
	}
}

class TurnstileAuthException extends Exception {
	public TurnstileAuthException(String message) {
		super(message);
	}
}
