package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.service.AuthServiceKt;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.service.ICacheService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.DASHBOARD) // Global lock for "/dashboard"
public class DashboardController {

	private final AuthServiceKt authServiceKt;
	private final TenantRepository tenantRepository;
	private final ICacheService cacheService;

	public DashboardController(AuthServiceKt authServiceKt, TenantRepository tenantRepository, ICacheService cacheService) {
		this.authServiceKt = authServiceKt;
		this.tenantRepository = tenantRepository;
		this.cacheService = cacheService;
	}

	@GetMapping
	public String showDashboard(@CookieValue(name = "sb_token", required = false) String token,
	                            @RequestParam(name = "customer_session_token", required = false) String customerSessionToken,
	                            Model model) {

		// Removed hardcoded redirect string
		if (token == null || token.isBlank()) {
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;
		}

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

		boolean checkingPurchase = customerSessionToken != null && !customerSessionToken.isBlank();
		model.addAttribute("checkingPurchase", checkingPurchase);

		return ViewRegistry.DASHBOARD;
	}

	@GetMapping(PathRegistry.Dashboard.BILLING_STATUS) // "/billing-status"
	public String pollBillingStatus(@CookieValue(name = "sb_token") String token,
	                                @RequestParam(name = "customer_session_token", required = false) String customerSessionToken,
	                                Model model) {

		var userMetadata = authServiceKt.getUserMetadata(token);
		UUID userId = UUID.fromString(userMetadata.getId());

		Tenant tenant = tenantRepository.findById(userId).orElseThrow();

		model.addAttribute("tenant", tenant);
		model.addAttribute("tier", tenant.resolveCurrentTier());

		boolean checkingPurchase = customerSessionToken != null && !customerSessionToken.isBlank();
		model.addAttribute("checkingPurchase", checkingPurchase);

		// Replaced raw Thymeleaf fragment syntax path string
		return ViewRegistry.Dashboard.Fragments.SUBSCRIPTION_CARD_INNER;
	}
}