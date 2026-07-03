package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
}