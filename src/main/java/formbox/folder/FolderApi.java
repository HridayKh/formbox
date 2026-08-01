package formbox.folder;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderApi {
	@WithSpan
	List<FolderDto> getTenantFolders(UUID tenantId);

	Optional<FolderDto> getFolderById(UUID folderId);
}
