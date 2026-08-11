package formbox.dashboard;

import formbox.billing.EntitlementsApi;
import formbox.billing.PolarSubmissionApi;
import formbox.shared.Entitlements;
import formbox.shared.PathRegistry;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import formbox.form.FormApi;
import formbox.form.FormDto;
import formbox.submission.CsvExportApi;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
class NewControllers {

	private final EntitlementsApi entitlementsApi;
	private final PolarSubmissionApi polarSubmissionApi;
	private final VerifiedEmailsService verifiedEmailsService;
	private final FormApi formApi;
	private final CsvExportApi csvExportApi;

	@GetMapping("/dashboard/support")
	@WithSpan
	public String supportPage(@RequestAttribute JwtPayload userMetadata, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		populateNavbarModel(tenantId, userMetadata.getEmail(), model);

		return "dash/support";
	}

	@GetMapping("/dashboard/emails")
	@WithSpan
	public String emailsPage(@RequestAttribute JwtPayload userMetadata, @RequestParam(required = false) String msg, Model model) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		populateNavbarModel(tenantId, userMetadata.getEmail(), model);
		model.addAttribute("verifiedEmails", verifiedEmailsService.getVerifiedEmails(tenantId));
		model.addAttribute("msg", msg);

		return "dash/emails";
	}

	@PostMapping("/dashboard/emails")
	@WithSpan
	public String sendVerificationEmail(@RequestAttribute JwtPayload userMetadata, @RequestParam String email) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		try {
			verifiedEmailsService.sendVerificationEmail(tenantId, email.strip());
			return "redirect:/dashboard/emails?msg=Verification email sent to " + email.strip() + ". Check your inbox!";
		} catch (Exception e) {
			log.error("Failed to send verification email for tenant ID: {}", tenantId, e);
			return "redirect:/dashboard/emails?msg=Failed to send verification email. Please try again.";
		}
	}

	@GetMapping("/dashboard/emails-verify")
	@WithSpan
	public String verifyEmail(@RequestAttribute JwtPayload userMetadata,
	                          @RequestParam(required = false) String token,
	                          @RequestParam(required = false) String email,
	                          @RequestParam(required = false) String code) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		boolean verified = verifiedEmailsService.verifyEmail(tenantId, token, email, code);

		if (verified) {
			return "redirect:/dashboard/emails?msg=Email verified successfully!";
		} else {
			return "redirect:/dashboard/emails?msg=Verification failed. The link may have expired or is invalid. Please try again.";
		}
	}

	@PostMapping("/dashboard/emails-remove")
	@WithSpan
	public String removeEmail(@RequestAttribute JwtPayload userMetadata, @RequestParam String email) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		verifiedEmailsService.removeEmail(tenantId, email.strip());
		return "redirect:/dashboard/emails?msg=" + email.strip() + " removed.";
	}

	@PostMapping("/forms/{folderId}/{formId}/export-csv")
	@WithSpan
	public String exportCsv(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID folderId, @PathVariable UUID formId) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		FormDto form = formApi.getFormDto(formId);

		if (form == null || !form.tenantId().equals(tenantId)) {
			return "redirect:/dashboard?msg=Invalid form";
		}

		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		if (!entitlements.csvExportsAllowed()) {
			return "redirect:/forms/" + folderId + "/" + formId + "?msg=CSV exports require a plan upgrade. Please upgrade your plan!";
		}

		csvExportApi.generateAndUploadCsvExport(tenantId, userMetadata.getEmail(), formId);

		return "redirect:/forms/" + folderId + "/" + formId + "?msg=CSV export job started! Check your email for the download link.";
	}

	private void populateNavbarModel(UUID tenantId, String email, Model model) {
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		model.addAttribute("balanceLeft", polarSubmissionApi.getCachedSubmissionBalance(tenantId));
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", email);
	}
}
