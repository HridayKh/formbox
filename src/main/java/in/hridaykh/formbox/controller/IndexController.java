package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Submission;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import in.hridaykh.formbox.service.AuthService;
import in.hridaykh.formbox.service.FilterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;


@Controller
@RequestMapping(PathRegistry.ROOT)
public class IndexController {

	private final AuthService authService;
	private final FormRepository formRepository;
	private final FilterService filterService;
	private final SubmissionRepository submissionRepository;

	public IndexController(AuthService authService, FormRepository formRepository, FilterService filterService, SubmissionRepository submissionRepository) {
		this.authService = authService;
		this.formRepository = formRepository;
		this.filterService = filterService;
		this.submissionRepository = submissionRepository;
	}

	@GetMapping
	public String index(@CookieValue(name = "sb_token", required = false) String token, Model model) {
		model.addAttribute("loggedIn", token != null && !token.isBlank() && authService.isValidToken(token));
		return ViewRegistry.INDEX;
	}

	@PostMapping(PathRegistry.WAITLIST)
	public String waitlist(@RequestParam String ignoredEmail) {
		// TODO: store emails
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping("/{id}")
	public String submission(@PathVariable UUID id, @RequestParam Map<String, String> payload, HttpServletRequest request) {
		Form form = formRepository.findById(id).orElse(null);

		if (form == null)
			return "form-not-found";

		String clientIp = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");

		boolean isSpam = filterService.checkIsSpam(form, payload, clientIp, userAgent);

		Submission submission = new Submission(form, clientIp, payload, isSpam);

		submissionRepository.save(submission);

		return "redirect:" + form.getRedirectUrl();
	}
}
