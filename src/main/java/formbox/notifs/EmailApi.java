package formbox.notifs;

import io.opentelemetry.instrumentation.annotations.WithSpan;

public interface EmailApi {
	@WithSpan
	ZeptoMailSuccessResponse sendGenericEmail(String to, String subject, String htmlBody, String fromName, String fromAddress);
}
