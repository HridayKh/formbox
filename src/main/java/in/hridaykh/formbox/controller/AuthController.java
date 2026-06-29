package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.AuthServiceKt;
import in.hridaykh.formbox.service.LoginRequest;
import in.hridaykh.formbox.service.SignUpRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

	private final AuthServiceKt authServiceKt;

	public AuthController(AuthServiceKt authServiceKt) {
		this.authServiceKt = authServiceKt;
	}

	@GetMapping("/login")
	public String loginPage() {
		return ViewRegistry.Auth.LOGIN;
	}

	// 2. ADD THIS TO HANDLE GET /auth/signup
	@GetMapping("/signup")
	public String signupPage() {
		return ViewRegistry.Auth.REGISTER;
	}

	@PostMapping("/signup")
	public String handleSignup(@RequestParam String email, @RequestParam String password, @RequestParam String username, HttpServletResponse response, Model model) {
		try {
			authServiceKt.signUp(new SignUpRequest(email, password, username));
			response.setHeader("HX-Redirect", "/auth/login?msg=check_email");
			return ViewRegistry.Auth.Fragments.EMPTY;
		} catch (Exception e) {
			response.setStatus(400);
			model.addAttribute("error", "Signup failed. Please try again.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping("/login")
	public String handleLogin(@RequestParam String email, @RequestParam String password, HttpServletResponse response, Model model) {
		try {
			var auth = authServiceKt.login(new LoginRequest(email, password));

			setAuthCookie(response, "sb_token", auth.getAccessToken(), 3600);
			setAuthCookie(response, "sb_refresh", auth.getRefreshToken(), 604800);

			response.setHeader("HX-Redirect", "/dashboard");
			return ViewRegistry.Auth.Fragments.EMPTY;
		} catch (Exception e) {
			response.setStatus(401);
			model.addAttribute("error", "Invalid email or password.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping("/logout")
	public String logout(HttpServletResponse response, @CookieValue(name = "sb_token", required = false) String accessToken, @CookieValue(name = "sb_refresh", required = false) String refreshToken) {

		if (accessToken != null && refreshToken != null) {
			try {
				authServiceKt.logout(accessToken, refreshToken);
			} catch (Exception ignored) {
			}
		}

		setAuthCookie(response, "sb_token", null, 0);
		setAuthCookie(response, "sb_refresh", null, 0);

		response.setHeader("HX-Redirect", "/auth/login?msg=logged_out");
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping("/resend-confirmation")
	public String resend(@RequestParam String email, Model model) {
		authServiceKt.resendConfirmation(email);
		model.addAttribute("message", "Confirmation email resent successfully!");
		return ViewRegistry.Auth.Fragments.SUCCESS_ALERT;
	}

	@PostMapping("/session-callback")
	@ResponseBody
	public void handleSessionCallback(@RequestParam("access_token") String accessToken, @RequestParam("refresh_token") String refreshToken, @RequestParam("expires_in") int expiresIn, HttpServletResponse response) {
		setAuthCookie(response, "sb_token", accessToken, expiresIn);
		setAuthCookie(response, "sb_refresh", refreshToken, 604800);
		response.setHeader("HX-Redirect", "/dashboard");
	}

	@GetMapping("/callback")
	public String sessionCallback() {
		return "auth/callback";
	}

	private void setAuthCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}
}