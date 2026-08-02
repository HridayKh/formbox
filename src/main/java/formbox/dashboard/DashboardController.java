package formbox.dashboard;

import formbox.billing.PolarSubmissionApi;
import formbox.folder.FolderApi;
import formbox.folder.FolderDto;
import formbox.form.FormDto;
import formbox.billing.Entitlements;
import formbox.submission.FormSubmissionsResponse;
import formbox.shared.PathRegistry;
import formbox.billing.EntitlementsApi;
import formbox.form.FormApi;
import formbox.submission.SubmissionApi;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
class DashboardController {

	private final EntitlementsApi entitlementsApi;
	private final FormApi formApi;
	private final PolarSubmissionApi polarSubmissionApi;
	private final SubmissionApi submissionApi;
	private final FolderApi folderApi;

	@GetMapping("/dashboard")
	@WithSpan
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, @RequestParam(required = false) String msg, Model model) {
		log.trace("Initiating dashboard view generation request routing context.");

		if (userMetadata == null || userMetadata.getSub() == null) {
			log.warn("Dashboard interception access denial rule triggered. User metadata context is completely unauthenticated.");
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		Entitlements entitlements = entitlementsApi.getEntitlements(tenantId);
		log.debug("Resolved entitlements for Tenant ID: {}, Service Tier: {}", tenantId, entitlements.tierName());

		List<FormDto> forms = formApi.getTenantForms(tenantId);
		List<FolderDto> folders = folderApi.getTenantFolders(tenantId);
		List<FolderFormDTO> folderForms = new ArrayList<>(folders.size());

		for (FolderDto folder : folders) {
			List<FormDto> folderForm = forms.stream().filter((f) -> f.folderId().equals(folder.id())).toList();
			folderForms.add(new FolderFormDTO(folder, folderForm));
		}

		model.addAttribute("balanceLeft", polarSubmissionApi.getCachedSubmissionBalance(tenantId));
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", userMetadata.getEmail());
		model.addAttribute("msg", msg);
		model.addAttribute("folderForms", folderForms);
		model.addAttribute("allowRedirUrl", entitlements.redirectUrlsAllowed());

		if (customerSessionToken != null && !customerSessionToken.isBlank()) {
			log.debug("Detected customer_session_token parameter; redirecting to dashboard without query string.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}

		return "dash/dashboard";
	}

	@GetMapping("/forms/{ignoredFolderId}/{formId}")
	@WithSpan
	public String manageFormPage(@RequestAttribute JwtPayload userMetadata, @RequestParam(required = false) String msg, @PathVariable UUID formId, Model model, @PathVariable String ignoredFolderId) {
		log.debug("Loading primary console management data array structure for form ID: {} triggered by user: {}", formId, userMetadata.getSub());
		FormDto form = formApi.getFormDto(formId);

		if (form == null) return "redirect:/dashboard?msg=Form not found!";

		if (!form.tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized form manage attempt of form {} by user {}", formId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid form";
		}

		FormSubmissionsResponse submissions = submissionApi.getFormSubmissionsGrouped(formId);
		Entitlements entitlements = entitlementsApi.getEntitlements(form.tenantId());

		log.trace("Loaded dashboard variables for form {}: {} submissions, {} spam", formId, submissions.submissions().size(), submissions.spam().size());

		model.addAttribute("msg", msg);
		model.addAttribute("balanceLeft", polarSubmissionApi.getCachedSubmissionBalance(form.tenantId()));
		model.addAttribute("showManageSubscription", !entitlements.isFree());
		model.addAttribute("email", userMetadata.getEmail());

		List<FolderDto> folders = folderApi.getTenantFolders(form.tenantId());
		List<FolderDto> thisFolder = folders.stream().filter(f -> f.id().equals(form.folderId())).toList();

		model.addAttribute("folders", folders);
		model.addAttribute("form", form);
		model.addAttribute("folderName", thisFolder.getFirst().name());
		model.addAttribute("entitlements", entitlements);
		model.addAttribute("validSubmissions", submissions.submissions());
		model.addAttribute("spamSubmissions", submissions.spam());

		return "dash/manage-form";
	}

}