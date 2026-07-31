package formbox.auth;


public class GenericAuthException extends RuntimeException {
	public GenericAuthException(String message) {
		super(message);
	}

	public GenericAuthException(String message, Throwable cause) {
		super(message, cause);
	}
}

