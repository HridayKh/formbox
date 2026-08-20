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

	private static final Logger log = LoggerFactory.getLogger(CorsFilter.class);

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		String uri = request.getRequestURI();
		String method = request.getMethod();
		String host = request.getHeader("Host");
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String cfRay = request.getHeader("CF-Ray"); // Useful to trace Cloudflare requests

		log.info("========== [DEBUG REQUEST START] ==========");
		log.info("HTTP Method      : {}", method);
		log.info("Request URI      : {}", uri);
		log.info("Host Header      : {}", host);
		log.info("X-Forwarded-For  : {}", forwardedFor);
		log.info("CF-Ray           : {}", cfRay);

		if (uri.startsWith("/f/")) {
			log.info("Applying CORS headers for path matching /f/*");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.addHeader("Access-Control-Allow-Methods", "POST");
			response.addHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
			response.addHeader("Access-Control-Allow-Credentials", "false");
			response.addHeader("Access-Control-Max-Age", "3600");
			response.addHeader("Access-Control-Expose-Headers", "Content-Type");
		} else if (uri.contains("/assets/")) {
			log.info("ASSET REQUEST DETECTED: {}", uri);
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			log.info("Response Status  : {}", response.getStatus());
			log.info("Location Header  : {}", response.getHeader("Location")); // Captures 301/302 redirects happening downstream
			log.info("========== [DEBUG REQUEST END] ==========\n");
		}
	}
}