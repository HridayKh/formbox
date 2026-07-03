package in.hridaykh.formbox.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllowedField implements Serializable {

	private static final long serialVersionUID = 1L;

	public String name;
	public String label;
	public FieldType type;
	public boolean required;
	public ValidationRules validation;
	public List<String> options;

	public enum FieldType {
		text, email, password, number, textarea, select, checkbox, radio, tel, url, date, file
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AllowedField that = (AllowedField) o;
		return required == that.required && Objects.equals(name, that.name) && Objects.equals(label, that.label) && type == that.type && Objects.equals(validation, that.validation) && Objects.equals(options, that.options);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, label, type, required, validation, options);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ValidationRules implements Serializable {

		private static final long serialVersionUID = 1L;

		@JsonProperty("min_length")
		public Integer minLength;

		@JsonProperty("max_length")
		public Integer maxLength;

		public String pattern;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ValidationRules that = (ValidationRules) o;
			return Objects.equals(minLength, that.minLength) && Objects.equals(maxLength, that.maxLength) && Objects.equals(pattern, that.pattern);
		}

		@Override
		public int hashCode() {
			return Objects.hash(minLength, maxLength, pattern);
		}
	}
}