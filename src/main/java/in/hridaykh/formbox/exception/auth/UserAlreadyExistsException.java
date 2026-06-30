package in.hridaykh.formbox.exception.auth;

public class UserAlreadyExistsException extends AuthException {
	public UserAlreadyExistsException(String message) {
		super(message);
	}
}
