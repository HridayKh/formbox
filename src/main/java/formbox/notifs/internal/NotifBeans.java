package formbox.notifs.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@Slf4j
public class NotifBeans {

	@Bean
	public S3Client getS3Client(S3Properties s3Properties) {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(
			s3Properties.accessKeyId(),
			s3Properties.accessKeySecret()
		);

		return S3Client.builder()
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.region(Region.of(s3Properties.regionName()))
			.endpointOverride(URI.create(s3Properties.endpointUrl()))
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.forcePathStyle(true)
			.build();
	}
}
