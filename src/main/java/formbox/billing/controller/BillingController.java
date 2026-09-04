package formbox.billing.controller;

import formbox.billing.EntitlementsApi;
import formbox.billing.PolarSubmissionApi;
import formbox.billing.StorageApi;
import formbox.billing.internal.PolarUtil;
import formbox.shared.Entitlements;
import formbox.shared.PathRegistry;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerSessionResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping(PathRegistry.Billing.BASE)
@RequiredArgsConstructor
@Slf4j
public class BillingController {

	private final PolarHttpClient polarHttpClient;
	private final EntitlementsApi entitlementsApi;
	private final PolarSubmissionApi polarSubmissionApi;
	private final StorageApi storageApi;
	private final PolarUtil polarUtil;

	@GetMapping(PathRegistry.Billing.PORTAL)
	@WithSpan
	public String handleBillingPortal(@RequestAttribute JwtPayload userMetadata,
	                                  @RequestParam(name = "action", required = false) String action,
	                                  @RequestParam(name = "msg", required = false) String msg,
	                                  HttpServletRequest request,
	                                  HttpServletResponse response,
	                                  Model model) {
		String userId = userMetadata != null ? userMetadata.getSub() : null;
		log.debug("GET request received for billing portal/pricing for user ID: {}", userId);

		if (userId == null) {
			log.warn("Billing portal request rejected. Missing user subject metadata.");
			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED);
			}
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userId);
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);

		// If user explicitly asks to manage subscription or if they are already paid and didn't specify action=plans
		if ("manage".equalsIgnoreCase(action) || (!entitlements.isFree() && !"plans".equalsIgnoreCase(action))) {
			if (entitlements.isFree()) {
				try {
					polarUtil.ensurePolarCustomerExists(userId, userMetadata.getEmail());
				} catch (Exception e) {
					log.error("Failed to ensure Polar customer for user: {}", userId, e);
				}
			}
			return redirectToPortal(userId, request, response);
		}

		// Otherwise, render pricing page
		model.addAttribute("balanceLeft", polarSubmissionApi.getCachedSubmissionBalance(tenantId));
		model.addAttribute("storageUsed", storageApi.getStorageBytesConsumed(tenantId));
		model.addAttribute("storageLimit", entitlements.storageLimitBytes());
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", userMetadata.getEmail());
		model.addAttribute("entitlements", entitlements);
		model.addAttribute("products", getProductMap());
		model.addAttribute("msg", msg);

		return "dash/pricing";
	}

	@GetMapping("/manage")
	@WithSpan
	public String redirectToCustomerPortalOnly(@RequestAttribute JwtPayload userMetadata, HttpServletRequest request, HttpServletResponse response) {
		String userId = userMetadata != null ? userMetadata.getSub() : null;
		if (userId == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}
		try {
			polarUtil.ensurePolarCustomerExists(userId, userMetadata.getEmail());
		} catch (Exception e) {
			log.error("Failed to ensure Polar customer before portal redirect for user: {}", userId, e);
		}
		return redirectToPortal(userId, request, response);
	}

	@RequestMapping(value = "/checkout", method = {RequestMethod.GET, RequestMethod.POST})
	@WithSpan
	public String createCheckoutSession(@RequestAttribute JwtPayload userMetadata,
	                                    @RequestParam("product_id") String productId,
	                                    HttpServletRequest request,
	                                    HttpServletResponse response) {
		String userId = userMetadata != null ? userMetadata.getSub() : null;
		if (userId == null) {
			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED);
			}
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		try {
			polarUtil.ensurePolarCustomerExists(userId, userMetadata.getEmail());

			String successUrl = getBaseUrl(request) + PathRegistry.DASHBOARD + "?msg=" + URLEncoder.encode("Subscription checkout initiated successfully!", StandardCharsets.UTF_8);

			Map<String, Object> payload = new HashMap<>();
			payload.put("product_id", productId);
			payload.put("external_customer_id", userId);
			payload.put("customer_email", userMetadata.getEmail());
			payload.put("success_url", successUrl);

			var checkoutRes = polarHttpClient.post("/checkouts/custom/", payload, Map.class);
			String checkoutUrl = (String) checkoutRes.get("url");

			log.info("Successfully created Polar checkout session for user {} with product {}: {}", userId, productId, checkoutUrl);

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", checkoutUrl);
			}
			return "redirect:" + checkoutUrl;
		} catch (Exception e) {
			log.error("Failed to create checkout session for user ID: {} product ID: {}", userId, productId, e);
			String errorMessage = URLEncoder.encode("Failed to initiate checkout. Please try again later.", StandardCharsets.UTF_8);
			String fallbackUrl = PathRegistry.Billing.BASE + PathRegistry.Billing.PORTAL + "?msg=" + errorMessage;

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", fallbackUrl);
			}
			return "redirect:" + fallbackUrl;
		}
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

			String errorMessage = URLEncoder.encode("Failed to open billing portal. Please try again later.", StandardCharsets.UTF_8);
			String fallbackUrl = PathRegistry.DASHBOARD + "?msg=" + errorMessage;

			if (request.getHeader("HX-Request") != null) {
				response.setHeader("HX-Redirect", fallbackUrl);
			}
			return "redirect:" + fallbackUrl;
		}
	}

	private Map<String, String> getProductMap() {
		Map<String, String> map = new HashMap<>();
		map.put("starter_month", "820ba1ad-d808-4a0c-908e-35371f426186");
		map.put("starter_year", "21c1ec9d-21e8-4641-926b-3ed1b4b8d15a");
		map.put("pro_month", "1dd24a52-785f-491a-99d8-6040ac678660");
		map.put("pro_year", "42b707b5-6d03-4ed6-8880-f3d509a1435b");

		try {
			var res = polarHttpClient.get("/products/?is_archived=false", Map.class);
			if (res != null && res.containsKey("items")) {
				List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("items");
				for (Map<String, Object> item : items) {
					String id = (String) item.get("id");
					String interval = (String) item.get("recurring_interval");
					Map<String, String> metadata = (Map<String, String>) item.get("metadata");
					if (metadata != null && metadata.containsKey("tier_name") && interval != null) {
						String tier = metadata.get("tier_name").toLowerCase();
						String key = tier + "_" + interval;
						map.put(key, id);
					}
				}
			}
		} catch (Exception e) {
			log.warn("Could not fetch active products dynamically from Polar API; using default product IDs", e);
		}
		return map;
	}

	private String getBaseUrl(HttpServletRequest request) {
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String contextPath = request.getContextPath();

		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
			url.append(":").append(serverPort);
		}
		url.append(contextPath);
		return url.toString();
	}
}