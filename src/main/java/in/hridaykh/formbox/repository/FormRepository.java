package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.entity.Form;
import in.hridaykh.formbox.model.entity.Tenant;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FormRepository extends JpaRepository<Form, UUID> {

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM forms WHERE id = :formId", nativeQuery = true)
	void hardDeleteForm(@Param("formId") UUID formId);

	@Query(value = "SELECT id FROM forms WHERE is_deleted = true LIMIT :limit", nativeQuery = true)
	List<UUID> findSoftDeletedFormIds(@Param("limit") int limit);

	List<Form> findByTenantAndIsDeletedIsFalse(Tenant tenant);
}
