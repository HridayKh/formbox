package formbox.notifs.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
record ZeptoMailRequest(
	From from,
	List<Recipient> to,
	List<Recipient> cc,
	List<Recipient> bcc,
	@JsonProperty("reply_to") List<ReplyTo> replyTo,
	String subject,
	@JsonProperty("htmlbody") String htmlBody
) {
	record From(String address, String name) {}

	// to/cc/bcc all wrap address in an "email_address" object per ZeptoMail's API
	record Recipient(@JsonProperty("email_address") EmailAddress emailAddress) {
		static Recipient of(String address) {
			return new Recipient(new EmailAddress(address, null));
		}
	}

	record EmailAddress(String address, String name) {}

	// reply_to is flat - no email_address wrapper
	record ReplyTo(String address, String name) {
		static ReplyTo of(String address) {
			return new ReplyTo(address, null);
		}
	}
}