package formbox.shared.filter;

import formbox.shared.util.CloudflareIpValidator;
import formbox.shared.constant.CacheNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@Component
public class IpRateLimitFilter extends OncePerRequestFilter {

	private final StringRedisTemplate stringRedisTemplate;
	private final RedisScript<Long> rateLimiterScript = RedisScript.of(new ClassPathResource("scripts/rate_limiter.lua"), Long.class);
	private final Environment environment;
	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
		if (activeProfiles.contains("dev")) {
			filterChain.doFilter(request, response);
			return;
		} else {
			String requestUri = request.getRequestURI();
			String contextPath = request.getContextPath();
			String relativePath = requestUri.substring(contextPath.length());

			if (!pathMatcher.match("/", relativePath)) {
				response.sendRedirect(contextPath + "/");
				return;
			}
		}

		long start = System.currentTimeMillis();
		String clientIp = getClientIp(request);

		if (clientIp == null) {
			filterChain.doFilter(request, response);
			log.debug("IP rate limit check completed in {}ms", System.currentTimeMillis() - start);
			return;
		}

		String scope = "a";
		String capacity = "10";
		String refillRate = "1";

		String path = request.getRequestURI();
		if (path.startsWith("/f/") || path.startsWith("/auth/")) {
			scope = "b";
			capacity = "4";
			refillRate = "0.1";
		}

		// Removed the dangling trailing colon from the string formatter
		List<String> keys = Collections.singletonList(String.format("formbox:%s:%s:%s", CacheNames.IP_RATE_LIMIT, clientIp, scope));
		Object[] args = new Object[]{capacity, refillRate, String.valueOf(Instant.now().getEpochSecond()), "1"};

		Long result = 1L;

		try {
			result = stringRedisTemplate.execute(rateLimiterScript, keys, args);
		} catch (Exception e) {
			log.error("Redis rate limiter failed for IP: {}. Allowing request due to fallback.", clientIp, e);
		}

		if (result != null && result == 1) {
			log.debug("IP rate limit check completed in {}ms", System.currentTimeMillis() - start);
			filterChain.doFilter(request, response);
		} else {
			log.warn("IP Rate Limit exceeded for IP: {}", clientIp);

			// Handle fallback checking safely for native GET requests where Content-Type is absent
			String contentType = request.getContentType();
			String acceptHeader = request.getHeader("Accept");

			boolean isJson = (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

			handleRateLimitViolation(isJson, response);
		}
	}

	private String getClientIp(HttpServletRequest request) {
		String cfIp = request.getHeader("CF-Connecting-IP");
		if (cfIp != null && !cfIp.isBlank()) {
			String cleanCfIp = cfIp.trim();
			return isCloudflareIp(cleanCfIp) ? null : cleanCfIp;
		}

		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			String firstXffIp = xff.split(",")[0].trim();
			return isCloudflareIp(firstXffIp) ? null : firstXffIp;
		}

		String remoteAddr = request.getRemoteAddr();
		if (remoteAddr != null && isCloudflareIp(remoteAddr.trim())) {
			return null;
		}

		log.warn("No valid client IP address found in request headers.");
		return null;
	}

	private boolean isCloudflareIp(String ip) {
		return ip != null && !ip.isBlank() && CloudflareIpValidator.contains(ip);
	}

	private void handleRateLimitViolation(boolean json, HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

		if (json) {
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
		} else {
			response.setContentType(MediaType.TEXT_HTML_VALUE);
			response.getWriter().write("<h1>429 Too Many Requests</h1><p>Too many requests from your IP. Please try again later.</p>");
		}
	}
}