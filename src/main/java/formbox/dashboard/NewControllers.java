package formbox.dashboard;

import formbox.billing.EntitlementsApi;
import formbox.billing.PolarSubmissionApi;
import formbox.shared.Entitlements;
import formbox.shared.PathRegistry;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
class NewControllers {

	private final EntitlementsApi entitlementsApi;
	private final PolarSubmissionApi polarSubmissionApi;

	@GetMapping("/dashboard/support")
	@WithSpan
	public String supportPage(@RequestAttribute JwtPayload userMetadata, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		populateNavbarModel(tenantId, userMetadata.getEmail(), model);

		return "dash/support";
	}

	@GetMapping("/dashboard/emails")
	@WithSpan
	public String emailsPage(@RequestAttribute JwtPayload userMetadata, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		populateNavbarModel(tenantId, userMetadata.getEmail(), model);

		return "dash/emails";
	}

	private void populateNavbarModel(UUID tenantId, String email, Model model) {
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		model.addAttribute("balanceLeft", polarSubmissionApi.getCachedSubmissionBalance(tenantId));
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", email);
	}
}
