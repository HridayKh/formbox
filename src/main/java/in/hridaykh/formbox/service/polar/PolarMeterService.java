package in.hridaykh.formbox.service.polar;

import com.fasterxml.jackson.core.type.TypeReference;
import in.hridaykh.formbox.config.PolarIdProperties;
import in.hridaykh.formbox.model.entity.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.common.PolarListResponse;
import sh.polar.sdk.models.meter.PolarCustomerMeterResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PolarMeterService {

	private static final Logger log = LoggerFactory.getLogger(PolarMeterService.class);

	private final PolarHttpClient polarHttpClient;
	private final PolarIdProperties polarIdProperties;
	private final Polar polar;

	public PolarMeterService(PolarHttpClient polarHttpClient, PolarIdProperties polarIdProperties, Polar polar) {
		this.polarHttpClient = polarHttpClient;
		this.polarIdProperties = polarIdProperties;
		this.polar = polar;
	}

	public long getRemainingSubmissionsBalance(Tenant tenant) {
		String externalUserId = tenant.getId().toString();
		try {
			String url = "/customer-meters/?external_customer_id=" + externalUserId;
			PolarListResponse<PolarCustomerMeterResponse> response = polarHttpClient.get(url, new TypeReference<>() {
			});

			Optional<PolarCustomerMeterResponse> matchedMeter = Optional.ofNullable(response).map(PolarListResponse::items).stream().flatMap(List::stream).filter(meter -> meter.meterId().toString().equalsIgnoreCase(polarIdProperties.getSubmissionMeterId())).filter(meter -> meter.balance() != null).findFirst();

			if (matchedMeter.isPresent()) {
				return matchedMeter.get().balance().longValue();
			} else {
				log.warn("No submission meter configuration initialized on Polar for user: {}", externalUserId);
				return 0L;
			}

		} catch (Exception e) {
			log.error("Failed to fetch real-time Polar CustomerMeter balance for user: {}", externalUserId, e);
			return 0L; // Fail-secure: stop actions if we can't verify limits
		}
	}

	public void reportSubmissionUsageEvent(Tenant tenant) {
		String externalUserId = tenant.getId().toString();
		try {
			polar.events().ingest(Map.of("events", List.of(Map.of("name", "form_submissions", "external_customer_id", tenant.getId()))));
			log.debug("Successfully reported 1 submission event to Polar for user: {}", externalUserId);

		} catch (Exception e) {
			log.error("Failed to report outbound usage event to Polar for user: {}", externalUserId, e);
		}
	}
}