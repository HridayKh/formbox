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
			String EMPTY = BASE + " :: empty-frag";
			String ERROR_ALERT = BASE + " :: error-alert";
			String SUCCESS_ALERT = BASE + " :: success-alert";
		}
	}

	interface Fragments {
		String FORM_ROWS = "fragments/form-list :: form-rows";
		String SETTINGS = "dashboard/manage-form :: settings-panel";
	}
}