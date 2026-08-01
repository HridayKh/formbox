package formbox.folder.internal;

import formbox.folder.FolderApi;
import formbox.folder.FolderDto;
import formbox.shared.CacheNames;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderApiImpl implements FolderApi {

	private final FolderRepository folderRepository;
	private final RedisCache redisCache;
	private final FolderCacheService folderCacheService;

	@Cacheable(value = CacheNames.TENANT_FOLDERS, key = "#tenantId.toString()")
	@WithSpan
	@Override
	public List<FolderDto> getTenantFolders(UUID tenantId) {
		return redisCache.getOrCompute(CacheNames.TENANT_FOLDERS, tenantId.toString(), new TypeReference<>() {
		}, () -> folderRepository.findAllByTenantId(tenantId));
	}

	@Override
	public Optional<FolderDto> getFolderById(UUID folderId) {
		return folderCacheService.getFolderById(folderId);
	}
}