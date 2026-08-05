package formbox.notifs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single element of the "data" array in {@link ZeptoMailSuccessResponse}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeptoMailResponseData {

	/** The code corresponding to a success response, e.g. "EM_104". */
	@JsonProperty("code")
	private String code;

	/** Additional information about the action performed in the request. */
	@JsonProperty("additional_info")
	private List<Object> additionalInfo;

	/** The action taken for this request, e.g. "OK". */
	@JsonProperty("message")
	private String message;
}