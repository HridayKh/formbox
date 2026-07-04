package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.service.ICacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.DASHBOARD)
public class DashboardController {

	private final TenantRepository tenantRepository;
	private final ICacheService cacheService;

	public DashboardController(TenantRepository tenantRepository, ICacheService cacheService) {
		this.tenantRepository = tenantRepository;
		this.cacheService = cacheService;
	}

	@GetMapping
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		if (userMetadata == null) return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;

		String userIdStr = userMetadata.getSub();
		if (userIdStr == null)
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;

		UUID userId = UUID.fromString(userIdStr);
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

	@GetMapping(PathRegistry.Dashboard.BILLING_STATUS)
	public String pollBillingStatus(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null)
			return PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED;

		UUID userId = UUID.fromString(userMetadata.getSub());

		Tenant tenant = tenantRepository.findById(userId).orElseThrow();

		model.addAttribute("tenant", tenant);
		model.addAttribute("tier", tenant.resolveCurrentTier());

		boolean checkingPurchase = customerSessionToken != null && !customerSessionToken.isBlank();
		model.addAttribute("checkingPurchase", checkingPurchase);

		return ViewRegistry.Dashboard.Fragments.SUBSCRIPTION_CARD_INNER;
	}
}