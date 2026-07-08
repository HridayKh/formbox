package in.hridaykh.formbox.model.tier;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationFeatures(
	@JsonProperty("discord") boolean discord,
	@JsonProperty("slack") boolean slack,
	@JsonProperty("telegram") boolean telegram
) {}