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
}