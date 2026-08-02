package formbox.auth.internal;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
class AuthFilter extends OncePerRequestFilter {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();
	private final AuthFilterService authFilterService;

	private static final List<String> EXCLUDED_PATHS = List.of("/favicon.ico", "/assets/**", "/f/**", "/polar/**", "/error");

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		authFilterService.doFilterInternal(request, response, filterChain);
	}

}