package in.hridaykh.formbox.exception;

import io.github.jan.supabase.auth.exception.AuthErrorCode;
import io.github.jan.supabase.auth.exception.AuthRestException;
import io.github.jan.supabase.auth.exception.TokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ModelAndView genericException(Exception ex) {
		log.error("Unhandled system exception caught: {}", ex.getMessage(), ex);
		return buildErrorResponse("internal server error occurred", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ModelAndView handle404Error(NoResourceFoundException ex) {
		log.debug("Resource Not Found: {}", ex.getMessage());
		return buildErrorResponse("", ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(FormNotFoundException.class)
	public ModelAndView handle404FormError(FormNotFoundException ex) {
		log.debug("Form Not Found: {}", ex.getMessage());
		return buildErrorResponse("", ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ModelAndView handle40Error(HttpRequestMethodNotSupportedException ex) {
		log.info("Invalid Request Method: {}", ex.getMethod());
		return buildErrorResponse("", ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(MultipartException.class)
	public ModelAndView handleMultipartException(MultipartException ex) {
		Throwable rootCause = ex.getRootCause();
		if (rootCause instanceof java.io.EOFException || ex.getMessage().contains("parse multipart")) {
			log.info("Client disconnected or sent malformed data during multipart upload: {}", ex.getMessage());
			return buildErrorResponse("Incomplete multipart request", "", HttpStatus.BAD_REQUEST);
		}

		log.info("Multipart exception encountered: ", ex);
		return buildErrorResponse("Malformed request", "", HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(AuthRestException.class)
	public ModelAndView handleSupabaseAuthErrors(AuthRestException ex) {
		log.debug("Supabase AuthRestException occurred", ex);
		AuthErrorCode typedCode = ex.getErrorCode();
		String errorTitle = typedCode != null ? typedCode.name().replace("_", " ") : "Auth Error";
		String description = ex.getErrorDescription();
		HttpStatus status;
		try {
			status = HttpStatus.valueOf(ex.getStatusCode());
		} catch (IllegalArgumentException e) {
			log.warn("Invalid HTTP status code received from Supabase Auth: {}", ex.getStatusCode());
			status = HttpStatus.BAD_REQUEST;
		}

		log.debug("Supabase auth exception processed. Title: [{}], Status: [{}]", errorTitle, status);
		return buildErrorResponse(errorTitle, description, status);
	}

	@ExceptionHandler(TokenExpiredException.class)
	public ModelAndView handleTokenExpired() {
		log.debug("TokenExpiredException caught. User session has expired.");
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