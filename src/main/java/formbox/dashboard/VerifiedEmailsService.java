package formbox.dashboard;

import formbox.auth.TenantApi;
import formbox.notifs.EmailApi;
import formbox.shared.CacheNames;
import formbox.shared.RedisCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class VerifiedEmailsService {

	private final RedisCache redisCache;
	private final TenantApi tenantApi;
	private final EmailApi emailApi;

	private static final Duration VERIFY_CODE_TTL = Duration.ofHours(24);
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	public record EmailVerifyPayload(UUID tenantId, String email) implements Serializable {}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.strip().toLowerCase();
	}

	@WithSpan
	public List<String> getVerifiedEmails(UUID tenantId) {
		return redisCache.getOrCompute(
			CacheNames.TENANT_EMAILS,
			tenantId.toString(),
			STRING_LIST_TYPE,
			() -> tenantApi.getVerifiedEmails(tenantId)
		);
	}

	@WithSpan
	public void sendVerificationEmail(UUID tenantId, String email) {
		String cleanEmail = normalizeEmail(email);
		if (cleanEmail.isBlank()) return;

		String token = UUID.randomUUID().toString().replace("-", "");
		EmailVerifyPayload payload = new EmailVerifyPayload(tenantId, cleanEmail);
		redisCache.set(CacheNames.EMAIL_VERIFY_CODE, token, payload, VERIFY_CODE_TTL);

		String code = generateCode();
		String codeKey = tenantId + ":" + cleanEmail;
		redisCache.set(CacheNames.EMAIL_VERIFY_CODE, codeKey, code, VERIFY_CODE_TTL);

		String verifyUrl = "https://formbox.hridaykh.in/dashboard/emails-verify?token=" + token;

		String htmlBody = "<h2>Verify your email for Formbox</h2>"
			+ "<p>Click the link below to verify your email address for use with Formbox notifications:</p>"
			+ "<p><a href=\"" + verifyUrl + "\">Verify Email Address</a></p>"
			+ "<p>Or copy this link: " + verifyUrl + "</p>"
			+ "<p><small>This link expires in 24 hours.</small></p>"
			+ "<br/><p>-- Formbox</p>";

		emailApi.sendGenericEmail(cleanEmail, "Verify your email address — Formbox", htmlBody, "Formbox", "no-reply");
		log.info("Verification email dispatched for tenant ID: {}", tenantId);
	}

	@WithSpan
	public boolean verifyEmail(UUID tenantId, String token, String email, String code) {
		// 1. Primary verification via unique token
		if (token != null && !token.isBlank()) {
			String cleanToken = token.strip();
			var payloadOpt = redisCache.get(CacheNames.EMAIL_VERIFY_CODE, cleanToken, EmailVerifyPayload.class);
			if (payloadOpt.isPresent()) {
				EmailVerifyPayload payload = payloadOpt.get();
				if (payload.tenantId().equals(tenantId)) {
					tenantApi.addVerifiedEmail(payload.tenantId(), payload.email());
					redisCache.delete(CacheNames.EMAIL_VERIFY_CODE, cleanToken);
					redisCache.delete(CacheNames.EMAIL_VERIFY_CODE, tenantId + ":" + payload.email());
					redisCache.delete(CacheNames.TENANT_EMAILS, payload.tenantId().toString());
					log.info("Email verified successfully via token for tenant ID: {}", tenantId);
					return true;
				} else {
					log.warn("Email verification failed for tenant ID {}: token belongs to another tenant", tenantId);
					return false;
				}
			}
		}

		// 2. Secondary fallback via email + code
		String cleanEmail = normalizeEmail(email);
		String cleanCode = (code != null) ? code.strip() : "";

		if (!cleanEmail.isBlank() && !cleanCode.isBlank()) {
			String redisKey = tenantId + ":" + cleanEmail;
			var storedOpt = redisCache.get(CacheNames.EMAIL_VERIFY_CODE, redisKey, String.class);

			if (storedOpt.isPresent()) {
				String storedCode = storedOpt.get().replace("\"", "").strip();
				if (storedCode.equals(cleanCode)) {
					tenantApi.addVerifiedEmail(tenantId, cleanEmail);
					redisCache.delete(CacheNames.EMAIL_VERIFY_CODE, redisKey);
					redisCache.delete(CacheNames.TENANT_EMAILS, tenantId.toString());
					log.info("Email verified successfully via code for tenant ID: {}", tenantId);
					return true;
				}
			}
		}

		log.warn("Email verification failed for tenant ID: {}: token or code invalid or expired", tenantId);
		return false;
	}

	@WithSpan
	public void removeEmail(UUID tenantId, String email) {
		String cleanEmail = normalizeEmail(email);
		if (cleanEmail.isBlank()) return;

		String redisKey = tenantId + ":" + cleanEmail;
		tenantApi.removeVerifiedEmail(tenantId, cleanEmail);
		redisCache.delete(CacheNames.EMAIL_VERIFY_CODE, redisKey);
		redisCache.delete(CacheNames.TENANT_EMAILS, tenantId.toString());
		log.info("Removed verified email for tenant ID: {}", tenantId);
	}

	private String generateCode() {
		int code = SECURE_RANDOM.nextInt(100000, 1000000);
		return String.valueOf(code);
	}
}
