package in.hridaykh.formbox.filter;

import in.hridaykh.formbox.AuthServiceKt;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.service.AuthService;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.auth.user.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

// ... keeping your imports ...

@Component
public class SupabaseSessionFilter extends OncePerRequestFilter {

	private final Logger log = LoggerFactory.getLogger(SupabaseSessionFilter.class);
	private final AuthService authService;
	private final AuthServiceKt authServiceKt;

	public SupabaseSessionFilter(AuthService authService, AuthServiceKt authServiceKt) {
		this.authService = authService;
		this.authServiceKt = authServiceKt;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();

		// 1. Skip static/public endpoints
		if (path.startsWith("/favicon")) {
			response.setStatus(404);
			return;
		}
		if (path.startsWith("/assets/") || path.startsWith("/f/") || path.startsWith("/polar")) {
			filterChain.doFilter(request, response);
			return;
		}

		SupabaseClient supabaseClient = authServiceKt.createIsolatedClient();
		request.setAttribute("supabaseClient", supabaseClient);

		String oldAccessToken = getCookieValue(request, "sb_token");
		String oldRefreshToken = getCookieValue(request, "sb_refresh");

		request.setAttribute("userMetadata", null);
		var userMetadata = authServiceKt.getUserMetadata(supabaseClient, oldAccessToken);

		if (userMetadata != null && userMetadata.getSub() != null) {
			request.setAttribute("userMetadata", userMetadata);
			filterChain.doFilter(request, response);
			authServiceKt.closeIsolatedClient(supabaseClient);
			return;
		}

		// 3. Path B: Expired Session / Background Token Rotation
		if (oldRefreshToken != null && !oldRefreshToken.isBlank()) {
			log.info("Access token expired/invalid. Attempting background token rotation...");
			UserSession newSession = authService.refreshUserSession(supabaseClient, oldRefreshToken);

			if (newSession != null) {
				String newAccessToken = newSession.getAccessToken();
				String newRefreshToken = newSession.getRefreshToken();

				authService.setAuthCookie(response, "sb_token", newAccessToken, (int) newSession.getExpiresIn());
				authService.setAuthCookie(response, "sb_refresh", newRefreshToken, (int) Duration.ofDays(7).toSeconds());
				log.info("OAuth token state exchange generation successful.");

				HttpServletRequest wrappedRequest = new RequestWrapper(request, newAccessToken, newRefreshToken);

				wrappedRequest.setAttribute("userMetadata", authServiceKt.getUserMetadata(supabaseClient, newAccessToken));

				filterChain.doFilter(wrappedRequest, response);
				authServiceKt.closeIsolatedClient(supabaseClient);
				return;
			}
		}

		if (path.startsWith(PathRegistry.Auth.BASE) || "/".equals(path) || path.isBlank()) {
			filterChain.doFilter(request, response);
			authServiceKt.closeIsolatedClient(supabaseClient);
			return;
		}

		authServiceKt.closeIsolatedClient(supabaseClient);
		log.warn("Unauthenticated attempt targeting protected domain path: {}", path);
		if ("true".equals(request.getHeader("HX-Request"))) {
			response.setHeader("HX-Redirect", PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED);
		} else {
			response.sendRedirect(PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED);
		}
	}

	private String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return null;
		return Arrays.stream(request.getCookies()).filter(cookie -> name.equals(cookie.getName())).map(Cookie::getValue).findFirst().orElse(null);
	}
}

class RequestWrapper extends HttpServletRequestWrapper {
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