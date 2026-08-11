package formbox.submission.internal;

import formbox.submission.SubmissionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface SubmissionRepository extends JpaRepository<Submission, UUID> {
	List<SubmissionItem> findAllByFormIdOrderByCreatedAtDesc(UUID formId);

	@Query("SELECT COUNT(s) FROM Submission s WHERE s.tenantId = :tenantId AND s.createdAt >= :since")
	long countByTenantIdAndCreatedAtAfter(@Param("tenantId") UUID tenantId, @Param("since") OffsetDateTime since);

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM submissions WHERE id IN (SELECT id FROM submissions WHERE form_id = :formId LIMIT :batchSize)", nativeQuery = true)
	int deleteSubmissionsInBatch(@Param("id") UUID formId, @Param("batchSize") int batchSize);

	Submission findByEmailAutoresponseRequestId(String emailAutoresponseRequestId);

	Submission findByEmailNotifRequestId(String emailNotifRequestId);

	List<Submission> findAllByTenantIdAndCreatedAtBefore(UUID tenantId, OffsetDateTime cutoff);
}