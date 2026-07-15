package in.hridaykh.formbox.billing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import in.hridaykh.formbox.billing.PolarIdProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.common.PolarListResponse;
import sh.polar.sdk.models.meter.PolarCustomerMeterResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PolarMeterService {

	private final PolarHttpClient polarHttpClient;
	private final PolarIdProperties polarIdProperties;
	private final Polar polar;

	public long getRemainingSubmissionsBalance(UUID tenantId) {
		String externalUserId = tenantId.toString();
		try {
			String url = "/customer-meters/?external_customer_id=" + externalUserId;
			PolarListResponse<PolarCustomerMeterResponse> response = polarHttpClient.get(url, new TypeReference<>() {
			});

			Optional<PolarCustomerMeterResponse> matchedMeter = Optional.ofNullable(response).map(PolarListResponse::items).stream().flatMap(List::stream).filter(meter -> meter.meterId().toString().equalsIgnoreCase(polarIdProperties.getSubmissionMeterId())).filter(meter -> meter.balance() != null).findFirst();

			if (matchedMeter.isPresent()) {
				long balance = matchedMeter.get().balance().longValue();
				log.debug("Polar customer meter match verified. Balance for ID [{}]: {}", externalUserId, balance);
				return balance;
			} else {
				log.warn("No submission meter configuration initialized on Polar for user: {}", externalUserId);
				return 0L;
			}

		} catch (Exception e) {
			log.error("Failed to fetch real-time Polar CustomerMeter balance for user: {}", externalUserId, e);
			return 0L;
		}
	}

	public void reportSubmissionUsageEvent(UUID tenantId) {
		String externalUserId = tenantId.toString();
		try {
			polar.events().ingest(Map.of("events", List.of(Map.of("name", "form_submissions", "external_customer_id", tenantId))));
			log.debug("Successfully reported 1 submission event to Polar for user: {}", externalUserId);

		} catch (Exception e) {
			log.error("Failed to report outbound usage event to Polar for user: {}", externalUserId, e);
		}
	}
}