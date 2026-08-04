package formbox.form.internal;

import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.form.FormNotifs;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
class FormNotifController {

	private final FormApi formApi;
	private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});
	private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator(false, false, DomainValidator.getInstance(false));

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

		var oldNotifs = form.formNotifs();
		formApi.updateFormNotifs(formId, new FormNotifs(webhookUrl.strip(), body.strip(),
			oldNotifs.autoresponderEmailFieldName(), oldNotifs.autoresponderEmailBody()
			, oldNotifs.autoresponderReplyTo(), oldNotifs.autoresponderSubjectLine()));

		return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=DiscordNotif settings updated successfully!";
	}

	@PostMapping("/{}/{formId}/notifs/email-autoresponder")
	@WithSpan
	public String email(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId,
	                    @RequestParam(required = false) String fieldName, @RequestParam(required = false) String body,
	                    @RequestParam(required = false) String replyTo, @RequestParam(required = false) String subjectLine) {
		log.debug("Editing email autoresponse settings for form: {}", formId);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		FormDto form = formApi.getFormDto(formId);

		if (!EMAIL_VALIDATOR.isValid(replyTo.strip()))
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Invalid Reply-To email!";

		if (!tenantId.equals(form.tenantId())) {
			log.warn("tenant {} tried accessing email autoresponse for form {}", tenantId, formId);
			return "redirect:/dashboard?msg=Invalid form";
		}

		var oldNotifs = form.formNotifs();
		formApi.updateFormNotifs(formId, new FormNotifs(oldNotifs.discordWebhookUrl(), oldNotifs.discordBody(),
			fieldName.strip(), body.strip(), replyTo.strip(), subjectLine.strip()));

		return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Email Autoresponder settings updated successfully!";
	}

}