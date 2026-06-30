package in.hridaykh.formbox.exception.auth;

public class SessionExpiredException extends AuthException {
	public SessionExpiredException(String message) {
		super(message);
	}
}
