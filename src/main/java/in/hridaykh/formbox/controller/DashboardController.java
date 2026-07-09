package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
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

	private final TenantTierCacheService tenantTierCacheService;

	public DashboardController(TenantTierCacheService tenantTierCacheService) {
		this.tenantTierCacheService = tenantTierCacheService;
	}

	@GetMapping
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		log.trace("Initiating dashboard view generation request routing context.");

		if (userMetadata == null || userMetadata.getSub() == null) {
			log.warn("Dashboard interception access denial rule triggered. User metadata context is completely unauthenticated.");
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		String activeTier = tenantTierCacheService.resolveHighestActiveTierNonNull(tenantId);
		log.debug("Resolved active workspace references for Tenant ID: {}, Service Tier: {}", tenantId, activeTier);

		model.addAttribute("user", userMetadata);
		model.addAttribute("tier", activeTier);

		if (customerSessionToken != null && !customerSessionToken.isBlank()) {
			log.info("Detected Polar query string authentication fallback parameter payload. Cleaning execution URL state via dynamic client-side refresh redirect loop.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}

		return ViewRegistry.DASHBOARD;
	}

}