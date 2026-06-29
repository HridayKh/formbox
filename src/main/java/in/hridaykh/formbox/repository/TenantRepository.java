package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}