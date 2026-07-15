package in.hridaykh.formbox.model.dto;

import lombok.Setter;

import java.util.List;
import java.util.Objects;

public final class TierValidationResult {
	private final FormSettingsRequest sanitizedRequest;
	private final List<String> warnings;
	@Setter
	private CachedForm updatedForm;

	// Canonical constructor
	public TierValidationResult(FormSettingsRequest sanitizedRequest, List<String> warnings, CachedForm updatedForm) {
		this.sanitizedRequest = sanitizedRequest;
		this.warnings = warnings;
		this.updatedForm = updatedForm;
	}

	public FormSettingsRequest sanitizedRequest() {
		return this.sanitizedRequest;
	}

	public List<String> warnings() {
		return this.warnings;
	}

	public CachedForm updatedForm() {
		return this.updatedForm;
	}

	// Custom helper methods from your original record
	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	public String getFirstWarning() {
		return warnings.isEmpty() ? "" : warnings.getFirst();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TierValidationResult that = (TierValidationResult) o;
		return Objects.equals(sanitizedRequest, that.sanitizedRequest) && Objects.equals(warnings, that.warnings) && Objects.equals(updatedForm, that.updatedForm);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sanitizedRequest, warnings, updatedForm);
	}

	@Override
	public String toString() {
		return "TierValidationResult[" + "sanitizedRequest=" + sanitizedRequest + ", warnings=" + warnings + ", updatedForm=" + updatedForm + ']';
	}
}