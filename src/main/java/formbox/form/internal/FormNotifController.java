package formbox.form.internal;

import formbox.auth.TenantApi;
import formbox.billing.EntitlementsApi;
import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.shared.Entitlements;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
class FormNotifController {

	private final FormApi formApi;
	private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});
	private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator(false, false, DomainValidator.getInstance(false));
	private final EntitlementsApi entitlementsApi;

	private final TenantApi tenantApi;

	@PostMapping("/{}/{formId}/notifs/discord")
	@WithSpan
	public String discordNotifs(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(required = false) String webhookUrl, @RequestParam(required = false) String body) {
		log.debug("Editing discord settings for form: {}", formId);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		FormDto form = formApi.getFormDto(formId);

		if (!URL_VALIDATOR.isValid(webhookUrl.strip()))
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Invalid discord url!";

		if (!tenantId.equals(form.tenantId())) {
			log.warn("tenant {} tried accessing discord notifs for form {}", tenantId, formId);
			return "redirect:/dashboard?msg=Invalid form";
		}

		var oldNotifs = form.formNotifs().toBuilder().discordWebhookUrl(webhookUrl.strip()).discordBody(body.strip());
		formApi.updateFormNotifs(formId, oldNotifs.build());

		return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=DiscordNotif settings updated successfully!";
	}

	@PostMapping("/{}/{formId}/notifs/email-autoresponder")
	@WithSpan
	public String autoresponder(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId,
	                            @RequestParam(required = false) String fieldName, @RequestParam(required = false) String body,
	                            @RequestParam(required = false) String replyTo, @RequestParam(required = false) String subjectLine) {
		log.debug("Editing autoresponder autoresponse settings for form: {}", formId);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		FormDto form = formApi.getFormDto(formId);

		if (!tenantId.equals(form.tenantId())) {
			log.warn("tenant {} tried accessing autoresponder for form {}", tenantId, formId);
			return "redirect:/dashboard?msg=Invalid form";
		}

		var oldNotifs = form.formNotifs().toBuilder();

		oldNotifs.autoresponderEmailFieldName(fieldName.strip());
		oldNotifs.autoresponderEmailBody(body.strip());
		oldNotifs.autoresponderReplyTo(replyTo.strip());
		oldNotifs.autoresponderSubjectLine(subjectLine.strip());

		formApi.updateFormNotifs(formId, oldNotifs.build());

		return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Email Autoresponder settings updated successfully!";
	}

	@PostMapping("/{}/{formId}/notifs/email-notifications")
	@WithSpan
	public String emailNotification(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId,
	                                @RequestParam(required = false) String to, @RequestParam(required = false) String cc,
	                                @RequestParam(required = false) String bcc) {
		log.debug("Editing email notification settings for form: {}", formId);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		FormDto form = formApi.getFormDto(formId);

		if (!tenantId.equals(form.tenantId())) {
			log.warn("tenant {} tried accessing email notifications for form {}", tenantId, formId);
			return "redirect:/dashboard?msg=Invalid form";
		}

		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);

		String toEmail = (to != null) ? to.strip() : "";
		List<String> ccEmail = (cc != null && !cc.isBlank()) ? Stream.of(cc.split(",")).map(String::strip).filter(e -> !e.isBlank()).toList() : List.of();
		List<String> bccEmail = (bcc != null && !bcc.isBlank()) ? Stream.of(bcc.split(",")).map(String::strip).filter(e -> !e.isBlank()).toList() : List.of();

		// Disallow setting CC or BCC if To is not set
		if (toEmail.isBlank()) {
			if (!ccEmail.isEmpty() || !bccEmail.isEmpty()) {
				return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Cannot set CC or BCC when 'To' email address is not set!";
			}
		} else {
			if (!EMAIL_VALIDATOR.isValid(toEmail))
				return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Invalid To Email Address!";

			List<String> verifiedEmails = tenantApi.getVerifiedEmails(tenantId);
			if (!verifiedEmails.contains(toEmail))
				return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Email address '" + toEmail + "' is not verified! Please verify it in Allowed Emails first.";
		}

		if (!ccEmail.isEmpty() && ccEmail.stream().anyMatch(e -> !EMAIL_VALIDATOR.isValid(e)))
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Invalid CC Email Address!";

		if (!bccEmail.isEmpty() && bccEmail.stream().anyMatch(e -> !EMAIL_VALIDATOR.isValid(e)))
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Invalid BCC Email Address!";

		int recipientCount = (toEmail.isBlank() ? 0 : 1) + ccEmail.size() + bccEmail.size();
		if (recipientCount > entitlements.maxEmailNotifRecipients())
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Too many email recipients (max for cc + bcc + to = " + entitlements.maxEmailNotifRecipients() + ")!";

		var oldNotifs = form.formNotifs().toBuilder();
		oldNotifs.emailNotifTo(toEmail);
		oldNotifs.emailNotifCc(ccEmail);
		oldNotifs.emailNotifBcc(bccEmail);

		formApi.updateFormNotifs(formId, oldNotifs.build());

		return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Email Notification settings updated successfully!";
	}

}