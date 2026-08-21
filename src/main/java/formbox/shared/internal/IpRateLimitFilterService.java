package formbox.shared.internal;

import formbox.shared.CacheNames;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
class IpRateLimitFilterService {
	private final StringRedisTemplate stringRedisTemplate;
	private final RedisScript<Long> rateLimiterScript = RedisScript.of(new ClassPathResource("scripts/rate_limiter.lua"), Long.class);
	private final Environment environment;
	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	@WithSpan
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());

		String clientIp = getClientIp(request);
		if (clientIp == null) {
			filterChain.doFilter(request, response);
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

		List<String> keys = Collections.singletonList(String.format("f:%s:%s:%s", CacheNames.IP_RATE_LIMIT, clientIp, scope));
		Object[] args = new Object[]{capacity, refillRate, String.valueOf(Instant.now().getEpochSecond()), "1"};

		Long result = 1L;
		try {
			result = stringRedisTemplate.execute(rateLimiterScript, keys, args);
		} catch (Exception e) {
			log.error("Redis rate limiter failed for IP: {}. Allowing request due to fallback.", clientIp, e);
		}

		if (result != null && result == 1) {
			filterChain.doFilter(request, response);
			return;
		}

		log.warn("IP Rate Limit exceeded for IP: {}", clientIp);

		String contentType = request.getContentType();
		String acceptHeader = request.getHeader("Accept");

		boolean isJson = (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));

		handleRateLimitViolation(isJson, response);

	}

	@WithSpan
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

	@WithSpan
	private boolean isCloudflareIp(String ip) {
		return ip != null && !ip.isBlank() && CloudflareIpValidator.contains(ip);
	}

	@WithSpan
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

@Slf4j
class CloudflareIpValidator {

	private static final List<CidrMatcher> CF_RANGES = new ArrayList<>();

	static {
		String[] CIDRs = {
			"173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
			"141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
			"197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
			"104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
			"2400:cb00::/32", "2606:4700::/32", "2803:f800::/32", "2405:b500::/32",
			"2405:8100::/32", "2a06:98c0::/29", "2c0f:f248::/32"
		};

		for (String cidr : CIDRs) {
			try {
				CF_RANGES.add(new CidrMatcher(cidr));
			} catch (Exception e) {
				log.error("Failed to parse Cloudflare CIDR: {}", cidr, e);
			}
		}
	}

	public static boolean contains(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank())
			return false;

		try {
			InetAddress targetAddress = InetAddress.getByName(ipAddress.trim());
			byte[] targetBytes = targetAddress.getAddress();

			for (CidrMatcher matcher : CF_RANGES) {
				if (matcher.matches(targetBytes)) {
					return true;
				}
			}
		} catch (UnknownHostException e) {
			return false;
		}

		return false;
	}

	private static class CidrMatcher {
		private final byte[] networkAddress;
		private final int prefixLength;

		public CidrMatcher(String cidr) throws UnknownHostException {
			String[] parts = cidr.split("/");
			this.networkAddress = InetAddress.getByName(parts[0]).getAddress();
			this.prefixLength = Integer.parseInt(parts[1]);
		}

		public boolean matches(byte[] targetAddress) {
			if (this.networkAddress.length != targetAddress.length) {
				return false;
			}

			int remainingBits = prefixLength;
			for (int i = 0; i < networkAddress.length && remainingBits > 0; i++) {
				int mask = 0xFF00 >> Math.min(remainingBits, 8);
				if ((networkAddress[i] & mask) != (targetAddress[i] & mask)) {
					return false;
				}
				remainingBits -= 8;
			}
			return true;
		}
	}
}
