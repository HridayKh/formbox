package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.dto.SubmissionItem;
import in.hridaykh.formbox.model.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
	List<SubmissionItem> findSubmissionsByFormIdAndIsSpam(UUID formId, Boolean isSpam);
}