package in.hridaykh.formbox.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllowedField {

	private String name;
	private String label;
	private FieldType type;
	private boolean required;
	private ValidationRules validation;
	private List<String> options;

	public enum FieldType {
		text, email, password, number, textarea, select, checkbox, radio, tel, url, date, file
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ValidationRules {
		@JsonProperty("min_length")
		private Integer minLength;

		@JsonProperty("max_length")
		private Integer maxLength;

		private String pattern;
	}

}