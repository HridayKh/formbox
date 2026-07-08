package in.hridaykh.formbox.model.tier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public record TierConfiguration(
	@JsonValue
	Map<String, TierFeatures> tiers
) {
	@JsonCreator
	public static TierConfiguration fromJson(Map<String, TierFeatures> tiers) {
		return new TierConfiguration(tiers);
	}
}