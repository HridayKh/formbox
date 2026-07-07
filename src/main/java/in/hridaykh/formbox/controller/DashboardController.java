package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.DashboardService;
import in.hridaykh.formbox.service.cache.TenantTierCacheService;
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

	private final DashboardService dashboardService;
	private final TenantTierCacheService tenantTierCacheService;

	public DashboardController(DashboardService dashboardService, TenantTierCacheService tenantTierCacheService) {
		this.dashboardService = dashboardService;
		this.tenantTierCacheService = tenantTierCacheService;
	}

	@GetMapping
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		log.trace("Initiating dashboard view generation request routing context.");

		if (userMetadata == null || userMetadata.getSub() == null) {
			log.warn("Dashboard interception access denial rule triggered. User metadata context is completely unauthenticated.");
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}

		log.debug("Authenticating dashboard context for user ID reference payload: {}", userMetadata.getSub());
		UUID tenant = dashboardService.getOrCreateTenantWithFreeSubscription(userMetadata);

		String activeTier = tenantTierCacheService.resolveHighestActiveTierNonNull(tenant);
		log.debug("Resolved active workspace references for user ID: {} -> Tenant ID: {}, Service Tier: {}", userMetadata.getSub(), tenant, activeTier);

		model.addAttribute("user", userMetadata);
		model.addAttribute("tier", activeTier);

		if (customerSessionToken != null && !customerSessionToken.isBlank()) {
			log.info("Detected Polar query string authentication fallback parameter payload. Cleaning execution URL state via dynamic client-side refresh redirect loop.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}

		return ViewRegistry.DASHBOARD;
	}

}