package in.hridaykh.formbox.billing.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.service.cache.TenantCacheService;
import in.hridaykh.formbox.billing.service.PolarCacheService;
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

import java.util.*;

@Controller
@RequestMapping(PathRegistry.Billing.BASE)
@RequiredArgsConstructor
@Slf4j
public class BillingController {

	private final Polar polar;
	private final PolarHttpClient polarHttpClient;
	private final PolarCacheService polarCacheService;
	private final TenantCacheService tenantCacheService;


	@GetMapping("/upgrade/{plan}")
	public String redirectToCheckoutGet(@PathVariable String plan, @RequestAttribute JwtPayload userMetadata, HttpServletRequest request) {
		log.debug("Processing standard GET redirect checkout request for plan: {} from user: {}", plan, userMetadata.getSub());
		return "redirect:" + generateCheckoutUrl(plan, userMetadata, request);
	}

	@GetMapping(PathRegistry.Billing.PORTAL)
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

		String tier = tenantCacheService.resolveHighestActiveTierNonNull(UUID.fromString(userId));
		if ("free".equalsIgnoreCase(tier)) {
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

	private String generateCheckoutUrl(String plan, JwtPayload userMetadata, HttpServletRequest request) {
		log.trace("Initiating checkout URL generation workflow for requested plan token: {}", plan);

		if ("free".equalsIgnoreCase(plan) || "free-v1".equalsIgnoreCase(plan)) {
			log.debug("Plan parameter detected as tier baseline default [free]. Skipping checkout routing, redirecting straight to dashboard.");
			return PathRegistry.DASHBOARD;
		}

		UUID tierUuid = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));
		String tier = tenantCacheService.resolveHighestActiveTierNonNull(tierUuid);

		if (plan.equalsIgnoreCase(tier)) {
			log.debug("User already on requested plan {}. Skipping checkout routing, redirecting straight to dashboard.", plan);
			return PathRegistry.DASHBOARD;
		}

		if ("pro-v1".equalsIgnoreCase(tier)) {
			log.debug("Pro user cannot upgrade further. Skipping checkout routing, redirecting straight to dashboard.");
			return PathRegistry.DASHBOARD;
		}

		String polarProductId = polarCacheService.getPolarProductIdBySlug(plan);
		if (polarProductId == null) {
			log.error("Resolution mismatch error. Target checkout schema strategy maps to an unknown plan: {}", plan);
			throw new IllegalArgumentException("Unknown plan: " + plan);
		}

		String successUrl = ServletUriComponentsBuilder.fromContextPath(request).path(PathRegistry.DASHBOARD).toUriString();
		log.trace("Configured callback endpoint fallback resolution tracking target url context to: {}", successUrl);

		Map<String, Object> customBody = new HashMap<>();
		customBody.put("products", List.of(polarProductId));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);
		customBody.put("external_customer_id", userMetadata.getSub());

		try {
			log.info("Successfully provisioned Polar hosted checkout pipeline context link instance for plan product: {} (ID: {})", plan, polarProductId);
			return polar.checkouts().create(customBody).url();
		} catch (Exception e) {
			log.error("Failed to complete remote checkout generation handshake structure with external Polar API engine parameters.", e);
			throw e;
		}
	}
}