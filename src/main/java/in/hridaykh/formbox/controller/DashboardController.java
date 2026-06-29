package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.service.AuthServiceKt;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.service.ICacheService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class DashboardController {

	private final AuthServiceKt authServiceKt;
	private final TenantRepository tenantRepository;
	private final ICacheService cacheService;

	public DashboardController(AuthServiceKt authServiceKt, TenantRepository tenantRepository, ICacheService cacheService) {
		this.authServiceKt = authServiceKt;
		this.tenantRepository = tenantRepository;
		this.cacheService = cacheService;
	}

	@GetMapping("/dashboard")
	public String showDashboard(@CookieValue(name = "sb_token", required = false) String token, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		if (token == null || token.isBlank()) return "redirect:/auth/login?msg=unauthorized";

		var userMetadata = authServiceKt.getUserMetadata(token);
		UUID userId = UUID.fromString(userMetadata.getId());

		Tenant tenant = tenantRepository.findById(userId).orElseGet(() -> {
			Tenant newTenant = new Tenant(userId, userMetadata.getEmail());
			newTenant.giveFreeSubscription();
			tenantRepository.save(newTenant);

			cacheService.set("user:" + userId + ":tier", "free");
			cacheService.set("user:" + userId + ":meter_balance", "100");
			return newTenant;
		});

		model.addAttribute("user", userMetadata);
		model.addAttribute("tenant", tenant);
		model.addAttribute("tier", tenant.resolveCurrentTier());

		// If they just got back from Polar checkout, show the loader state
		boolean checkingPurchase = customerSessionToken != null && !customerSessionToken.isBlank();
		model.addAttribute("checkingPurchase", checkingPurchase);

		return ViewRegistry.DASHBOARD;
	}

	@GetMapping("/dashboard/billing-status")
	public String pollBillingStatus(@CookieValue(name = "sb_token") String token, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {

		var userMetadata = authServiceKt.getUserMetadata(token);
		UUID userId = UUID.fromString(userMetadata.getId());

		Tenant tenant = tenantRepository.findById(userId).orElseThrow();

		model.addAttribute("tenant", tenant);
		model.addAttribute("tier", tenant.resolveCurrentTier());

		// CRITICAL FIX: Keep the evaluation flag alive during background polling loop stages
		boolean checkingPurchase = customerSessionToken != null && !customerSessionToken.isBlank();
		model.addAttribute("checkingPurchase", checkingPurchase);

		// Return the specific card element block string context layout
		return "dashboard :: subscriptionCardInner";
	}
}