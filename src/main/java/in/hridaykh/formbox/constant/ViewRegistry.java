package in.hridaykh.formbox.constant;

public interface ViewRegistry {
	String INDEX = "index";
	String DASHBOARD = "dashboard";

	interface Auth {
		String LOGIN = "auth/login";
		String REGISTER = "auth/register";
		String CALLBACK = "auth/callback";

		interface Fragments {
			String BASE = "auth/fragments";
			String EMPTY = BASE + " :: empty";
			String ERROR_ALERT = BASE + " :: error-alert";
			String SUCCESS_ALERT = BASE + " :: success-alert";
		}
	}

	interface Dashboard {
		interface Fragments {
			String SUBSCRIPTION_CARD_INNER = ViewRegistry.DASHBOARD + " :: subscriptionCardInner";
		}
	}
}