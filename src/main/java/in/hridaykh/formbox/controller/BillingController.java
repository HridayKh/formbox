package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.config.PolarIdProperties;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.service.AuthServiceKt;
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

	private final AuthServiceKt authServiceKt;
	private final Polar polar;
	private final PolarHttpClient polarHttpClient;
	private final PolarIdProperties polarIdProperties;

	public BillingController(AuthServiceKt authServiceKt, Polar polar, PolarHttpClient polarHttpClient, PolarIdProperties polarIdProperties) {
		this.authServiceKt = authServiceKt;
		this.polar = polar;
		this.polarHttpClient = polarHttpClient;
		this.polarIdProperties = polarIdProperties;
	}

	@PostMapping(PathRegistry.Billing.UPGRADE)
	@ResponseBody
	public void redirectToCheckout(@CookieValue(name = "sb_token") String token, HttpServletRequest request, HttpServletResponse response) {
		JwtPayload userMetadata = authServiceKt.getUserMetadata(token);
		String successUrl = ServletUriComponentsBuilder.fromContextPath(request).path(PathRegistry.DASHBOARD).toUriString();
		Map<String, Object> customBody = new HashMap<>();
		customBody.put("products", List.of(polarIdProperties.getPaidProductId()));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);
		customBody.put("external_customer_id", userMetadata.getSub());
		customBody.put("allow_discount_codes", false);
		response.setHeader("HX-Redirect", polar.checkouts().create(customBody).url());
	}

	@PostMapping(PathRegistry.Billing.PORTAL)
	@ResponseBody
	public void redirectToCustomerPortal(@CookieValue(name = "sb_token") String token, HttpServletResponse response) {
		String userId = authServiceKt.getUserMetadata(token).getSub();
		if (userId == null) {
			response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
			return;
		}
		var session = polarHttpClient.post("/customer-sessions/", Map.of("external_customer_id", userId), PolarCustomerSessionResponse.class);
		response.setHeader("HX-Redirect", session.customerPortalUrl());
	}
}