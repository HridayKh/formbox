package formbox.folder.internal;

import formbox.folder.FolderDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FolderRepository extends JpaRepository<Folder, UUID> {
	List<FolderDto> findAllByTenantId(UUID tenantId);

	Optional<FolderDto> findFolderDtoById(UUID id);
}
