package in.hridaykh.formbox.filter;

import in.hridaykh.formbox.AuthServiceKt;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.service.AuthService;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.auth.user.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class SupabaseSessionFilter extends OncePerRequestFilter {

	private final AuthService authService;
	private final AuthServiceKt authServiceKt;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private static final List<String> EXCLUDED_PATHS = List.of("/favicon.ico", "/assets/**", "/f/**", "/polar/**", "/error");
	private static final List<String> OPTIONAL_PATHS = List.of("/", PathRegistry.Auth.BASE + "/**");

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();
		log.debug("Executing token validation workflow for secure route: {}", path);

		SupabaseClient supabaseClient = authServiceKt.createIsolatedClient();
		request.setAttribute("supabaseClient", supabaseClient);

		try {
			String oldAccessToken = getCookieValue(request, "sb_token");
			String oldRefreshToken = getCookieValue(request, "sb_refresh");

			request.setAttribute("userMetadata", null);
			var userMetadata = authServiceKt.getUserMetadata(supabaseClient, oldAccessToken);

			if (userMetadata != null && userMetadata.getSub() != null) {
				log.debug("Valid active session discovered for user identity payload: {}", userMetadata.getSub());
				request.setAttribute("userMetadata", userMetadata);
				filterChain.doFilter(request, response);
				return;
			}

			if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
				handleUnauthorizedRedirect(request, response, filterChain);
				return;
			}

			log.debug("Access token expired or missing. Initializing rotation workflow using refresh token.");
			UserSession newSession = authService.refreshUserSession(supabaseClient, oldRefreshToken);

			if (newSession == null) {
				log.warn("Refresh token present but server validation process failed.");
				handleUnauthorizedRedirect(request, response, filterChain);
				return;
			}

			String newAccessToken = newSession.getAccessToken();
			String newRefreshToken = newSession.getRefreshToken();

			authService.setAuthCookie(response, "sb_token", newAccessToken, (int) newSession.getExpiresIn());
			authService.setAuthCookie(response, "sb_refresh", newRefreshToken, (int) Duration.ofDays(7).toSeconds());

			HttpServletRequest wrappedRequest = new RequestWrapper(request, newAccessToken, newRefreshToken);
			var freshMetadata = authServiceKt.getUserMetadata(supabaseClient, newAccessToken);
			wrappedRequest.setAttribute("userMetadata", freshMetadata);

			log.debug("Session successfully refreshed for user sub: {}", freshMetadata != null ? freshMetadata.getSub() : "unknown");

			filterChain.doFilter(wrappedRequest, response);
		} finally {
			authServiceKt.closeIsolatedClient(supabaseClient);
		}
	}

	private void handleUnauthorizedRedirect(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (OPTIONAL_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()))) {
			filterChain.doFilter(request, response);
			return;
		}

		if ("true".equals(request.getHeader("HX-Request"))) {
			log.debug("HTMX request metadata verified. Returning target redirect custom header element.");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
		} else {
			response.sendRedirect(PathRegistry.Auth.Hx.LOGIN_UNAUTHORIZED);
		}
	}

	private String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return null;
		return Arrays.stream(request.getCookies()).filter(cookie -> name.equals(cookie.getName())).map(Cookie::getValue).findFirst().orElse(null);
	}
}