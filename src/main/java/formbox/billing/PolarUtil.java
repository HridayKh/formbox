package formbox.billing;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sh.polar.sdk.models.customer.PolarCustomerResponse;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolarUtil {

	@WithSpan
	public void ensurePolarCustomerExists(String userId, String email) {
		log.trace("Verifying remote customer identity mirror layer presence with tracking external user index parameter: {}", userId);
		try {
			PolarCustomerResponse existingCustomer = polar.customers().getByExternalId(userId);
			if (existingCustomer != null && existingCustomer.id() != null) {
				log.trace("Polar customer mirror validation verified. Matching customer layout context exists for unique reference token: {}", existingCustomer.id());
				return;
			}
		} catch (Exception e) {
			log.debug("Customer matching constraint lookup by external ID failed on Polar system platform. Proceeding to instantiate automated record initialization.");
		}

		Map<String, Object> reqBody = new HashMap<>();
		reqBody.put("external_id", userId);
		reqBody.put("email", email);

		Map<String, Object> billingAddress = new HashMap<>();
		billingAddress.put("country", "IN");
		reqBody.put("billing_address", billingAddress);

		try {
			var createdCustomer = polar.customers().create(reqBody);
			log.info("Successfully created Polar customer with ID: {}", createdCustomer.id());
		} catch (Exception e) {
			log.error("Failed to push customer metadata mapping pipeline configuration record down to remote Polar payment engine architectures for target ID: {}", userId, e);
			throw e;
		}
	}
}
