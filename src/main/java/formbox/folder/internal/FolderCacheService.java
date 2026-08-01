package formbox.folder.internal;

import formbox.folder.FolderDto;
import formbox.shared.CacheNames;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class FolderCacheService {

	private final FolderRepository folderRepository;
	private final RedisCache redisCache;

	@CacheEvict(value = CacheNames.TENANT_FOLDERS, key = "#tenantId.toString()")
	@WithSpan
	public void evictTenantFolders(UUID tenantId) {
		redisCache.delete(CacheNames.TENANT_FOLDERS, tenantId.toString());
	}

	@Cacheable(value = CacheNames.FOLDER_METADATA, key = "#folderId.toString()")
	@WithSpan
	public Optional<FolderDto> getFolderById(UUID folderId) {
		return redisCache.getOrCompute(CacheNames.FOLDER_METADATA, folderId.toString(), new TypeReference<>() {
		}, () -> folderRepository.findFolderDtoById(folderId));
	}

	@CacheEvict(value = CacheNames.FOLDER_METADATA, key = "#folderId.toString()")
	@WithSpan
	public void evictFolderById(UUID folderId) {
		redisCache.delete(CacheNames.FOLDER_METADATA, folderId.toString());
	}
}