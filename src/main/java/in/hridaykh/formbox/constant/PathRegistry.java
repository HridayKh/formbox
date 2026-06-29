package in.hridaykh.formbox.constant;

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

		interface Redirects {
			String TO_LOGIN_UNAUTHORIZED = "redirect:" + BASE + LOGIN + "?msg=unauthorized";
		}

		interface Hx {
			String LOGIN_CHECK_EMAIL = BASE + LOGIN + "?msg=check_email";
			String LOGIN_LOGGED_OUT = BASE + LOGIN + "?msg=logged_out";
			String DASHBOARD = PathRegistry.DASHBOARD;
		}
	}

	interface Billing {
		String BASE = "/billing";
		String UPGRADE = "/upgrade";
		String PORTAL = "/portal";
	}

	interface Dashboard {
		String BILLING_STATUS = "/billing-status";
	}

	// --- Added Webhooks Section ---
	interface Webhooks {
		String POLAR = "/polar";
	}
}