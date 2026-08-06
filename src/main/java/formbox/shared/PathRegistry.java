package formbox.shared;

public interface PathRegistry {

	String DASHBOARD = "/dashboard";

	interface Auth {
		String BASE = "/auth";
		String LOGIN = "/login";
		String SIGNUP = "/signup";
		String LOGOUT = "/logout";
		String RESEND_CONFIRMATION = "/resend-confirmation";
		String SESSION_CALLBACK = "/session-callback";
		String CALLBACK = "/callback";

		interface Hx {
			String LOGIN_UNAUTHORIZED = BASE + LOGIN + "?msg=unauthorized";
			String LOGIN_CHECK_EMAIL = BASE + LOGIN + "?msg=check_email";
			String LOGIN_LOGGED_OUT = BASE + LOGIN + "?msg=logged_out";
		}
	}

	interface Billing {
		String BASE = "/billing";
		String PORTAL = "/portal";
	}
}