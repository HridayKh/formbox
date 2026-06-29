package in.hridaykh.formbox.constant;

public interface PathRegistry {

	// Base roots
	String ROOT = "/";
	String DASHBOARD = "/dashboard";

	interface Auth {
		String BASE = "/auth";

		// Endpoint Mappings (Must be constants for annotations)
		String LOGIN = "/login";
		String SIGNUP = "/signup";
		String LOGOUT = "/logout";
		String RESEND_CONFIRMATION = "/resend-confirmation";
		String SESSION_CALLBACK = "/session-callback";
		String CALLBACK = "/callback";

		// Full paths for absolute redirects
		interface Redirects {
			String TO_LOGIN_CHECK_EMAIL = "redirect:" + BASE + LOGIN + "?msg=check_email";
			String TO_LOGIN_LOGBED_OUT = "redirect:" + BASE + LOGIN + "?msg=logged_out";
			String TO_LOGIN_UNAUTHORIZED = "redirect:" + BASE + LOGIN + "?msg=unauthorized";
			String TO_LOGIN_SESSION_EXPIRED = "redirect:" + BASE + LOGIN + "?msg=session_expired";
			String TO_DASHBOARD = "redirect:" + DASHBOARD;
		}

		// HX-Redirect raw paths (HTMX expects pure paths, not the "redirect:" prefix)
		interface Hx {
			String LOGIN_CHECK_EMAIL = BASE + LOGIN + "?msg=check_email";
			String LOGIN_LOGGED_OUT = BASE + LOGIN + "?msg=logged_out";
			String DASHBOARD = PathRegistry.DASHBOARD;
		}
	}
}