package formbox.repository;

import formbox.model.entity.Folder;
import formbox.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
	List<Folder> findAllByTenant(Tenant tenant);
}
