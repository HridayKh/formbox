package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.service.DashboardService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(PathRegistry.DASHBOARD)
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}
		Tenant tenant = dashboardService.getOrCreateTenantWithFreeSubscription(userMetadata);
		model.addAttribute("user", userMetadata);
		model.addAttribute("tenant", tenant);
		model.addAttribute("tier", dashboardService.resolveHighestActiveTier(tenant));

		if (customerSessionToken != null && !customerSessionToken.isBlank())
			return "redirect:" + PathRegistry.DASHBOARD;

		return ViewRegistry.DASHBOARD;
	}

}