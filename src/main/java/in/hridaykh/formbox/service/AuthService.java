package in.hridaykh.formbox.service;

import in.hridaykh.formbox.AuthServiceKt;
import in.hridaykh.formbox.AuthResponse;
import in.hridaykh.formbox.LoginRequest;
import in.hridaykh.formbox.SignUpRequest;
import in.hridaykh.formbox.constant.PathRegistry;
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import io.github.jan.supabase.auth.user.UserSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private final AuthServiceKt authServiceKt;

	public AuthService(AuthServiceKt authServiceKt) {
		this.authServiceKt = authServiceKt;
	}

	public void processLoginPage(String msg, HttpServletResponse response) {
		if (msg != null && !msg.isBlank()) {
			log.info("Purging client session cookies context due to path trigger code: '{}'", msg);
			clearAuthCookies(response);
		}
	}

	public void registerUser(SignUpRequest request, HttpServletResponse response) throws AuthWeakPasswordException {
		authServiceKt.signUp(request);
		log.info("Registration request completed cleanly for: {}", request.getEmail());
		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_CHECK_EMAIL);
	}

	public void authenticateUser(LoginRequest request, HttpServletResponse response) {
		AuthResponse auth = authServiceKt.login(request);

		setAuthCookie(response, "sb_token", auth.getAccessToken(), 3600);
		setAuthCookie(response, "sb_refresh", auth.getRefreshToken(), 604800);

		log.info("Assigned secure cookie contexts for verified UID payload reference: {}", auth.getUserId());
		response.setHeader("HX-Redirect", PathRegistry.DASHBOARD);
	}

	public void terminateSession(String accessToken, String refreshToken, HttpServletResponse response) {
		if (accessToken != null && refreshToken != null) {
			authServiceKt.logout(accessToken, refreshToken);
		}
		clearAuthCookies(response);
		log.info("User cookie persistence matrices successfully flushed.");
		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_LOGGED_OUT);
	}

	public void resendVerification(String email) {
		authServiceKt.resendConfirmation(email);
	}

	public void handleOAuthCallback(String accessToken, String refreshToken, int expiresInSeconds, HttpServletResponse response) {
		setAuthCookie(response, "sb_token", accessToken, expiresInSeconds);
		setAuthCookie(response, "sb_refresh", refreshToken, (int) Duration.ofDays(7).toSeconds());
		log.info("OAuth token state exchange generation successful.");
		response.setHeader("HX-Redirect", PathRegistry.DASHBOARD);
	}

	private void clearAuthCookies(HttpServletResponse response) {
		setAuthCookie(response, "sb_token", "", 0);
		setAuthCookie(response, "sb_refresh", "", 0);
	}

	public void setAuthCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value == null ? "" : value);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}

	public boolean isValidToken(String token) {
		try {
			var metadata = authServiceKt.getUserMetadata(token);

			return metadata.getSub() != null;
		} catch (Exception e) {
			log.debug("Token evaluation failed or expired: {}", e.getMessage());
			return false;
		}
	}

	public UserSession refreshUserSession(String refreshToken) {
		try {
			return authServiceKt.refreshSession(refreshToken);
		} catch (Exception e) {
			log.error("Failed to silently roll session credentials: {}", e.getMessage());
			return null;
		}
	}
}