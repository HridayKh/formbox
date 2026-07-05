package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.entity.PolarProducts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolarProductsRepository extends JpaRepository<PolarProducts, UUID> {
	Optional<PolarProducts> findByPolarProductId(String polarProductId);
	Optional<PolarProducts> findBySlug(String slug);
}