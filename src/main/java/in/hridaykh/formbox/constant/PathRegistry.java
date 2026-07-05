package in.hridaykh.formbox.constant;

public interface PathRegistry {

	String DASHBOARD = "/dashboard";
	String WAITLIST = "/api/waitlist";

	interface Form {
		String BASE = "/forms";

	}

	interface Auth {
		String BASE = "/auth";
		String LOGIN = "/login";
		String SIGNUP = "/signup";
		String LOGOUT = "/logout";
		String RESEND_CONFIRMATION = "/resend-confirmation";
		String SESSION_CALLBACK = "/session-callback";
		String CALLBACK = "/callback";

		interface Redirects {
			String TO_LOGIN_UNAUTHORIZED = "redirect:" + Hx.LOGIN_UNAUTHORIZED;
		}

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

	interface Webhooks {
		String POLAR = "/polar";
	}
}