package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.Tiers;
import in.hridaykh.formbox.model.entity.PolarProducts;
import in.hridaykh.formbox.service.cache.TenantTierCacheService;
import in.hridaykh.formbox.service.polar.PolarCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerSessionResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.Billing.BASE)
@RequiredArgsConstructor
@Slf4j
public class BillingController {

	private final Polar polar;
	private final PolarHttpClient polarHttpClient;
	private final PolarCacheService polarCacheService;
	private final TenantTierCacheService tenantTierCacheService;

	@PostMapping("/upgrade/{plan}")
	@ResponseBody
	public void redirectToCheckout(@PathVariable String plan, @RequestAttribute JwtPayload userMetadata, HttpServletRequest request, HttpServletResponse response) {
		log.debug("Processing htmx checkout request for plan: {} from user: {}", plan, userMetadata.getSub());
		response.setHeader("HX-Redirect", generateCheckoutUrl(plan, userMetadata, request));
	}

	@GetMapping("/upgrade/{plan}")
	public String redirectToCheckoutGet(@PathVariable String plan, @RequestAttribute JwtPayload userMetadata, HttpServletRequest request) {
		log.debug("Processing standard GET redirect checkout request for plan: {} from user: {}", plan, userMetadata.getSub());
		return "redirect:" + generateCheckoutUrl(plan, userMetadata, request);
	}

	@PostMapping(PathRegistry.Billing.PORTAL)
	@ResponseBody
	public void redirectToCustomerPortal(@RequestAttribute JwtPayload userMetadata, HttpServletResponse response) {
		String userId = userMetadata.getSub();
		log.debug("Request received to generate customer portal session for user ID: {}", userId);

		if (userId == null) {
			log.warn("Customer portal generation rejected. Missing user subject metadata in request attributes.");
			response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
			return;
		}

		String tier = tenantTierCacheService.resolveHighestActiveTierNonNull(UUID.fromString(userId));
		if (Tiers.isFree(tier)) {
			log.warn("Customer {} attempted to redirect to checkout on free tier!", userId);
			try {
				response.getWriter().write("Cannot manage subscription on free tier!");
			} catch (IOException e) {
				log.error("unable to write to response after tier check", e);
			}
			return;
		}

		try {
			var session = polarHttpClient.post("/customer-sessions/", Map.of("external_customer_id", userId), PolarCustomerSessionResponse.class);
			log.info("Customer portal billing session successfully generated for user ID: {}", userId);
			response.setHeader("HX-Redirect", session.customerPortalUrl());
		} catch (Exception e) {
			log.error("Failed to provision downstream customer session portal from Polar billing client layer for user ID: {}", userId, e);
			throw e;
		}
	}

	private String generateCheckoutUrl(String plan, JwtPayload userMetadata, HttpServletRequest request) {
		log.trace("Initiating checkout URL generation workflow for requested plan token: {}", plan);

		if (Tiers.isFree(plan)) {
			log.debug("Plan parameter detected as tier baseline default [free-v1]. Skipping checkout routing, redirecting straight to dashboard.");
			return PathRegistry.DASHBOARD;
		}

		PolarProducts product = polarCacheService.productBySlug(plan.toLowerCase());
		if (product == null) {
			log.error("Resolution mismatch error. Target checkout schema strategy maps to an unknown plan: {}", plan);
			throw new IllegalArgumentException("Unknown plan: " + plan);
		}

		String successUrl = ServletUriComponentsBuilder.fromContextPath(request).path(PathRegistry.DASHBOARD).toUriString();
		log.trace("Configured callback endpoint fallback resolution tracking target url context to: {}", successUrl);

		Map<String, Object> customBody = new HashMap<>();
		customBody.put("products", List.of(product.getPolarProductId()));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);
		customBody.put("external_customer_id", userMetadata.getSub());

		try {
			String checkoutUrl = polar.checkouts().create(customBody).url();
			log.info("Successfully provisioned Polar hosted checkout pipeline context link instance for plan product: {} (ID: {})", plan, product.getPolarProductId());
			return checkoutUrl;
		} catch (Exception e) {
			log.error("Failed to complete remote checkout generation handshake structure with external Polar API engine parameters.", e);
			throw e;
		}
	}
}