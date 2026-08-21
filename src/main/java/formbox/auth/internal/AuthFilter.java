package formbox.auth.internal;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
@Component
class AuthFilter extends OncePerRequestFilter {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();
	private final AuthFilterService authFilterService;

	private static final List<String> EXCLUDED_PATHS = List.of("/f/**", "/favicon.ico", "/assets/**", "/pages/**", "/c/**", "/error", "/webhooks/**");

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		String path = request.getRequestURI();

		if (path.startsWith("/assets/") || path.startsWith("/f/") || path.startsWith("/pages/") || path.startsWith("/c/") || path.startsWith("/webhooks/")) {
			return true;
		}

		return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		authFilterService.doFilterInternal(request, response, filterChain);
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
	@WithSpan
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
