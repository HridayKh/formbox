package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.dto.SubmissionItem;
import in.hridaykh.formbox.model.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
	List<SubmissionItem> findAllByFormId(UUID formId);

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM submissions WHERE id IN (SELECT id FROM submissions WHERE form_id = :formId LIMIT :batchSize)", nativeQuery = true)
	int deleteSubmissionsInBatch(@Param("formId") UUID formId, @Param("batchSize") int batchSize);
}