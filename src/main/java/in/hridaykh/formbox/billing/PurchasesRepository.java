package in.hridaykh.formbox.billing;

import in.hridaykh.formbox.billing.model.PolarProducts;
import in.hridaykh.formbox.billing.model.Purchases;
import in.hridaykh.formbox.billing.model.SubscriptionState;
import in.hridaykh.formbox.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchasesRepository extends JpaRepository<Purchases, UUID> {

	Optional<Purchases> findByUserIdAndProduct(Tenant userId, PolarProducts product);

	void deleteByUserId_idAndProduct(UUID userId, PolarProducts polarProductsId);

	@Query("SELECT p FROM Purchases p WHERE p.userId.id = :user AND (p.status = :freeStatus OR p.status = :activeStatus OR (p.status = :cancelledStatus AND p.currentPeriodEnd > :now)) ORDER BY p.product.priceCents DESC")
	List<Purchases> findValidUserPurchases(UUID user, OffsetDateTime now, SubscriptionState freeStatus, SubscriptionState activeStatus, SubscriptionState cancelledStatus);

}