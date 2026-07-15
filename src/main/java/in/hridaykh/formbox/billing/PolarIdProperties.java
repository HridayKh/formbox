package in.hridaykh.formbox.billing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "polar-ids")
@Data
public class PolarIdProperties {
	private String submissionMeterId;
}