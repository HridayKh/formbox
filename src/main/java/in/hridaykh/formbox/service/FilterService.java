package in.hridaykh.formbox.service;

import in.hridaykh.formbox.model.entity.Form;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FilterService {

	public boolean checkIsSpam(Form ignoredForm, Map<String, String> payload, String ignoredClientIp, String ignoredUserAgent) {
		String p = payload.getOrDefault("_gotcha", "");
		return p != null && !p.isBlank();
	}
}

