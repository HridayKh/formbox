package formbox.shared.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class CorsFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String uri = request.getRequestURI();

		if (uri.startsWith("/f/")) {
			log.info("Applying CORS headers for path matching /f/*");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.addHeader("Access-Control-Allow-Methods", "POST");
			response.addHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
			response.addHeader("Access-Control-Allow-Credentials", "false");
			response.addHeader("Access-Control-Max-Age", "3600");
			response.addHeader("Access-Control-Expose-Headers", "Content-Type");
		}
			filterChain.doFilter(request, response);
	}
}
