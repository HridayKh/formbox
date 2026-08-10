package formbox.notifs.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Properties(
	String endpointUrl,
	String accessKeyId,
	String accessKeySecret,
	String regionName,
	String attachmentsBucket
) {
}