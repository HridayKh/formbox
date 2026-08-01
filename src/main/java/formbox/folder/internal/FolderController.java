package formbox.folder.internal;

import formbox.folder.FolderDto;
import formbox.form.FormApi;
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
class FolderController {
	private final formbox.folder.internal.FolderCacheService folderCacheService;
	private final FolderRepository folderRepository;
	private final FormApi formApi;

	@PostMapping
	@WithSpan
	public String createFolder(@RequestAttribute JwtPayload userMetadata, @RequestParam String folderName) {
		if (folderName == null || folderName.isBlank()) {
			return "redirect:/dashboard?msg=Empty folderId name not allowed!";
		}
		log.debug("Created folderId: {} for user {}", folderName, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Folder newFolder = new Folder();
		newFolder.setName(folderName);
		newFolder.setTenantId(tenantId);

		Folder savedFolder = folderRepository.save(newFolder);
		log.info("Successfully persisted new Folder Entity. ID: {} for tenant ID: {}", savedFolder.getId(), tenantId);

		folderCacheService.evictTenantFolders(tenantId);
		folderCacheService.evictFolderById(savedFolder.getId());

		return "redirect:/dashboard?msg=Successfully created the folderId " + folderName;
	}

	@PostMapping("/{folderId}/rename")
	@WithSpan
	@Transactional
	public String renameFolder(@RequestAttribute JwtPayload userMetadata, @RequestParam String newName, @PathVariable UUID folderId) {
		if (newName == null || newName.isBlank()) {
			return "redirect:/dashboard?msg=Empty folderId name not allowed!";
		}
		log.debug("Renaming folderId: {} for user {}", folderId, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Optional<Folder> folderOpt = folderRepository.findById(folderId);
		if (folderOpt.isEmpty())
			return "redirect:/dashboard?msg=Folder not found!";
		Folder folder = folderOpt.get();

		if (!folder.getTenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized rename attempt of folderId {} by user {}", folderId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid folderId";
		}

		folder.setName(newName);

		Folder savedFolder = folderRepository.save(folder);
		log.info("Successfully renamed Folder Entity. ID: {} for tenant ID: {}", savedFolder.getId(), tenantId);

		folderCacheService.evictTenantFolders(tenantId);
		folderCacheService.evictFolderById(savedFolder.getId());

		return "redirect:/dashboard?msg=Successfully renamed the folderId to " + newName;
	}

	@PostMapping("/{folderId}/delete")
	@WithSpan
	@Transactional
	public String deleteFolder(@RequestAttribute JwtPayload userMetadata, @PathVariable UUID folderId) {
		log.debug("Deleting folderId: {} for user {}", folderId, userMetadata.getSub());

		UUID tenantId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		Optional<FolderDto> folderOpt = folderCacheService.getFolderById(folderId);

		if (folderOpt.isEmpty())
			return "redirect:/dashboard?msg=Folder not found!";

		if (!folderOpt.get().tenantId().toString().equals(userMetadata.getSub())) {
			log.warn("Unauthorized delete attempt of folderId {} by user {}", folderId, userMetadata.getSub());
			return "redirect:/dashboard?msg=Invalid folderId";
		}
		int formCount = formApi.getTenantForms(tenantId).stream().filter(f -> f.folderId().equals(folderId)).toList().size();
		if (formCount > 0)
			return "redirect:/dashboard?msg=Cannot delete folder, please move or delete its forms!";

		folderRepository.deleteById(folderId);

		log.info("Successfully deleted Folder Entity. ID: {} for tenant ID: {}", folderId, tenantId);

		folderCacheService.evictTenantFolders(tenantId);

		return "redirect:/dashboard?msg=Successfully deleted the folderId " + folderOpt.get().name();
	}

}