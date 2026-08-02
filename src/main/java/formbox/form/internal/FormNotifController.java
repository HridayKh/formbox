package formbox.form.internal;

import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.form.FormNotifs;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

	@PostMapping("/{}/{formId}/notifs/discord")
	@WithSpan
	public String discordNotifs(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID formId, @RequestParam(required = false) String webhookUrl, @RequestParam(required = false) String body) {
		log.debug("Editing discord settings for form: {}", formId);

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		FormDto form = formApi.getFormDto(formId);

		if (!tenantId.equals(form.tenantId())) {
			log.warn("tenant {} tried accessing form {}", tenantId, formId);
			return "redirect:/dashboard?msg=Invalid form";
		}

		if (webhookUrl == null || webhookUrl.isBlank() || body == null || body.isBlank() || !URL_VALIDATOR.isValid(webhookUrl.strip()))
			return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Empty or invalid discord url or body";

		formApi.updateFormNotifs(formId, new FormNotifs(webhookUrl.strip(), body.strip()));

		formApi.evictTenantForms(tenantId);

		return "redirect:/forms/" + form.folderId() + "/" + formId + "?msg=Discord settings updated successfully!";
	}

}