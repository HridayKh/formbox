package in.hridaykh.formbox;

import io.github.jan.supabase.auth.exception.AuthErrorCode;
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import io.github.jan.supabase.auth.exception.AuthRestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthWeakPasswordException.class)
	public ResponseEntity<?> handleWeakPassword(AuthWeakPasswordException ex) {
		String errorDesc = "Password must be at least 8 characters long and contain uppercase, lowercase, digits, and symbols.";
		var errorName = Map.entry("error", "Weak Password");
		var errorMessage = Map.entry("message", ex.getDescription() == null ? errorDesc : ex.getDescription());
		var errorReasons = Map.entry("reasons", ex.getReasons());
		return ResponseEntity.badRequest().body(Map.ofEntries(errorName, errorMessage, errorReasons));
	}

	@ExceptionHandler(AuthRestException.class)
	public ResponseEntity<Map<String, Object>> handleSupabaseAuthErrors(AuthRestException ex) {
		Map<String, Object> body = new LinkedHashMap<>();

		AuthErrorCode typedCode = ex.getErrorCode();
		String rawCode = typedCode != null ? typedCode.name().toLowerCase() : ex.getError();
		body.put("code", rawCode);

		String description = ex.getErrorDescription();
		body.put("message", description);

		HttpStatus status;
		try {
			status = HttpStatus.valueOf(ex.getStatusCode());
		} catch (IllegalArgumentException e) {
			status = HttpStatus.BAD_REQUEST;
		}

		return ResponseEntity.status(status).body(body);
	}
}