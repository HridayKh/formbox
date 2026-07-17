package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.billing.service.PolarCacheService;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.DASHBOARD)
@Slf4j
public class DashboardController {

	private final EntitlementsCacheService entitlementsCacheService;
	private final PolarCacheService polarCacheService;

	public DashboardController(EntitlementsCacheService entitlementsCacheService, PolarCacheService polarCacheService) {
		this.entitlementsCacheService = entitlementsCacheService;
		this.polarCacheService = polarCacheService;
	}

	@GetMapping
	@WithSpan
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, @RequestParam(required = false) String msg, Model model) {
		log.trace("Initiating dashboard view generation request routing context.");

		if (userMetadata == null || userMetadata.getSub() == null) {
			log.warn("Dashboard interception access denial rule triggered. User metadata context is completely unauthenticated.");
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		Entitlements entitlements = entitlementsCacheService.getEntitlements(tenantId);
		log.debug("Resolved entitlements for Tenant ID: {}, Service Tier: {}", tenantId, entitlements.tierName());

		boolean isFreeTier = entitlements.isFree();
		model.addAttribute("showUpgradeCta", true); // always show — portal handles plan selection
		model.addAttribute("showManageSubscription", !isFreeTier);

		model.addAttribute("redirectUrlNotAllowed", !entitlements.redirectUrlsAllowed());
		model.addAttribute("user", userMetadata);
		model.addAttribute("msg", msg);
		model.addAttribute("balanceLeft", polarCacheService.getCachedSubmissionBalance(tenantId));

		if (customerSessionToken != null && !customerSessionToken.isBlank()) {
			log.debug("Detected customer_session_token parameter; redirecting to dashboard without query string.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}

		return ViewRegistry.DASHBOARD;
	}

}