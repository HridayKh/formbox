package formbox.core.web;

import formbox.billing.PolarSubmissionApi;
import formbox.shared.Entitlements;
import formbox.shared.PathRegistry;
import formbox.billing.EntitlementsApi;
import formbox.core.dto.CachedForm;
import formbox.core.dto.FolderFormDTO;
import formbox.core.entity.Folder;
import formbox.core.cache.FolderCacheService;
import formbox.core.cache.FormCacheService;
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
@RequestMapping(PathRegistry.DASHBOARD)
@RequiredArgsConstructor
@Slf4j
class DashboardController {

	private final EntitlementsApi entitlementsApi;
	private final FolderCacheService folderCacheService;
	private final FormCacheService formCacheService;
	private final PolarSubmissionApi polarSubmissionApi;

	@GetMapping
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

		List<CachedForm> forms = formCacheService.getTenantForms(tenantId);
		List<Folder> folders = folderCacheService.getTenantFolders(tenantId);
		List<FolderFormDTO> folderForms = new ArrayList<>(folders.size());

		for (Folder folder : folders) {
			List<CachedForm> folderForm = forms.stream().filter((f) -> f.folderId().equals(folder.getId())).toList();
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

}