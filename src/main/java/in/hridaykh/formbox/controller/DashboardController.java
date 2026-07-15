package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.billing.service.PolarCacheService;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.Tiers;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.cache.TenantCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.DASHBOARD)
@Slf4j
public class DashboardController {

	private final TenantCacheService tenantCacheService;
	private final PolarCacheService polarCacheService;

	public DashboardController(TenantCacheService tenantCacheService, PolarCacheService polarCacheService) {
		this.tenantCacheService = tenantCacheService;
		this.polarCacheService = polarCacheService;
	}

	@GetMapping
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, @RequestParam(required = false) String msg, Model model) {
		log.trace("Initiating dashboard view generation request routing context.");

		if (userMetadata == null || userMetadata.getSub() == null) {
			log.warn("Dashboard interception access denial rule triggered. User metadata context is completely unauthenticated.");
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		String activeTier = tenantCacheService.resolveHighestActiveTierNonNull(tenantId);
		log.debug("Resolved active workspace references for Tenant ID: {}, Service Tier: {}", tenantId, activeTier);

		boolean isFreeTier = Tiers.isFree(activeTier);
		boolean isStarterTier = Tiers.isStarter(activeTier);
		model.addAttribute("showUpgradeStarter", isFreeTier);
		model.addAttribute("showUpgradePro", isFreeTier || isStarterTier);
		model.addAttribute("showManageSubscription", !isFreeTier);

		model.addAttribute("redirectUrlNotAllowed", !Tiers.t(activeTier).redirectUrlAllowed());
		model.addAttribute("user", userMetadata);
		model.addAttribute("msg", msg);
		model.addAttribute("balanceLeft", polarCacheService.getCachedSubmissionBalance(tenantId));

		if (customerSessionToken != null && !customerSessionToken.isBlank()) {
			log.info("Detected Polar query string authentication fallback parameter payload. Cleaning execution URL state via dynamic client-side refresh redirect loop.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}

		return ViewRegistry.DASHBOARD;
	}

}