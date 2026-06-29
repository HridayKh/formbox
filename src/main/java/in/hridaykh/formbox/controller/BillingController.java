package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.service.AuthServiceKt;
import io.github.jan.supabase.auth.user.UserInfo;
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
@RequestMapping(PathRegistry.Billing.BASE) // "/billing"
public class BillingController {

	private final AuthServiceKt authServiceKt;
	private final Polar polar;
	private final PolarHttpClient polarHttpClient;

	public BillingController(AuthServiceKt authServiceKt, Polar polar, PolarHttpClient polarHttpClient) {
		this.authServiceKt = authServiceKt;
		this.polar = polar;
		this.polarHttpClient = polarHttpClient;
	}

	@PostMapping(PathRegistry.Billing.UPGRADE) // "/upgrade"
	@ResponseBody
	public void redirectToCheckout(@CookieValue(name = "sb_token") String token,
	                               HttpServletRequest request,
	                               HttpServletResponse response) {

		UserInfo userMetadata = authServiceKt.getUserMetadata(token);
		String productId = polar.products().list(1).items().getFirst().id().toString();

		// Dynamically builds "https://domain.com/dashboard" without manual string hacking
		String successUrl = ServletUriComponentsBuilder.fromContextPath(request)
			.path(PathRegistry.DASHBOARD)
			.toUriString();

		Map<String, Object> customBody = new HashMap<>();
		customBody.put("products", List.of(productId));
		customBody.put("customer_email", userMetadata.getEmail());
		customBody.put("success_url", successUrl);
		customBody.put("external_customer_id", userMetadata.getId());

		String polarCheckoutUrl = polar.checkouts().create(customBody).url();

		response.setHeader("HX-Redirect", polarCheckoutUrl);
	}

	@PostMapping(PathRegistry.Billing.PORTAL) // "/portal"
	@ResponseBody
	public void redirectToCustomerPortal(@CookieValue(name = "sb_token") String token,
	                                     HttpServletResponse response) {

		String userId = authServiceKt.getUserMetadata(token).getId();

		// "/customer-sessions/" can be extracted to an external API route config file or constant file later if it changes often
		PolarCustomerSessionResponse session = polarHttpClient.post(
			"/customer-sessions/",
			Map.of("external_customer_id", userId),
			PolarCustomerSessionResponse.class
		);

		response.setHeader("HX-Redirect", session.customerPortalUrl());
	}
}