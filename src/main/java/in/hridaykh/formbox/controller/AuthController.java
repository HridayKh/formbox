package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.AuthServiceKt;
import in.hridaykh.formbox.service.LoginRequest;
import in.hridaykh.formbox.service.SignUpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

@Controller
@RequestMapping(PathRegistry.Auth.BASE)
public class AuthController {

	private final AuthServiceKt authServiceKt;

	public AuthController(AuthServiceKt authServiceKt) {
		this.authServiceKt = authServiceKt;
	}

	@GetMapping(PathRegistry.Auth.LOGIN)
	public String loginPage(@RequestParam(name = "msg", required = false, defaultValue = "") String msg, HttpServletResponse response) {

		if (!msg.isBlank()) {
			setAuthCookie(response, "sb_token", "", 0);
			setAuthCookie(response, "sb_refresh", "", 0);
		}
		return ViewRegistry.Auth.LOGIN;
	}

	@GetMapping(PathRegistry.Auth.SIGNUP)
	public String signupPage() {
		return ViewRegistry.Auth.REGISTER;
	}

	@PostMapping(PathRegistry.Auth.SIGNUP)
	public String handleSignup(@RequestParam String email, @RequestParam String password, @RequestParam String username, HttpServletResponse response) {
		authServiceKt.signUp(new SignUpRequest(email, password, username));
		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_CHECK_EMAIL);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping(PathRegistry.Auth.LOGIN)
	public String handleLogin(@RequestParam String email, @RequestParam String password, HttpServletResponse response) {
		var auth = authServiceKt.login(new LoginRequest(email, password));

		setAuthCookie(response, "sb_token", auth.getAccessToken(), 3600);
		setAuthCookie(response, "sb_refresh", auth.getRefreshToken(), 604800);

		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.DASHBOARD);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping(PathRegistry.Auth.LOGOUT)
	public String logout(HttpServletResponse response, @CookieValue(name = "sb_token", required = false) String accessToken, @CookieValue(name = "sb_refresh", required = false) String refreshToken) {
		if (accessToken != null && refreshToken != null)
			authServiceKt.logout(accessToken, refreshToken);

		setAuthCookie(response, "sb_token", "", 0);
		setAuthCookie(response, "sb_refresh", "", 0);

		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.LOGIN_LOGGED_OUT);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping(PathRegistry.Auth.RESEND_CONFIRMATION)
	public String resend(@RequestParam String email, Model model) {
		authServiceKt.resendConfirmation(email);
		model.addAttribute("message", "Confirmation email resent successfully!");
		return ViewRegistry.Auth.Fragments.SUCCESS_ALERT;
	}

	@PostMapping(PathRegistry.Auth.SESSION_CALLBACK)
	@ResponseBody
	public void handleSessionCallback(@RequestParam("access_token") String accessToken, @RequestParam("refresh_token") String refreshToken, @RequestParam("expires_in") int expiresInSeconds, HttpServletResponse response) {
		setAuthCookie(response, "sb_token", accessToken, expiresInSeconds);
		setAuthCookie(response, "sb_refresh", refreshToken, (int) Duration.ofDays(7).toSeconds());
		response.setHeader("HX-Redirect", PathRegistry.Auth.Hx.DASHBOARD);
	}

	@GetMapping(PathRegistry.Auth.CALLBACK)
	public String sessionCallback() {
		return ViewRegistry.Auth.CALLBACK;
	}

	private void setAuthCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value == null ? "" : value);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}
}