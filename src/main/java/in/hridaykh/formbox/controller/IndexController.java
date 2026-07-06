package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Submission;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import in.hridaykh.formbox.service.FormService;
import in.hridaykh.formbox.service.cache.SubmissionCacheService;
import in.hridaykh.formbox.service.cache.PolarCacheService;
import in.hridaykh.formbox.service.TenantTierService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
public class IndexController {

	private final FormRepository formRepository;
	private final FormService formService;
	private final SubmissionRepository submissionRepository;
	private final PolarCacheService polarCacheService;
	private final TenantTierService tenantTierService;
	private final StringRedisTemplate stringRedisTemplate;

	public IndexController(FormRepository formRepository, FormService formService, SubmissionRepository submissionRepository, PolarCacheService polarCacheService, TenantTierService tenantTierService, StringRedisTemplate stringRedisTemplate) {
		this.formRepository = formRepository;
		this.formService = formService;
		this.submissionRepository = submissionRepository;
		this.polarCacheService = polarCacheService;
		this.tenantTierService = tenantTierService;
		this.stringRedisTemplate = stringRedisTemplate;
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

	@PostMapping("/f/{formId}")
	public String submission(@PathVariable UUID formId, @RequestParam Map<String, String> payload, HttpServletRequest request) {
		Form form = formRepository.findById(formId).orElse(null);
		if (form == null) return "form-not-found";
		var tenant = form.getTenant();
		long remainingSubmissions = polarCacheService.getCachedSubmissionBalance(tenant);

		if (remainingSubmissions <= 0) return "submit/out-of-submissions";

		polarCacheService.decrementCachedSubmissionBalance(tenant);

		String clientIp = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");

		boolean isSpam = formService.checkIsSpam(form, payload, clientIp, userAgent);

		Submission submission = new Submission();
		submission.setForm(form);
		submission.setSenderIp(clientIp);
		submission.setPayload(payload);
		submission.setIsSpam(isSpam);
		submissionRepository.save(submission);

		stringRedisTemplate.delete(SubmissionCacheService.CACHE_KEY_BASE + formId);

		String redirectUrl = form.getRedirectUrl();
		String tenantTier = tenantTierService.resolveHighestActiveTierNonNull(tenant.getId());

		if (redirectUrl == null || redirectUrl.isBlank() || tenantTier == null || "free-v1".equals(tenantTier))
			return "submit/thanks";

		return "redirect:" + redirectUrl;
	}
}
