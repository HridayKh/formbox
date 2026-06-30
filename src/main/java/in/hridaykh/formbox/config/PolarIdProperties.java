package in.hridaykh.formbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "polar-ids")
public class PolarIdProperties {
	private String paidProductId;
	private String submissionMeterId;

	public String getPaidProductId() {
		return paidProductId;
	}

	public void setPaidProductId(String paidProductId) {
		this.paidProductId = paidProductId;
	}

	public String getSubmissionMeterId() {
		return submissionMeterId;
	}

	public void setSubmissionMeterId(String submissionMeterId) {
		this.submissionMeterId = submissionMeterId;
	}
}