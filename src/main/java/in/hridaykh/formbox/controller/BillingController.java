package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.AuthServiceKt;
import io.github.jan.supabase.auth.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.customer.PolarCustomerSessionResponse;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/billing")
public class BillingController {

	private final AuthServiceKt authServiceKt;
	private final Polar polar;
	private final PolarHttpClient polarHttpClient;

	public BillingController(AuthServiceKt authServiceKt, Polar polar, PolarHttpClient polarHttpClient) {
		this.authServiceKt = authServiceKt;
		this.polar = polar;
		this.polarHttpClient = polarHttpClient;
	}

	@PostMapping("/upgrade")
	@ResponseBody
	public void redirectToCheckout(@CookieValue(name = "sb_token") String token, HttpServletRequest request, HttpServletResponse response) {
		UserInfo userMetadata = authServiceKt.getUserMetadata(token);
		String productId = polar.products().list(1).items().getFirst().id().toString();

		String[] base = request.getRequestURL().toString().split("/");
		String successUrl = base[0] + "/" + base[1] + "/" + base[2] + "/" + ViewRegistry.DASHBOARD;

		// Build the payload manually to place 'external_customer_id' at the root layer
		java.util.Map<String, Object> customBody = new java.util.HashMap<>();
		customBody.put("products", java.util.List.of(productId));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);

		// CRITICAL: This links the Supabase user identity straight to the root Polar customer resource!
		customBody.put("external_customer_id", userMetadata.getId());

		// Send it using the underlying dynamic creation channel
		String polarCheckoutUrl = polar.checkouts().create(customBody).url();

		response.setHeader("HX-Redirect", polarCheckoutUrl);
	}

	@PostMapping("/portal")
	@ResponseBody
	public void redirectToCustomerPortal(@CookieValue(name = "sb_token") String token, HttpServletResponse response) {
		String userId = authServiceKt.getUserMetadata(token).getId();

		PolarCustomerSessionResponse session = polarHttpClient.post(
			"/customer-sessions/",
			Map.of("external_customer_id", userId),
			PolarCustomerSessionResponse.class
		);

		response.setHeader("HX-Redirect", session.customerPortalUrl());
	}
}