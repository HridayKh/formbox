package formbox.core.repository;

import formbox.core.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
	List<Folder> findAllByTenantId(UUID tenantId);
}
