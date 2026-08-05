package formbox.notifs.internal.zeptomail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The "error" object nested inside {@link ZeptoMailErrorResponse}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeptoMailError {

	/** The code corresponding to the status of the request made, e.g. "TM_3201". */
	@JsonProperty("code")
	private String code;

	/** List of granular error details, one per field/issue. */
	@JsonProperty("details")
	private List<ZeptoMailErrorDetail> details;

	/** The status/reason of the request made, e.g. "Invalid request". */
	@JsonProperty("message")
	private String message;
}