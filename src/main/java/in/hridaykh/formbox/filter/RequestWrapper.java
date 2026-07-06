package in.hridaykh.formbox.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

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
