package formbox.notifs.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ZeptoWebhookSignatureValidator {

	private static final Duration ACCEPTABLE_CLOCK_SKEW = Duration.ofMinutes(15);

	private final EmailProperties emailProperties;

	public boolean isValid(String rawBody, String producerSignatureHeader) {
		if (producerSignatureHeader == null || producerSignatureHeader.isBlank()) {
			log.warn("Missing producer-signature header on ZeptoMail webhook request");
			return false;
		}

		String authKey = emailProperties.webhookAuthKey();
		if (authKey == null || authKey.isBlank()) {
			log.warn("No ZeptoMail webhook auth key configured");
			return false;
		}

		try {
			Map<String, String> parts = parseHeader(producerSignatureHeader);
			long timestamp = Long.parseLong(parts.get("ts"));
			String receivedSignature = parts.get("s");
			String algorithm = parts.getOrDefault("s-algorithm", "HmacSHA256");

			long now = System.currentTimeMillis();
			if (Math.abs(now - timestamp) > ACCEPTABLE_CLOCK_SKEW.toMillis()) {
				log.warn("ZeptoMail webhook timestamp outside acceptable clock skew");
				return false;
			}

			String computedSignature = hmac(rawBody, authKey, algorithm);

			byte[] received = Base64.getDecoder().decode(receivedSignature);
			byte[] computed = Base64.getDecoder().decode(computedSignature);
			return MessageDigest.isEqual(received, computed);
		} catch (Exception e) {
			log.warn("Failed to validate ZeptoMail webhook signature", e);
			return false;
		}
	}

	private Map<String, String> parseHeader(String header) {
		Map<String, String> map = new HashMap<>();
		for (String part : header.split(";")) {
			String[] kv = part.split("=", 2);
			if (kv.length == 2) {
				map.put(kv[0].trim(), URLDecoder.decode(kv[1].trim(), StandardCharsets.UTF_8));
			}
		}
		return map;
	}

	private String hmac(String data, String secretKey, String algorithm) throws Exception {
		SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
		Mac mac = Mac.getInstance(algorithm);
		mac.init(signingKey);
		byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(rawHmac);
	}
}