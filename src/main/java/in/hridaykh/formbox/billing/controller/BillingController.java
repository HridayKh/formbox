package in.hridaykh.formbox.billing.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import in.hridaykh.formbox.service.TenantService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerSessionResponse;

import java.util.*;

@Controller
@RequestMapping(PathRegistry.Billing.BASE)
@RequiredArgsConstructor
@Slf4j
public class BillingController {

	private final PolarHttpClient polarHttpClient;
	private final EntitlementsCacheService entitlementsCacheService;
	private final TenantService tenantService;

	@GetMapping(PathRegistry.Billing.PORTAL)
	@WithSpan
	public String redirectToCustomerPortal(@RequestAttribute JwtPayload userMetadata, HttpServletRequest request, HttpServletResponse response) {
		String userId = userMetadata.getSub();
		log.debug("GET request received to generate customer portal session for user ID: {}", userId);

		if (userId == null) {
			log.warn("Customer portal generation rejected. Missing user subject metadata in request attributes.");
			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
			}
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		Entitlements entitlements = entitlementsCacheService.getEntitlements(UUID.fromString(userId));
		if (entitlements.isFree()) {
			try {
				tenantService.ensurePolarCustomerExists(userId, userMetadata.getEmail());
				log.debug("Ensured Polar customer exists for free-tier user {} before portal redirect", userId);
			} catch (Exception e) {
				log.error("Failed to ensure Polar customer before portal redirect for user: {}", userId, e);
			}
		}

		return redirectToPortal(userId, request, response);
	}

	private String redirectToPortal(String userId, HttpServletRequest request, HttpServletResponse response) {
		try {
			var session = polarHttpClient.post("/customer-sessions/", Map.of("external_customer_id", userId), PolarCustomerSessionResponse.class);
			log.info("Customer portal billing session successfully generated for user ID: {}", userId);

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", session.customerPortalUrl());
			}
			return "redirect:" + session.customerPortalUrl();
		} catch (Exception e) {
			log.error("Failed to provision customer portal session for user ID: {}", userId, e);

			String errorMessage = java.net.URLEncoder.encode("Failed to open billing portal. Please try again later.", java.nio.charset.StandardCharsets.UTF_8);
			String fallbackUrl = PathRegistry.DASHBOARD + "?msg=" + errorMessage;

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", fallbackUrl);
			}
			return "redirect:" + fallbackUrl;
		}
	}
}