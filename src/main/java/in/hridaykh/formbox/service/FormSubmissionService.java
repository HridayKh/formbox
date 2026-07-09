package in.hridaykh.formbox.service;

import in.hridaykh.formbox.constant.CacheNames;
import in.hridaykh.formbox.model.dto.CachedForm;
import in.hridaykh.formbox.model.entity.Submission;
import in.hridaykh.formbox.repository.FormRepository;
import in.hridaykh.formbox.repository.SubmissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FormSubmissionService {
	public void saveSpamSubmission(UUID formId, String remoteAddr, Map<String, String> payload) {

	}

	public boolean verifyTurnstile(Map<String, String> payload, String turnstileSecretKey) {
		return turnstileSecretKey != null && !turnstileSecretKey.isBlank();
	}

	public boolean checkRateLimit(UUID formId, Integer rateLimitRpm) {
		return false;
	}

	public boolean filesHaveValidMimeTypes(HttpServletRequest request) {

		return true;
	}

	public boolean vaildateFields(Map<String, String> payload, CachedForm form) {
		return true;
	}
}

