package in.hridaykh.formbox.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import in.hridaykh.formbox.billing.model.PolarBenefitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sh.polar.sdk.Polar;
import sh.polar.sdk.http.PolarHttpClient;
import sh.polar.sdk.models.common.PolarListResponse;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TestController {

	private final Polar polar;
	private final PolarHttpClient polarHttpClient;

	@GetMapping("/test")
	public PolarBenefitResponse test() {
		PolarListResponse<PolarBenefitResponse> benifits = polarHttpClient.get("/benefits/?page=1&limit=10", new TypeReference<>() {
		});
//		polar.customers().getStateByExternalId("").
		return benifits.items().getFirst();
	}

}
