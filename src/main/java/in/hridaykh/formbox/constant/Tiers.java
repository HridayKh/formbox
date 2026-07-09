package in.hridaykh.formbox.constant;

import in.hridaykh.formbox.model.tier.TierFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

@Slf4j
public class Tiers {
	private static Map<String, TierFeatures> TIER_FEATURES_MAP;
	private static ConfigurableApplicationContext context;

	public static void initialize(Map<String, TierFeatures> map, ConfigurableApplicationContext appContext) {
		TIER_FEATURES_MAP = map;
		context = appContext;

		// make sure it works!
		Tiers.free();
		Tiers.starter();
		Tiers.pro();
	}

	public static TierFeatures free() {
		return load("free-v1");
	}

	public static TierFeatures starter() {
		return load("starter-v1");
	}

	public static TierFeatures pro() {
		return load("pro-v1");
	}

	public static boolean isFree(String plan) {
		return free().name().equalsIgnoreCase(plan);
	}

	public static boolean isStarter(String plan) {
		return starter().name().equalsIgnoreCase(plan);
	}

	public static boolean isPro(String plan) {
		return pro().name().equalsIgnoreCase(plan);
	}

	public static TierFeatures t(String tierName) {
		if (tierName.equalsIgnoreCase(starter().name()))
			return starter();
		if (tierName.equalsIgnoreCase(pro().name()))
			return pro();
		return free();
	}

	private static TierFeatures load(String tierName) {
		var tier = (TIER_FEATURES_MAP != null) ? TIER_FEATURES_MAP.get(tierName) : null;
		if (tier == null) {
			log.error("CRITICAL: '{}' configuration missing, maybe you forgot '-v1'?", tierName);
			int exitCode = SpringApplication.exit(context, () -> 1);
			System.exit(exitCode);
		}
		return tier;
	}
}
