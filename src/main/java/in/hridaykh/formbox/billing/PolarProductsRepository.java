package in.hridaykh.formbox.billing;

import in.hridaykh.formbox.billing.model.PolarProducts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolarProductsRepository extends JpaRepository<PolarProducts, UUID> {
	Optional<PolarProducts> findByPolarProductId(String polarProductId);
	Optional<PolarProducts> findBySlug(String slug);
}