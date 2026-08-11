package formbox.submission.internal;

import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.notifs.EmailApi;
import formbox.notifs.UploadService;
import formbox.submission.CsvExportApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import formbox.submission.SubmissionItem;

@Service
@RequiredArgsConstructor
@Slf4j
class CsvExportServiceImpl implements CsvExportApi {

	private final SubmissionRepository submissionRepository;
	private final FormApi formApi;
	private final UploadService uploadService;
	private final EmailApi emailApi;

	@Async
	@WithSpan
	@Override
	public void generateAndUploadCsvExport(UUID tenantId, String userEmail, UUID formId) {
		log.info("Starting async CSV export job for tenant ID: {}", tenantId);
		try {
			FormDto form = formApi.getFormDto(formId);
			if (form == null) {
				log.warn("Form not found for CSV export: {}", formId);
				return;
			}

			List<SubmissionItem> submissions = submissionRepository.findAllByFormIdOrderByCreatedAtDesc(formId);

			byte[] csvBytes = buildCsv(submissions);

			String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(OffsetDateTime.now());
			String fileName = timestamp + "-" + UUID.randomUUID() + ".csv";

			String downloadUrl = uploadService.uploadExportCsv(formId, csvBytes, fileName);

			log.info("CSV export completed and uploaded to S3 for tenant ID: {}", tenantId);

			if (userEmail != null && !userEmail.isBlank()) {
				String subject = "Your CSV export for form '" + form.name() + "' is ready!";
				String htmlBody = "<h2>Formbox CSV Export Ready</h2>"
					+ "<p>Your CSV export for form <strong>" + form.name() + "</strong> has been generated successfully.</p>"
					+ "<p><a href=\"" + downloadUrl + "\">Click here to download your CSV export</a></p>"
					+ "<p><small>Download link: " + downloadUrl + "</small></p>"
					+ "<br/><p>-- Formbox</p>";

				emailApi.sendGenericEmail(userEmail.strip(), subject, htmlBody, "Formbox", "no-reply");
			}
		} catch (Exception e) {
			log.error("Failed to generate CSV export for tenant ID: {}", tenantId, e);
		}
	}

	private byte[] buildCsv(List<SubmissionItem> submissions) {
		if (submissions == null || submissions.isEmpty()) {
			return "id,created_at,is_spam\n".getBytes(StandardCharsets.UTF_8);
		}

		Set<String> payloadKeys = new LinkedHashSet<>();
		for (SubmissionItem s : submissions) {
			if (s.payload() != null) {
				payloadKeys.addAll(s.payload().keySet());
			}
		}
		List<String> keys = new ArrayList<>(payloadKeys);
		Collections.sort(keys);

		StringBuilder sb = new StringBuilder();

		// Header row
		sb.append("id,created_at,is_spam");
		for (String key : keys) {
			sb.append(",").append(escapeCsv(key));
		}
		sb.append("\n");

		// Data rows
		for (SubmissionItem s : submissions) {
			sb.append(escapeCsv(s.id().toString())).append(",");
			sb.append(escapeCsv(s.createdAt() != null ? s.createdAt().toString() : "")).append(",");
			sb.append(s.isSpam() ? "true" : "false");

			Map<String, String> payload = s.payload() != null ? s.payload() : Map.of();
			for (String key : keys) {
				sb.append(",").append(escapeCsv(payload.getOrDefault(key, "")));
			}
			sb.append("\n");
		}

		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String escapeCsv(String input) {
		if (input == null) return "";
		String str = input.strip();
		if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
			return "\"" + str.replace("\"", "\"\"") + "\"";
		}
		return str;
	}
}
