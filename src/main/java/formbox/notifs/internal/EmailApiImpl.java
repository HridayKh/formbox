package formbox.notifs.internal;

import formbox.notifs.EmailApi;
import formbox.notifs.ZeptoMailSuccessResponse;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
class EmailApiImpl implements EmailApi {

	private final ZeptomailService zeptomailService;

	@WithSpan
	@Override
	public ZeptoMailSuccessResponse sendGenericEmail(String to, String subject, String htmlBody, String fromName, String fromAddress) {
		log.info("Sending generic email with subject: {}", subject);
		return zeptomailService.sendEmail(to, List.of(), List.of(), List.of(), subject, htmlBody, fromName, fromAddress);
	}
}
