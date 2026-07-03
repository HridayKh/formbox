package in.hridaykh.formbox.exception;

import io.github.jan.supabase.auth.exception.AuthErrorCode;
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import io.github.jan.supabase.auth.exception.AuthRestException;
import io.github.jan.supabase.auth.exception.TokenExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ModelAndView genericException(Exception ex) {
		return buildErrorResponse("internal server error occurred", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(AuthWeakPasswordException.class)
	public ModelAndView handleWeakPassword(AuthWeakPasswordException ex) {
		String errorDesc = ex.getDescription() != null ? ex.getDescription() : "Password must be at least 8 characters long and contain uppercase, lowercase, digits, and symbols.";
		if (!ex.getReasons().isEmpty()) {
			errorDesc += " Reasons: " + String.join(", ", ex.getReasons());
		}
		return buildErrorResponse("Weak Password", errorDesc, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(AuthRestException.class)
	public ModelAndView handleSupabaseAuthErrors(AuthRestException ex) {
		AuthErrorCode typedCode = ex.getErrorCode();
		String errorTitle = typedCode != null ? typedCode.name().replace("_", " ") : "Auth Error";
		String description = ex.getErrorDescription();
		HttpStatus status;
		try {
			status = HttpStatus.valueOf(ex.getStatusCode());
		} catch (IllegalArgumentException e) {
			status = HttpStatus.BAD_REQUEST;
		}
		return buildErrorResponse(errorTitle, description, status);
	}

	@ExceptionHandler(TokenExpiredException.class)
	public ModelAndView handleTokenExpired() {
		return buildErrorResponse("Session Expired", "Your session has expired. Please log in again.", HttpStatus.UNAUTHORIZED);
	}

	private ModelAndView buildErrorResponse(String errorTitle, String errorMessage, HttpStatus status) {
		ModelAndView mav = new ModelAndView("fragments/error-alert");
		mav.addObject("errorTitle", errorTitle);
		mav.addObject("errorMessage", errorMessage);
		mav.addObject("errorStatus", status);
		mav.setStatus(HttpStatus.OK);
		return mav;
	}
}