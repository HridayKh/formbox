package in.hridaykh.formbox.service;

import in.hridaykh.formbox.model.entity.Form;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FormService {

	public boolean checkIsSpam(Form form, Map<String, String> payload, String ignoredClientIp, String ignoredUserAgent) {
		String p = payload.getOrDefault("_gotcha", "");
		boolean isSpam = p != null && !p.isBlank();
		log.warn("Tenant {} got a submission on form {}({})", form.getTenant().getEmail(), form.getName(), form.getId());
		return isSpam;
	}
}

