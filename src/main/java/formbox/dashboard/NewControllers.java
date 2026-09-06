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

import formbox.notifs.UploadService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequiredArgsConstructor
@Slf4j
class NewControllers {

	private final EntitlementsApi entitlementsApi;
	private final PolarSubmissionApi polarSubmissionApi;
	private final VerifiedEmailsService verifiedEmailsService;
	private final FormApi formApi;
	private final CsvExportApi csvExportApi;
	private final UploadService uploadService;

	@GetMapping("/dashboard/support")
	@WithSpan
	public String supportPage(@RequestAttribute JwtPayload userMetadata, Model model, @RequestParam(required = false, defaultValue = "") String msg) {
		if (userMetadata == null || userMetadata.getSub() == null)
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		populateNavbarModel(tenantId, userMetadata.getEmail(), model);

		model.addAttribute("msg", msg);

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

	@PostMapping("/forms/{folderId}/{formId}/exports/delete")
	@WithSpan
	public String deleteCsvExport(@RequestAttribute JwtPayload userMetadata,
	                              @PathVariable UUID folderId,
	                              @PathVariable UUID formId,
	                              @RequestParam("fileName") String fileName,
	                              HttpServletRequest request) {
		if (userMetadata == null || userMetadata.getSub() == null) {
			return "redirect:" + PathRegistry.Auth.LoginRedirs.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		FormDto form = formApi.getFormDto(formId);

		if (form == null || !form.tenantId().equals(tenantId)) {
			return "redirect:/dashboard?msg=Invalid form";
		}

		uploadService.deleteCsvExport(formId, fileName);
		log.info("Deleted CSV export file: {} for form ID: {}", fileName, formId);

		String referer = request.getHeader("Referer");
		String target = (referer != null && !referer.isBlank()) ? referer : "/forms/" + folderId + "/" + formId;
		if (target.contains("msg=")) {
			target = target.replaceAll("msg=[^&]*", "msg=" + java.net.URLEncoder.encode("CSV export deleted successfully", java.nio.charset.StandardCharsets.UTF_8));
		} else if (target.contains("?")) {
			target += "&msg=" + java.net.URLEncoder.encode("CSV export deleted successfully", java.nio.charset.StandardCharsets.UTF_8);
		} else {
			target += "?msg=" + java.net.URLEncoder.encode("CSV export deleted successfully", java.nio.charset.StandardCharsets.UTF_8);
		}

		return "redirect:" + target;
	}

	private void populateNavbarModel(UUID tenantId, String email, Model model) {
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		model.addAttribute("balanceLeft", polarSubmissionApi.getCachedSubmissionBalance(tenantId));
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", email);
	}
}
