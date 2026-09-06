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

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

	private final S3Properties s3Props;
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

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
			.toString();
	}

	public String generatePresignedUrl(String fileUrlOrKey) {
		if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) return fileUrlOrKey;
		try {
			String bucket = s3Props.attachmentsBucket();
			String bucketToken = "/" + bucket + "/";
			String s3Key;
			if (fileUrlOrKey.contains(bucketToken)) {
				s3Key = fileUrlOrKey.substring(fileUrlOrKey.indexOf(bucketToken) + bucketToken.length());
			} else if (fileUrlOrKey.contains("uploads/")) {
				s3Key = fileUrlOrKey.substring(fileUrlOrKey.indexOf("uploads/"));
			} else if (fileUrlOrKey.contains("attachments/")) {
				s3Key = fileUrlOrKey.substring(fileUrlOrKey.indexOf("attachments/"));
			} else if (fileUrlOrKey.contains("exports/")) {
				s3Key = fileUrlOrKey.substring(fileUrlOrKey.indexOf("exports/"));
			} else {
				s3Key = fileUrlOrKey;
			}

			if (s3Key.contains("?")) {
				s3Key = s3Key.substring(0, s3Key.indexOf("?"));
			}

			software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest =
				software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.build();

			software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest =
				software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
					.signatureDuration(java.time.Duration.ofHours(1))
					.getObjectRequest(getObjectRequest)
					.build();

			return s3Presigner.presignGetObject(presignRequest).url().toString();
		} catch (Exception e) {
			log.error("Failed to generate presigned URL for file: {}", fileUrlOrKey, e);
			return fileUrlOrKey;
		}
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

	public void deleteCsvExport(UUID formId, String fileName) {
		if (formId == null || fileName == null || fileName.isBlank()) return;
		try {
			String bucket = s3Props.attachmentsBucket();
			String s3Key = "exports/" + formId + "/" + fileName;

			software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteObjectRequest =
				software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.build();

			s3Client.deleteObject(deleteObjectRequest);
			log.info("Deleted CSV export file: {} for form ID: {}", s3Key, formId);
		} catch (Exception e) {
			log.error("Failed to delete CSV export file: {} for form ID: {}", fileName, formId, e);
		}
	}

	public void deleteAllCsvExports(UUID formId) {
		if (formId == null) return;
		try {
			String bucket = s3Props.attachmentsBucket();
			String prefix = "exports/" + formId + "/";

			software.amazon.awssdk.services.s3.model.ListObjectsV2Request listReq =
				software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
					.bucket(bucket)
					.prefix(prefix)
					.build();

			software.amazon.awssdk.services.s3.model.ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
			for (software.amazon.awssdk.services.s3.model.S3Object s3Object : listRes.contents()) {
				s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(s3Object.key())
					.build());
			}
			log.info("Successfully deleted all S3 CSV exports for form ID: {}", formId);
		} catch (Exception e) {
			log.error("Failed to delete S3 CSV exports for form ID: {}", formId, e);
		}
	}

	public String uploadExportCsv(UUID formId, byte[] csvBytes, String fileName) {
		String s3Key = "exports/" + formId + "/" + fileName;

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Props.attachmentsBucket())
			.key(s3Key)
			.contentType("text/csv")
			.contentLength((long) csvBytes.length)
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromBytes(csvBytes));

		return generatePresignedUrl(s3Key);
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
				String url = generatePresignedUrl(key);

				items.add(new CsvExportItem(fileName, url, s3Object.lastModified()));
			}

			items.sort(java.util.Comparator.comparing(CsvExportItem::createdAt).reversed());
			return items;
		} catch (Exception e) {
			log.error("Failed to list CSV exports for form ID: {}", formId, e);
			return java.util.List.of();
		}
	}

	public void cleanupExpiredCsvExports(int maxAgeDays) {
		try {
			String bucket = s3Props.attachmentsBucket();
			String prefix = "exports/";

			software.amazon.awssdk.services.s3.model.ListObjectsV2Request listReq =
				software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
					.bucket(bucket)
					.prefix(prefix)
					.build();

			software.amazon.awssdk.services.s3.model.ListObjectsV2Response listRes;
			java.time.Instant cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(maxAgeDays));
			int deletedCount = 0;

			do {
				listRes = s3Client.listObjectsV2(listReq);
				for (software.amazon.awssdk.services.s3.model.S3Object s3Object : listRes.contents()) {
					if (s3Object.lastModified() != null && s3Object.lastModified().isBefore(cutoff)) {
						s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
							.bucket(bucket)
							.key(s3Object.key())
							.build());
						deletedCount++;
					}
				}
				if (listRes.nextContinuationToken() != null) {
					listReq = listReq.toBuilder().continuationToken(listRes.nextContinuationToken()).build();
				}
			} while (Boolean.TRUE.equals(listRes.isTruncated()));

			log.info("Cleaned up {} expired CSV export files older than {} days", deletedCount, maxAgeDays);
		} catch (Exception e) {
			log.error("Failed to clean up expired CSV exports from S3", e);
		}
	}
}