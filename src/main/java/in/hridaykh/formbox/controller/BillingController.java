package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.model.entity.PolarProducts;
import in.hridaykh.formbox.repository.PolarProductsRepository;
import in.hridaykh.formbox.service.polar.PolarCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerSessionResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(PathRegistry.Billing.BASE)
public class BillingController {

	private final Polar polar;
	private final PolarHttpClient polarHttpClient;
	private final PolarCacheService polarCacheService;

	public BillingController(Polar polar, PolarHttpClient polarHttpClient, PolarCacheService polarCacheService) {
		this.polar = polar;
		this.polarHttpClient = polarHttpClient;
		this.polarCacheService = polarCacheService;
	}

	@PostMapping("/upgrade/{plan}")
	@ResponseBody
	public void redirectToCheckout(@PathVariable String plan, @RequestAttribute JwtPayload userMetadata, HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("HX-Redirect", generateCheckoutUrl(plan, userMetadata, request));
	}

	@GetMapping("/upgrade/{plan}")
	public String redirectToCheckoutGet(@PathVariable String plan, @RequestAttribute JwtPayload userMetadata, HttpServletRequest request) {
		return "redirect:" + generateCheckoutUrl(plan, userMetadata, request);
	}

	@PostMapping(PathRegistry.Billing.PORTAL)
	@ResponseBody
	public void redirectToCustomerPortal(@RequestAttribute JwtPayload userMetadata, HttpServletResponse response) {
		String userId = userMetadata.getSub();
		if (userId == null) {
			response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
			return;
		}
		var session = polarHttpClient.post("/customer-sessions/", Map.of("external_customer_id", userId), PolarCustomerSessionResponse.class);
		response.setHeader("HX-Redirect", session.customerPortalUrl());
	}

	private String generateCheckoutUrl(String plan, JwtPayload userMetadata, HttpServletRequest request) {
		if ("free-v1".equals(plan))
			return PathRegistry.DASHBOARD;
		PolarProducts product = polarCacheService.productBySlug(plan.toLowerCase());
		if (product == null)
			throw new IllegalArgumentException("Unknown plan: " + plan);
		String successUrl = ServletUriComponentsBuilder.fromContextPath(request).path(PathRegistry.DASHBOARD).toUriString();

		Map<String, Object> customBody = new HashMap<>();
		customBody.put("products", List.of(product.getPolarProductId()));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);
		customBody.put("external_customer_id", userMetadata.getSub());

		return polar.checkouts().create(customBody).url();
	}
}