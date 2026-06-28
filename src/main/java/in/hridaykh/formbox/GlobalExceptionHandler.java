package in.hridaykh.formbox;

import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import io.github.jan.supabase.auth.exception.AuthRestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthWeakPasswordException.class)
	public ResponseEntity<?> handleWeakPassword(AuthWeakPasswordException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", "Weak Password", "message", "Password must be at least 8 characters long and contain uppercase, lowercase, digits, and symbols.", "msg", ex.getMessage()));
	}

	@ExceptionHandler(AuthRestException.class)
	public ResponseEntity<?> handleSupabaseAuthErrors(AuthRestException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", "Authentication Error", "message", ex.getMessage()));
	}
}