package formbox.auth.internal;

import formbox.auth.TenantApi;
import formbox.shared.PathRegistry;
import formbox.shared.TurnstileAuthException;
import io.github.jan.supabase.SupabaseClient;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
class AuthService {

	private final AuthServiceKt authServiceKt;
	private final TenantApi tenantApi;

	@WithSpan
	public void loginUser(SupabaseClient supabaseClient, LoginRequest request, HttpServletResponse response) throws TurnstileAuthException {
		log.debug("Initiating login for user");

		AuthResponse auth = authServiceKt.login(supabaseClient, request);

		setAuthCookies(response, auth.getRefreshToken(), auth.getAccessToken());

		log.info("Login successful. Assigned secure cookie contexts for verified UID payload reference: {}", auth.getUserId());
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
		response.setHeader("HX-Redirect", PathRegistry.Auth.LoginRedirs.LOGIN_LOGGED_OUT);
	}

	@WithSpan
	public void handleOAuthCallback(SupabaseClient supabaseClient, String accessToken, String refreshToken, HttpServletResponse response) {
		log.debug("Processing incoming OAuth callback payload. Setting local session cookies with expiration");

		setAuthCookies(response, refreshToken, accessToken);

		var userMetadata = authServiceKt.getUserMetadata(supabaseClient, accessToken);
		assert userMetadata != null;

		UUID userId = UUID.fromString(Objects.requireNonNull(userMetadata.getSub()));

		tenantApi.createTenant(userId, userMetadata.getEmail());

		log.info("OAuth session completely established and secure cookies injected successfully.");
		response.setHeader("HX-Redirect", PathRegistry.DASHBOARD);
	}

	@WithSpan
	public void setAuthCookies(HttpServletResponse response, String refreshToken, String accessToken) {
		setAuthCookie(response, "sb_refresh", refreshToken, (int) Duration.ofDays(7).toSeconds());
		setAuthCookie(response, "sb_token", accessToken, (int) Duration.ofHours(1).toSeconds());
	}

	@WithSpan
	public void clearAuthCookies(HttpServletResponse response) {
		setAuthCookie(response, "sb_refresh", "", 0);
		setAuthCookie(response, "sb_token", "", 0);
	}

	private void setAuthCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value == null ? "" : value);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}


}