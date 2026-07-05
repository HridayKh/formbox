package in.hridaykh.formbox.exception;

import io.github.jan.supabase.auth.exception.AuthErrorCode;
import io.github.jan.supabase.auth.exception.AuthRestException;
import io.github.jan.supabase.auth.exception.TokenExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

	private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	public ModelAndView genericException(Exception ex) {
		log.error("Internal Server Error" ,ex);
		return buildErrorResponse("internal server error occurred", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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