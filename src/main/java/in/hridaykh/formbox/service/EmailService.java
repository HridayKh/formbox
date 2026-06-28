package in.hridaykh.formbox.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Email;
import in.hridaykh.formbox.config.ResendProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {
	private final ResendProperties resendProps;

	public EmailService(ResendProperties resendProps) {
		this.resendProps = resendProps;
	}

	public String listEmails() {
		Resend resend = new Resend(resendProps.getApiKey());
		try {
			List<Email> listEmails = resend.emails().list().getData();
			StringBuilder out = new StringBuilder();
			for (Email summary : listEmails)
				out.append(resend.emails().get(summary.getId()).getHtml()).append("\n\n\n\n\ne\n\n\n\n\n");
			return out.toString();
		} catch (ResendException e) {
			e.printStackTrace();
			return "unable to list emails";
		}
	}
}
