package in.hridaykh.formbox.service;

import in.hridaykh.formbox.AuthServiceKt;
import in.hridaykh.formbox.AuthResponse;
import in.hridaykh.formbox.LoginRequest;
import in.hridaykh.formbox.SignUpRequest;
import in.hridaykh.formbox.config.TurnstileProperties;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.exception.TurnstileException;
import in.hridaykh.formbox.util.TurnstileVerifier;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

	private final AuthServiceKt authServiceKt;
	private final TenantService tenantService;
	private final ObjectMapper objectMapper;
	private final TurnstileProperties turnstileProperties;

	public void processLoginPage(String msg, HttpServletResponse response) {
		log.trace("Processing login page evaluation. Provided message trigger parameter: [{}]", msg);

		if (msg != null && !msg.isBlank()) {
			log.debug("Purging client session cookies context due to explicit path trigger code: '{}'", msg);
			clearAuthCookies(response);
		}
	}

	@WithSpan
	public void registerUser(SupabaseClient supabaseClient, SignUpRequest request, String turnstileResponse, HttpServletResponse response) throws AuthWeakPasswordException, TurnstileException {
		log.debug("Initiating user registration workflow for email: {}", request.getEmail());

		verifyTurnstile(turnstileResponse);
		authServiceKt.signUp(supabaseClient, request);

		log.info("Registration request completed cleanly for email: {}", request.getEmail());
		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_CHECK_EMAIL);
	}

	@WithSpan
	public void loginUser(SupabaseClient supabaseClient, LoginRequest request, String turnstileResponse, HttpServletResponse response) throws TurnstileException {
		log.debug("Initiating login for user: {}", request.getEmail());

		verifyTurnstile(turnstileResponse);
		AuthResponse auth = authServiceKt.login(supabaseClient, request);

		setAuthCookie(response, "sb_token", auth.getAccessToken(), 3600);
		setAuthCookie(response, "sb_refresh", auth.getRefreshToken(), 604800);

		var userMetadata = authServiceKt.getUserMetadata(supabaseClient, auth.getAccessToken());
		if (userMetadata != null) {
			tenantService.getOrCreateTenantWithFreeSubscription(userMetadata);
		}

		log.info("Login successful. Assigned secure cookie contexts for verified UID payload reference: {}", auth.getUserId());
		response.setHeader("HX-Redirect", PathRegistry.DASHBOARD);
	}

	@WithSpan
	public void terminateSession(SupabaseClient supabaseClient, String accessToken, String refreshToken, HttpServletResponse response) {
		log.debug("Initiating secure session termination sequence.");

		if (accessToken != null && refreshToken != null) {
			try {
				authServiceKt.logout(supabaseClient, accessToken, refreshToken);
				log.debug("Successfully invalidated active session tokens against upstream provider.");
			} catch (Exception e) {
				log.warn("Upstream session invalidation failed during logout execution. Proceeding with local cookie purge.", e);
			}
		}

		clearAuthCookies(response);
		log.info("User session successfully terminated.");
		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_LOGGED_OUT);
	}

	@WithSpan
	public void resendVerification(SupabaseClient supabaseClient, String email, String turnstileResponse) throws TurnstileException {
		log.debug("Dispatching confirmation email resend request for address: {}", email);

		verifyTurnstile(turnstileResponse);

		authServiceKt.resendConfirmation(supabaseClient, email);

		log.info("Verification email resend workflow dispatched successfully for: {}", email);
	}

	@WithSpan
	public void handleOAuthCallback(SupabaseClient supabaseClient, String accessToken, String refreshToken, int expiresInSeconds, HttpServletResponse response) {
		log.debug("Processing incoming OAuth callback payload. Setting local session cookies with expiration: {}s", expiresInSeconds);

		setAuthCookie(response, "sb_token", accessToken, expiresInSeconds);
		setAuthCookie(response, "sb_refresh", refreshToken, (int) Duration.ofDays(7).toSeconds());

		var userMetadata = authServiceKt.getUserMetadata(supabaseClient, accessToken);
		assert userMetadata != null;
		tenantService.getOrCreateTenantWithFreeSubscription(userMetadata);

		log.info("OAuth session completely established and secure cookies injected successfully.");
		response.setHeader("HX-Redirect", PathRegistry.DASHBOARD);
	}

	public void clearAuthCookies(HttpServletResponse response) {
		log.trace("Executing blanket wipe of local auth session tracking cookies.");
		setAuthCookie(response, "sb_token", "", 0);
		setAuthCookie(response, "sb_refresh", "", 0);
	}

	public void setAuthCookie(HttpServletResponse response, String name, String value, int maxAge) {
		log.trace("Injecting secure cookie response header attribute -> Name: [{}], MaxAge: [{}]", name, maxAge);

		Cookie cookie = new Cookie(name, value == null ? "" : value);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}

	private void verifyTurnstile(String turnstileResponse) throws TurnstileException {
		if (turnstileResponse == null || turnstileResponse.isBlank())
			throw new TurnstileException("Security verification is missing. Please try again.");

		Map<String, String> payload = new HashMap<>();
		payload.put("cf-turnstile-response", turnstileResponse);

		if (TurnstileVerifier.turnstileFailed(payload, turnstileProperties.getSecretKey(), objectMapper)) {
			log.warn("Cloudflare Turnstile verification failed.");
			throw new TurnstileException("Security verification failed. Please try again.");
		}
	}
}