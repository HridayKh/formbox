package in.hridaykh.formbox.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class TurnstileVerifier {

	private static final HttpClient httpClient = HttpClient.newBuilder().build();
	private static final String CLOUDFLARE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

	public static boolean turnstilePassed(Map<String, String> payload, String turnstileSecretKey, ObjectMapper objectMapper) {
		String turnstileCode = payload.getOrDefault("cf-turnstile", "");

		if (turnstileCode.isBlank() || turnstileSecretKey == null || turnstileSecretKey.isBlank()) {
			log.warn("Turnstile validation skipped or failed due to missing token or secret key.");
			return false;
		}

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
				return body != null && objectMapper.readTree(body).get("success").asBoolean();
			} else {
				log.error("Cloudflare Turnstile API returned unexpected status code: {}", response.statusCode());
			}

		} catch (Exception e) {
			log.error("Exception occurred while communicating with Cloudflare Turnstile API", e);
		}

		return false;
	}
}