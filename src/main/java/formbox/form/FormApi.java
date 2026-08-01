package formbox.form;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface FormApi {
	@Transactional(readOnly = true)
	@WithSpan
	FormDto getCachedForm(UUID formId);

	@WithSpan
	void updateFormCache(FormDto updatedForm);

	@WithSpan
	void evictFormCache(UUID formId);

	@WithSpan
	List<FormDto> getTenantForms(UUID tenantId);

	@WithSpan
	void evictTenantForms(UUID tenantId);
}
