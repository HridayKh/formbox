package formbox.submission;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.UUID;

public interface CsvExportApi {
	@WithSpan
	void generateAndUploadCsvExport(UUID tenantId, String userEmail, UUID formId);
}
