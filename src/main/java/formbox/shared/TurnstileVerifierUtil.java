package formbox.shared;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnstileVerifierUtil {

	private static final HttpClient httpClient = HttpClient.newBuilder().build();
	private static final String CLOUDFLARE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

	private final ObjectMapper objectMapper;

	@WithSpan
	public boolean turnstileFailed(Map<String, String> payload, String turnstileSecretKey) {
		String turnstileCode = payload.getOrDefault("cf-turnstile-response", "");
		payload.remove("cf-turnstile-response");

		if (turnstileSecretKey == null || turnstileSecretKey.isBlank()) {
			log.debug("Turnstile validation skipped due to missing secret key.");
			return false;
		}

		if (turnstileCode.isBlank())
			return true;

		try {
			String formData = String.format("secret=%s&response=%s", URLEncoder.encode(turnstileSecretKey, StandardCharsets.UTF_8), URLEncoder.encode(turnstileCode, StandardCharsets.UTF_8));

			HttpRequest.Builder reqBuilder = HttpRequest.newBuilder();
			reqBuilder.uri(URI.create(CLOUDFLARE_VERIFY_URL));
			reqBuilder.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
			reqBuilder.POST(HttpRequest.BodyPublishers.ofString(formData));
			HttpRequest request = reqBuilder.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				String body = response.body();
				return body == null || !objectMapper.readTree(body).get("success").asBoolean();
			} else {
				log.warn("Cloudflare Turnstile API returned unexpected status code: {}", response.statusCode());
			}

		} catch (Exception e) {
			log.warn("Exception occurred while communicating with Cloudflare Turnstile API", e);
		}

		return false;
	}

	@WithSpan
	public void verufyTurnstileWithException(String turnstileResponse, String turnstileSecretKey) throws formbox.shared.TurnstileAuthException {
		if (turnstileResponse == null || turnstileResponse.isBlank())
			throw new TurnstileAuthException("Security verification is missing. Please try again.");

		Map<String, String> payload = new HashMap<>();
		payload.put("cf-turnstile-response", turnstileResponse);

		if (turnstileFailed(payload, turnstileSecretKey)) {
			log.warn("Cloudflare Turnstile verification failed.");
			throw new TurnstileAuthException("Security verification failed. Please try again.");
		}
	}

}