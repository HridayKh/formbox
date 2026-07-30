package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.model.entity.Folder;
import in.hridaykh.formbox.repository.FolderRepository;
import in.hridaykh.formbox.repository.TenantRepository;
import in.hridaykh.formbox.service.cache.FolderCacheService;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
public class FolderController {
	private final FolderCacheService folderCacheService;
	private final TenantRepository tenantRepository;
	private final FolderRepository folderRepository;

	@PostMapping
	@WithSpan
	public String createFolder(@RequestAttribute JwtPayload userMetadata, @RequestParam String folderName) {
		if (folderName == null || folderName.isBlank()) {
			return "redirect:/dashboard?msg=Empty folder name not allowed!";
		}
		log.debug("Created folder: {} for user {}", folderName, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Folder newFolder = new Folder();
		newFolder.setName(folderName);
		newFolder.setTenant(tenantRepository.getReferenceById(tenantId));

		Folder savedFolder = folderRepository.save(newFolder);
		log.info("Successfully persisted new Folder Entity. ID: {} for tenant ID: {}", savedFolder.getId(), tenantId);

		folderCacheService.evictTenantFolders(tenantId);

		return "redirect:/dashboard?msg=Successfully created the folder " + folderName;
	}

	@PostMapping("/{folderId}/rename")
	@WithSpan
	@Transactional
	public String renameFolder(@RequestAttribute JwtPayload userMetadata, @RequestParam String newName, @PathVariable UUID folderId) {
		if (newName == null || newName.isBlank()) {
			return "redirect:/dashboard?msg=Empty folder name not allowed!";
		}
		log.debug("Renaming folder: {} for user {}", folderId, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Optional<Folder> folderOpt = folderRepository.findById(folderId);
		if (folderOpt.isEmpty())
			return "redirect:/dashboard?msg=Folder not found!";
		Folder folder = folderOpt.get();
		folder.setName(newName);

		Folder savedFolder = folderRepository.save(folder);
		log.info("Successfully renamed Folder Entity. ID: {} for tenant ID: {}", savedFolder.getId(), tenantId);

		folderCacheService.evictTenantFolders(tenantId);

		return "redirect:/dashboard?msg=Successfully renamed the folder to " + newName;
	}

	@PostMapping("/{folderId}/delete")
	@WithSpan
	@Transactional
	public String deleteFolder(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID folderId) {
		log.debug("Deleting folder: {} for user {}", folderId, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Optional<Folder> folderOpt = folderRepository.findById(folderId);
		if (folderOpt.isEmpty())
			return "redirect:/dashboard?msg=Folder not found!";

		folderRepository.deleteById(folderId);

		log.info("Successfully deleted Folder Entity. ID: {} for tenant ID: {}", folderId, tenantId);

		folderCacheService.evictTenantFolders(tenantId);

		return "redirect:/dashboard?msg=Successfully deleted the folder " + folderOpt.get().getName();
	}

}