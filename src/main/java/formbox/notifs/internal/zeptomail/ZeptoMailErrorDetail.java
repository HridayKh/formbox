package formbox.notifs.internal.zeptomail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single element of the "details" array in {@link ZeptoMailError}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeptoMailErrorDetail {

	/** Code of the specific error found, e.g. "TM_3301". */
	@JsonProperty("code")
	private String code;

	/** Reason for the error, e.g. "Invalid data". */
	@JsonProperty("message")
	private String message;

	/** The request field that caused the error, e.g. "email". */
	@JsonProperty("target")
	private String target;
}