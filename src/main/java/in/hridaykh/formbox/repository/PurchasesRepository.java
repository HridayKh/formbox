package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.entity.PolarProducts;
import in.hridaykh.formbox.model.entity.Purchases;
import in.hridaykh.formbox.model.entity.Tenant;
import in.hridaykh.formbox.model.enums.SubscriptionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchasesRepository extends JpaRepository<Purchases, UUID> {

	Optional<Purchases> findByUserIdAndProduct(Tenant userId, PolarProducts product);

	boolean existsByUserId(Tenant userId);

	void deleteByUserIdAndProduct(Tenant userId, PolarProducts polarProductsId);

	@Query("SELECT p FROM Purchases p WHERE p.userId = :user AND (p.status = :activeState OR (p.status = :canceledState AND p.currentPeriodEnd > :now)) ORDER BY p.product.priceCents DESC")
	List<Purchases> findActiveOrGracePurchasesByUserId(@Param("user") Tenant user, @Param("activeState") SubscriptionState activeState, @Param("canceledState") SubscriptionState canceledState, @Param("now") OffsetDateTime now);
}