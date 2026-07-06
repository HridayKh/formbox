package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.DashboardService;
import in.hridaykh.formbox.service.TenantTierService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.DASHBOARD)
public class DashboardController {

	private final DashboardService dashboardService;
	private final TenantTierService tenantTierService;

	public DashboardController(DashboardService dashboardService, TenantTierService tenantTierService) {
		this.dashboardService = dashboardService;
		this.tenantTierService = tenantTierService;
	}

	@GetMapping
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}
		UUID tenant = dashboardService.getOrCreateTenantWithFreeSubscription(userMetadata);
		model.addAttribute("user", userMetadata);
		model.addAttribute("tier", tenantTierService.resolveHighestActiveTierNonNull(tenant));

		if (customerSessionToken != null && !customerSessionToken.isBlank())
			return "redirect:" + PathRegistry.DASHBOARD;

		return ViewRegistry.DASHBOARD;
	}

}