package formbox.auth.internal;

import formbox.shared.PathRegistry;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.github.jan.supabase.auth.user.UserSession;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthFilterService {

	private final AuthService authService;
	private final AuthServiceKt authServiceKt;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private static final List<String> OPTIONAL_PATHS = List.of("/", "/auth/**");

	@WithSpan
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();
		log.debug("Secure Route: {}", path);

		SupabaseClient supabaseClient = authServiceKt.createIsolatedClient();
		request.setAttribute("supabaseClient", supabaseClient);

		try {
			String oldAccessToken = getCookieValue(request, "sb_token");
			String oldRefreshToken = getCookieValue(request, "sb_refresh");

			request.setAttribute("userMetadata", null);

			JwtPayload userMetadata = null;
			if (oldAccessToken != null && !oldAccessToken.isBlank()) {
				try {
					userMetadata = authServiceKt.getUserMetadata(supabaseClient, oldAccessToken);
				} catch (IllegalArgumentException e) {
					log.warn("Access token structurally invalid: {}. Falling back to token rotation.", e.getMessage(), e);
				} catch (Exception e) {
					log.warn("Unexpected exception during access token processing: {}", e.getMessage(), e);
				}
			}

			if (userMetadata != null && userMetadata.getSub() != null) {
				log.debug("Valid active session");
				request.setAttribute("userMetadata", userMetadata);
				filterChain.doFilter(request, response);
				log.debug("Request done");
				return;
			}

			if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
				handleUnauthorizedRedirect(request, response, filterChain);
				return;
			}

			log.debug("Access token expired, missing, or invalid. Initializing rotation workflow using refresh token.");
			UserSession newSession;
			try {
				newSession = authServiceKt.refreshSession(supabaseClient, oldRefreshToken);
			} catch (Exception e) {
				log.warn("Unexpected error during session token rotation", e);
				authService.clearAuthCookies(response);
				handleUnauthorizedRedirect(request, response, filterChain);
				return;
			}

			String newAccessToken = newSession.getAccessToken();
			String newRefreshToken = newSession.getRefreshToken();

			authService.setAuthCookie(response, "sb_token", newAccessToken, (int) newSession.getExpiresIn());
			authService.setAuthCookie(response, "sb_refresh", newRefreshToken, (int) Duration.ofDays(7).toSeconds());

			HttpServletRequest wrappedRequest = new RequestWrapper(request, newAccessToken, newRefreshToken);
			wrappedRequest.setAttribute("userMetadata", authServiceKt.getUserMetadata(supabaseClient, newAccessToken));

			log.debug("Session successfully refreshed.");
			filterChain.doFilter(wrappedRequest, response);

		} finally {
			authServiceKt.closeIsolatedClient(supabaseClient);
		}
	}

	@WithSpan
	private void handleUnauthorizedRedirect(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (OPTIONAL_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()))) {
			filterChain.doFilter(request, response);
			return;
		}
		response.sendRedirect(PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);

	}

	@WithSpan
	private String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return null;
		return Arrays.stream(request.getCookies()).filter(cookie -> name.equals(cookie.getName())).map(Cookie::getValue).findFirst().orElse(null);
	}

	static class RequestWrapper extends HttpServletRequestWrapper {
		private final String accessToken;
		private final String refreshToken;

		public RequestWrapper(HttpServletRequest request, String accessToken, String refreshToken) {
			super(request);
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}

		@Override
		public Cookie[] getCookies() {
			Cookie[] originalCookies = super.getCookies();
			for (Cookie cookie : originalCookies) {
				if ("sb_token".equals(cookie.getName())) {
					cookie.setValue(accessToken);
				} else if ("sb_refresh".equals(cookie.getName())) {
					cookie.setValue(refreshToken);
				}
			}
			return originalCookies;
		}
	}
}
