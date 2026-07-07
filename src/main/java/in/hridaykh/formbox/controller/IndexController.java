package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Submission;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import in.hridaykh.formbox.service.FormService;
import in.hridaykh.formbox.service.cache.SubmissionCacheService;
import in.hridaykh.formbox.service.polar.PolarCacheService;
import in.hridaykh.formbox.service.cache.TenantTierCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

	private final FormRepository formRepository;
	private final FormService formService;
	private final SubmissionRepository submissionRepository;
	private final PolarCacheService polarCacheService;
	private final TenantTierCacheService tenantTierCacheService;
	private final StringRedisTemplate stringRedisTemplate;

	@GetMapping("/")
	public String index(@RequestAttribute(required = false) JwtPayload userMetadata, Model model) {
		boolean isLoggedIn = userMetadata != null && userMetadata.getSub() != null;
		log.trace("Landing index page evaluated. Auth active status: {}", isLoggedIn);

		model.addAttribute("loggedIn", isLoggedIn);
		return ViewRegistry.INDEX;
	}

	@PostMapping(PathRegistry.WAITLIST)
	public String waitlist(@RequestParam String ignoredEmail) {
		try {
			log.error("WHO ENTERED THE WAITLIST BURH\nIncoming request to join product waitlist allocation for email placeholder: {}", ignoredEmail, new Exception("WAITLIST"));
		} catch (Exception _) {
		}
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping("/f/{formId}")
	public String submission(@PathVariable UUID formId, @RequestParam Map<String, String> payload, HttpServletRequest request) {
		log.debug("Processing incoming webhook submission request path channel for form ID: {}", formId);

		Form form = formRepository.findById(formId).orElse(null);
		if (form == null) {
			log.warn("Submission rejected. Form destination completely missing from structural records for ID: {}", formId);
			return "form-not-found";
		}

		var tenant = form.getTenant();
		long remainingSubmissions = polarCacheService.getCachedSubmissionBalance(tenant);
		log.trace("Evaluated submission request metrics for form ID: {} -> Tenant ID: {}, Remaining tokens: {}", formId, tenant.getId(), remainingSubmissions);

		if (remainingSubmissions <= 0) {
			log.warn("Submission blocked due to depleted usage bounds. Tenant target profile: {} is out of tokens.", tenant.getId());
			return "submit/out-of-submissions";
		}

		polarCacheService.decrementCachedSubmissionBalance(tenant);

		String clientIp = request.getRemoteAddr();
		String userAgent = request.getHeader("User-Agent");

		boolean isSpam = formService.checkIsSpam(form, payload, clientIp, userAgent);
		log.debug("Spam filter check evaluation completed for form ID {}. Spam detected classification status: {}", formId, isSpam);

		Submission submission = new Submission();
		submission.setForm(form);
		submission.setSenderIp(clientIp);
		submission.setPayload(payload);
		submission.setIsSpam(isSpam);

		submissionRepository.save(submission);
		log.info("Successfully persisted incoming submission record ID: {} for form context mapping: {}", submission.getId(), formId);

		try {
			stringRedisTemplate.delete(SubmissionCacheService.CACHE_KEY_BASE + formId);
			log.trace("Evicted submission aggregates collection buffer from Redis for form ID: {}", formId);
		} catch (Exception e) {
			log.error("Failed to invalidate group view cache string bindings for form ID matching key payload: {}", formId, e);
		}

		String redirectUrl = form.getRedirectUrl();
		String tenantTier = tenantTierCacheService.resolveHighestActiveTierNonNull(tenant.getId());

		if (redirectUrl == null || redirectUrl.isBlank() || "free-v1".equals(tenantTier)) {
			log.debug("Standard internal notification acknowledgment view served. Tier: {}, Redirect field configuration empty or restricted.", tenantTier);
			return "submit/thanks";
		}

		log.debug("Executing custom transaction redirection route parsing instructions. Forwarding response target thread to URL: {}", redirectUrl);
		return "redirect:" + redirectUrl;
	}
}