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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class SupabaseSessionFilter extends OncePerRequestFilter {

	private final AuthService authService;
	private final AuthServiceKt authServiceKt;

	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();

		if (path.startsWith("/favicon")) {
			log.trace("Favicon shortcut intercept triggered for path: {}", path);
			response.setStatus(404);
			return;
		}
		if (path.startsWith("/assets/") || path.startsWith("/f/") || path.startsWith("/polar")) {
			log.trace("Bypassing filter execution for asset/webhook static route: {}", path);
			filterChain.doFilter(request, response);
			return;
		}

		log.debug("Executing token validation workflow for route: {}", path);
		SupabaseClient supabaseClient = authServiceKt.createIsolatedClient();
		request.setAttribute("supabaseClient", supabaseClient);

		String oldAccessToken = getCookieValue(request, "sb_token");
		String oldRefreshToken = getCookieValue(request, "sb_refresh");

		request.setAttribute("userMetadata", null);
		var userMetadata = authServiceKt.getUserMetadata(supabaseClient, oldAccessToken);

		if (userMetadata != null && userMetadata.getSub() != null) {
			log.debug("Valid active session discovered for user identity payload: {}", userMetadata.getSub());
			request.setAttribute("userMetadata", userMetadata);
			filterChain.doFilter(request, response);
			authServiceKt.closeIsolatedClient(supabaseClient);
			return;
		}

		if (oldRefreshToken != null && !oldRefreshToken.isBlank()) {
			log.debug("Access token expired or missing. Initializing rotation workflow using refresh token.");
			UserSession newSession = authService.refreshUserSession(supabaseClient, oldRefreshToken);

			if (newSession != null) {
				String newAccessToken = newSession.getAccessToken();
				String newRefreshToken = newSession.getRefreshToken();

				authService.setAuthCookie(response, "sb_token", newAccessToken, (int) newSession.getExpiresIn());
				authService.setAuthCookie(response, "sb_refresh", newRefreshToken, (int) Duration.ofDays(7).toSeconds());

				HttpServletRequest wrappedRequest = new RequestWrapper(request, newAccessToken, newRefreshToken);

				var freshMetadata = authServiceKt.getUserMetadata(supabaseClient, newAccessToken);
				wrappedRequest.setAttribute("userMetadata", freshMetadata);

				log.debug("Session successfully refreshed for user sub: {}", freshMetadata != null ? freshMetadata.getSub() : "unknown");

				filterChain.doFilter(wrappedRequest, response);
				authServiceKt.closeIsolatedClient(supabaseClient);
				return;
			}
			log.warn("Refresh token present but server validation process failed.");
		}

		if (path.startsWith(PathRegistry.Auth.BASE) || "/".equals(path) || path.isBlank()) {
			log.debug("Allowing unauthenticated access path through filter chain context: {}", path);
			filterChain.doFilter(request, response);
			authServiceKt.closeIsolatedClient(supabaseClient);
			return;
		}

		authServiceKt.closeIsolatedClient(supabaseClient);
		log.warn("Unauthenticated attempt targeting protected domain path: {}", path);
		if ("true".equals(request.getHeader("HX-Request"))) {
			log.debug("HTMX request metadata verified. Returning target redirect custom header element.");
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