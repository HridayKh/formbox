package in.hridaykh.formbox.billing.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.billing.model.PolarProductDetails;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import in.hridaykh.formbox.billing.service.PolarCacheService;
import in.hridaykh.formbox.service.TenantService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerSessionResponse;

import java.util.*;

@Controller
@RequestMapping(PathRegistry.Billing.BASE)
@RequiredArgsConstructor
@Slf4j
public class BillingController {

	private final Polar polar;
	private final PolarHttpClient polarHttpClient;
	private final PolarCacheService polarCacheService;
	private final EntitlementsCacheService entitlementsCacheService;
	private final TenantService tenantService;


	@GetMapping("/upgrade")
	@WithSpan
	public String showUpgradeOptions(@RequestAttribute JwtPayload userMetadata, Model model) {
		UUID userId = UUID.fromString(userMetadata.getSub());
		Entitlements entitlements = entitlementsCacheService.getEntitlements(userId);
		
		List<PolarProductDetails> allProducts = polarCacheService.getAllProducts();
		List<PolarProductDetails> upgradeOptions = allProducts.stream()
			.filter(p -> p.priority() > entitlements.tierPriority())
			.toList();

		model.addAttribute("currentTier", entitlements.tierName() == null ? "free" : entitlements.tierName());
		model.addAttribute("upgradeOptions", upgradeOptions);
		return "dashboard/upgrade-options";
	}

	@GetMapping("/upgrade/{plan}")
	@WithSpan
	public String redirectToCheckoutGet(@PathVariable String plan, @RequestAttribute JwtPayload userMetadata, HttpServletRequest request) {
		UUID userId = UUID.fromString(userMetadata.getSub());
		Entitlements entitlements = entitlementsCacheService.getEntitlements(userId);

		PolarProductDetails targetProduct = polarCacheService.getPolarProductDetailsBySlug(plan);
		if (targetProduct == null) {
			log.error("Plan not found: {}", plan);
			return "redirect:" + PathRegistry.DASHBOARD;
		}

		// Only allow checkouts if the target tier priority is higher than current
		if (targetProduct.priority() <= entitlements.tierPriority()) {
			log.warn("User {} attempted to checkout plan {} with priority {}, but current priority is {}", 
				userId, plan, targetProduct.priority(), entitlements.tierPriority());
			return "redirect:/billing/upgrade";
		}

		// Ensure customer exists on Polar for free-tier users
		if (entitlements.isFree()) {
			try {
				tenantService.ensurePolarCustomerExists(userId.toString(), userMetadata.getEmail());
			} catch (Exception e) {
				log.error("Failed to ensure Polar customer existence before checkout for user: {}", userId, e);
			}
		}

		String successUrl = ServletUriComponentsBuilder.fromContextPath(request).path(PathRegistry.DASHBOARD).toUriString();
		Map<String, Object> customBody = new HashMap<>();
		customBody.put("products", List.of(targetProduct.id()));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);
		customBody.put("external_customer_id", userId.toString());

		try {
			String url = polar.checkouts().create(customBody).url();
			log.info("Successfully provisioned Polar hosted checkout pipeline context link instance for plan product: {} (ID: {})", plan, targetProduct.id());
			return "redirect:" + url;
		} catch (Exception e) {
			log.error("Failed to generate Polar checkout link", e);
			return "redirect:/billing/upgrade";
		}
	}

	@GetMapping(PathRegistry.Billing.PORTAL)
	@WithSpan
	public String redirectToCustomerPortal(@RequestAttribute JwtPayload userMetadata, HttpServletRequest request, HttpServletResponse response) {
		String userId = userMetadata.getSub();
		log.debug("GET request received to generate customer portal session for user ID: {}", userId);

		if (userId == null) {
			log.warn("Customer portal generation rejected. Missing user subject metadata in request attributes.");
			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
				return null;
			}
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		Entitlements entitlements = entitlementsCacheService.getEntitlements(UUID.fromString(userId));
		if (entitlements.isFree()) {
			log.warn("Customer {} attempted to redirect to portal on free tier!", userId);

			String errorMessage = java.net.URLEncoder.encode("Cannot manage subscription on free tier!", java.nio.charset.StandardCharsets.UTF_8);
			String fallbackUrl = PathRegistry.DASHBOARD + "?msg=" + errorMessage;

			if (request.getHeader("HX-Request") != null)
				response.setHeader("HX-Redirect", fallbackUrl);
			return "redirect:" + fallbackUrl;
		}

		try {
			var session = polarHttpClient.post("/customer-sessions/", Map.of("external_customer_id", userId), PolarCustomerSessionResponse.class);
			log.info("Customer portal billing session successfully generated for user ID: {}", userId);

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", session.customerPortalUrl());
				return null;
			}
			return "redirect:" + session.customerPortalUrl();
		} catch (Exception e) {
			log.error("Failed to provision downstream customer session portal from Polar billing client layer for user ID: {}", userId, e);

			String errorMessage = java.net.URLEncoder.encode("Failed to open billing portal. Please try again later.", java.nio.charset.StandardCharsets.UTF_8);
			String fallbackUrl = PathRegistry.DASHBOARD + "?msg=" + errorMessage;

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", fallbackUrl);
				return null;
			}
			return "redirect:" + fallbackUrl;
		}
	}
}