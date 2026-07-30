package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.billing.model.Entitlements;
import in.hridaykh.formbox.billing.service.PolarCacheService;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.billing.service.EntitlementsCacheService;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.dto.FolderFormDTO;
import in.hridaykh.formbox.model.entity.Folder;
import in.hridaykh.formbox.service.cache.FolderCacheService;
import in.hridaykh.formbox.service.cache.FormCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping(PathRegistry.DASHBOARD)
@Slf4j
public class DashboardController {

	private final EntitlementsCacheService entitlementsCacheService;
	private final PolarCacheService polarCacheService;
	private final FolderCacheService folderCacheService;
	private final FormCacheService formCacheService;

	public DashboardController(EntitlementsCacheService entitlementsCacheService, PolarCacheService polarCacheService, FolderCacheService folderCacheService, FormCacheService formCacheService) {
		this.entitlementsCacheService = entitlementsCacheService;
		this.polarCacheService = polarCacheService;
		this.folderCacheService = folderCacheService;
		this.formCacheService = formCacheService;
	}

	@GetMapping
	@WithSpan
	public String showDashboard(@RequestAttribute JwtPayload userMetadata, @RequestParam(name = "customer_session_token", required = false) String customerSessionToken, @RequestParam(required = false) String msg, Model model) {
		log.trace("Initiating dashboard view generation request routing context.");

		if (userMetadata == null || userMetadata.getSub() == null) {
			log.warn("Dashboard interception access denial rule triggered. User metadata context is completely unauthenticated.");
			return "redirect:" + PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED;
		}

		UUID tenantId = UUID.fromString(userMetadata.getSub());
		Entitlements entitlements = entitlementsCacheService.getEntitlements(tenantId);
		log.debug("Resolved entitlements for Tenant ID: {}, Service Tier: {}", tenantId, entitlements.tierName());

		List<CachedForm> forms = formCacheService.getTenantForms(tenantId);
		List<Folder> folders = folderCacheService.getTenantFolders(tenantId);
		List<FolderFormDTO> folderForms = new ArrayList<>(folders.size());

		for (Folder folder : folders) {
			List<CachedForm> folderForm = forms.stream().filter((f) -> f.folderId().equals(folder.getId())).toList();
			folderForms.add(new FolderFormDTO(folder, folderForm));
		}

		model.addAttribute("balanceLeft", polarCacheService.getCachedSubmissionBalance(tenantId));
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