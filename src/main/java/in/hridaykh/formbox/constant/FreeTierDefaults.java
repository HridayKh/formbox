package in.hridaykh.formbox.constant;

/**
 * Default entitlement values for the free tier.
 * Used as fallback when a tenant has no Polar subscription
 * or no granted benefits from Polar.
 */
public final class FreeTierDefaults {

	public static final String TIER_NAME = "free";
	public static final int TIER_PRIORITY = 0;

	// Meter limits
	public static final long SUBMISSIONS_LIMIT = 100;
	public static final long FORMS_LIMIT = 3;
	public static final long STORAGE_LIMIT_BYTES = 0;

	// Numeric limits
	public static final int MAX_RATE_LIMIT_RPM = 10;
	public static final long MAX_FILE_SIZE_BYTES = 0;

	// All boolean feature flags default to false for the free tier

	private FreeTierDefaults() {
	}
}
