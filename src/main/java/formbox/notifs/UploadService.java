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
}