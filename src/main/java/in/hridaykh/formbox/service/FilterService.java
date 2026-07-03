package in.hridaykh.formbox.service;

import in.hridaykh.formbox.model.entity.Form;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FilterService {

	public boolean checkIsSpam(Form form, Map<String, String> payload, String clientIp, String userAgent) {
		return false;
	}
}

