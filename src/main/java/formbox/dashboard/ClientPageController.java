package formbox.dashboard;

import formbox.form.FormApi;
import formbox.shared.HmacSignerService;
import formbox.submission.FormSubmissionsResponse;
import formbox.submission.SubmissionApi;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
class ClientPageController {

	private final HmacSignerService hmacSignerService;
	private final FormApi formApi;
	private final SubmissionApi submissionApi;

	@GetMapping("/c/{formId}")
	@WithSpan
	public String renderLoginPage(@PathVariable UUID formId, Model model) {
		log.debug("Rendering login page for form: {}", formId);

		if(formApi.getFormDto(formId) == null){
			model.addAttribute("msg", "Invalid Form");
			return "client/client-page-login";
		}

		model.addAttribute("formId", formId);

		return "client/client-page-login";
	}

	@PostMapping("/c/{formId}")
	@WithSpan
	public String handleLoginAndRenderPage(@PathVariable UUID formId, @RequestParam String password, Model model) {
		log.debug("Authenticating access for form: {}", formId);

		if (!hmacSignerService.verify(formId.toString(), password)) {
			log.warn("Invalid signature attempt for form: {}", formId);
			model.addAttribute("msg", "Invalid signature or password");
			return "client/client-page-login";
		}

		FormSubmissionsResponse submissions = submissionApi.getFormSubmissionsGrouped(formId);
		model.addAttribute("validSubmissions", submissions.submissions().reversed());
		model.addAttribute("spamSubmissions", submissions.spam().reversed());
		return "client/client-page";
	}
}