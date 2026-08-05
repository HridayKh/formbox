package formbox.shared;

import formbox.shared.internal.HmacKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class HmacSignerService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private final HmacKey hmacKey;

	public String sign(String payload) {

		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			SecretKeySpec secretKeySpec = new SecretKeySpec(
				hmacKey.getKey().getBytes(StandardCharsets.UTF_8),
				HMAC_ALGORITHM
			);
			mac.init(secretKeySpec);

			byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("Error generating HMAC signature", e);
		}
	}

	public boolean verify(String payload, String signature) {
		if (signature == null || payload == null) {
			return false;
		}

		String expectedSignature = sign(payload);

		return MessageDigest.isEqual(
			expectedSignature.getBytes(StandardCharsets.UTF_8),
			signature.getBytes(StandardCharsets.UTF_8)
		);
	}
}