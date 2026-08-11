package formbox.notifs;

import formbox.notifs.internal.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

	private final S3Properties s3Props;
	private final S3Client s3Client;

	public String uploadFile(InputStream is, String fileName, long size, String contentType) {
		String s3Key = "uploads/" + UUID.randomUUID() + "/" + fileName;

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Props.attachmentsBucket())
			.key(s3Key)
			.contentType(contentType)
			.contentLength(size)
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is, size));

		return s3Client.utilities()
			.getUrl(GetUrlRequest.builder().bucket(s3Props.attachmentsBucket()).key(s3Key).build())
			.toString().replace("s3.hridaykh.in", "web-s3.hridaykh.in");
	}

	public void deleteFileByUrl(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank()) return;
		try {
			String bucket = s3Props.attachmentsBucket();
			String bucketToken = "/" + bucket + "/";
			String s3Key;
			if (fileUrl.contains(bucketToken)) {
				s3Key = fileUrl.substring(fileUrl.indexOf(bucketToken) + bucketToken.length());
			} else if (fileUrl.contains("/uploads/")) {
				s3Key = fileUrl.substring(fileUrl.indexOf("uploads/"));
			} else if (fileUrl.contains("/attachments/")) {
				s3Key = fileUrl.substring(fileUrl.indexOf("attachments/"));
			} else {
				log.warn("Could not extract S3 key from file URL");
				return;
			}

			software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteObjectRequest =
				software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.build();

			s3Client.deleteObject(deleteObjectRequest);
			log.info("Deleted S3 object with key: {} from bucket: {}", s3Key, bucket);
		} catch (Exception e) {
			log.error("Failed to delete S3 object for file URL", e);
		}
	}

	public record CsvExportItem(String fileName, String downloadUrl, java.time.Instant createdAt) {}

	public String uploadExportCsv(UUID formId, byte[] csvBytes, String fileName) {
		String s3Key = "exports/" + formId + "/" + fileName;

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Props.attachmentsBucket())
			.key(s3Key)
			.contentType("text/csv")
			.contentLength((long) csvBytes.length)
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromBytes(csvBytes));

		return s3Client.utilities()
			.getUrl(GetUrlRequest.builder().bucket(s3Props.attachmentsBucket()).key(s3Key).build())
			.toString().replace("s3.hridaykh.in", "web-s3.hridaykh.in");
	}

	public java.util.List<CsvExportItem> listCsvExports(UUID formId) {
		if (formId == null) return java.util.List.of();
		try {
			String bucket = s3Props.attachmentsBucket();
			String prefix = "exports/" + formId + "/";

			software.amazon.awssdk.services.s3.model.ListObjectsV2Request request =
				software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
					.bucket(bucket)
					.prefix(prefix)
					.build();

			software.amazon.awssdk.services.s3.model.ListObjectsV2Response response = s3Client.listObjectsV2(request);

			java.util.List<CsvExportItem> items = new java.util.ArrayList<>();
			for (software.amazon.awssdk.services.s3.model.S3Object s3Object : response.contents()) {
				String key = s3Object.key();
				String fileName = key.substring(key.lastIndexOf('/') + 1);
				String url = s3Client.utilities()
					.getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build())
					.toString().replace("s3.hridaykh.in", "web-s3.hridaykh.in");

				items.add(new CsvExportItem(fileName, url, s3Object.lastModified()));
			}

			items.sort(java.util.Comparator.comparing(CsvExportItem::createdAt).reversed());
			return items;
		} catch (Exception e) {
			log.error("Failed to list CSV exports for form ID: {}", formId, e);
			return java.util.List.of();
		}
	}
}