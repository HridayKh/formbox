package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Submission;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.PurchasesRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import in.hridaykh.formbox.service.FilterService;
import in.hridaykh.formbox.service.PolarCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;


@Controller
public class IndexController {

	private final FormRepository formRepository;
	private final FilterService filterService;
	private final SubmissionRepository submissionRepository;
	private final PurchasesRepository purchasesRepository;
	private final PolarCacheService polarCacheService;

	public IndexController(FormRepository formRepository, FilterService filterService, SubmissionRepository submissionRepository, PurchasesRepository purchasesRepository, PolarCacheService polarCacheService) {
		this.formRepository = formRepository;
		this.filterService = filterService;
		this.submissionRepository = submissionRepository;
		this.purchasesRepository = purchasesRepository;
		this.polarCacheService = polarCacheService;
	}

	@GetMapping("/")
	public String index(@RequestAttribute(required = false) JwtPayload userMetadata, Model model) {
		model.addAttribute("loggedIn", userMetadata != null && userMetadata.getSub() != null);
		return ViewRegistry.INDEX;
	}

	@PostMapping(PathRegistry.WAITLIST)
	public String waitlist(@RequestParam String ignoredEmail) {
		// TODO: store emails
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping("/f/{id}")
	public String submission(@PathVariable UUID id, @RequestParam Map<String, String> payload, HttpServletRequest request) {
		Form form = formRepository.findById(id).orElse(null);
		if (form == null)
			return "form-not-found";
		var tenant = form.getTenant();
		long remainingSubmissions = polarCacheService.getCachedSubmissionBalance(tenant);

		if (remainingSubmissions <= 0)
			return "submit/out-of-submissions";

		polarCacheService.decrementCachedSubmissionBalance(tenant);

		String clientIp = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");

		boolean isSpam = filterService.checkIsSpam(form, payload, clientIp, userAgent);

		Submission submission = new Submission();
		submission.setForm(form);
		submission.setSenderIp(clientIp);
		submission.setPayload(payload);
		submission.setIsSpam(isSpam);
		submissionRepository.save(submission);

		String redirectUrl = form.getRedirectUrl();
		String tenantTier = tenant.resolveHighestActiveTier(purchasesRepository);

		if (redirectUrl == null || redirectUrl.isBlank() || "free-v1".equals(tenantTier))
			return "submit/thanks";

		return "redirect:" + redirectUrl;
	}
}
