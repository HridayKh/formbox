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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

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
		long start = System.currentTimeMillis();
		String path = request.getRequestURI();
		String method = request.getMethod();

		if (path.startsWith("/favicon")) {
			response.setStatus(404);
			return;
		}
		if (path.startsWith("/assets/") || path.startsWith("/f/") || path.startsWith("/polar")) {
			filterChain.doFilter(request, response);
			log.info("{} {}: {}", method, path, System.currentTimeMillis() - start);
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
			log.info("{} {}: {}", method, path, System.currentTimeMillis() - start);
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
				log.info("{} {}: {}", method, path, System.currentTimeMillis() - start);
				return;
			}
		}

		if (path.startsWith(PathRegistry.Auth.BASE) || "/".equals(path) || path.isBlank()) {
			filterChain.doFilter(request, response);
			authServiceKt.closeIsolatedClient(supabaseClient);
			log.info("{} {}: {}", method, path, System.currentTimeMillis() - start);
			return;
		}

		authServiceKt.closeIsolatedClient(supabaseClient);
		log.warn("Unauthenticated attempt targeting protected domain path: {}", path);
		if ("true".equals(request.getHeader("HX-Request"))) {
			response.setHeader("HX-Redirect", PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED);
			log.info("{} {}: {}", method, path, System.currentTimeMillis() - start);
		} else {
			response.sendRedirect(PathRegistry.Auth.Redirects.TO_LOGIN_UNAUTHORIZED);
			log.info("{} {}: {}", method, path, System.currentTimeMillis() - start);
		}
	}

	private String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return null;
		return Arrays.stream(request.getCookies()).filter(cookie -> name.equals(cookie.getName())).map(Cookie::getValue).findFirst().orElse(null);
	}
}
