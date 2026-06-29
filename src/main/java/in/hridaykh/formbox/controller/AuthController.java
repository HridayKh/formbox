package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.service.AuthServiceKt;
import in.hridaykh.formbox.service.LoginRequest;
import in.hridaykh.formbox.service.SignUpRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

	private final AuthServiceKt authServiceKt;

	public AuthController(AuthServiceKt authServiceKt) {
		this.authServiceKt = authServiceKt;
	}

	@GetMapping("/signup")
	public String signUpPage() {
		return "register";
	}

	@PostMapping("/signup")
	public String handleSignup(@RequestParam String email, @RequestParam String password, @RequestParam String username) {
		authServiceKt.signUp(new SignUpRequest(email, password, username));
		return "redirect:/login?msg=check_email";
	}

	@PostMapping("/resend-confirmation")
	public String resend(@RequestParam String email) {
		authServiceKt.resendConfirmation(email);
		return "redirect:/login?msg=sent";
	}

	@PostMapping("/api/auth/handle-redirect")
	@ResponseBody
	public void handleRedirect(@RequestBody String fragment, HttpServletResponse response) {
		Cookie cookie = new Cookie("sb_token", parseToken(fragment, "access_token"));
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		response.addCookie(cookie);
		Cookie refreshCookie = new Cookie("sb_refresh", parseToken(fragment, "refresh_token"));
		refreshCookie.setHttpOnly(true);
		refreshCookie.setSecure(true);
		refreshCookie.setPath("/");
		response.addCookie(refreshCookie);
	}

	@GetMapping("/login")
	public String loginPage() {
		return "login";
	}

	@PostMapping("/login")
	public String handleLogin(@RequestParam String email, @RequestParam String password, HttpServletResponse response) {
		var auth = authServiceKt.login(new LoginRequest(email, password));

		Cookie accessCookie = new Cookie("sb_token", auth.getAccessToken());
		accessCookie.setHttpOnly(true);
		accessCookie.setSecure(true);
		accessCookie.setPath("/");
		response.addCookie(accessCookie);

		Cookie refreshCookie = new Cookie("sb_refresh", auth.getRefreshToken());
		refreshCookie.setHttpOnly(true);
		refreshCookie.setSecure(true);
		refreshCookie.setPath("/");
		response.addCookie(refreshCookie);

		return "redirect:/dashboard";
	}

	@GetMapping("/dashboard")
	public String dashboard(@CookieValue(name = "sb_token", required = false) String token) {
		if (token == null) return "redirect:/login";
		return "dashboard";
	}

	@GetMapping("/me")
	@ResponseBody
	public ResponseEntity<?> getProfile(@CookieValue(name = "sb_token", required = false) String token, HttpServletResponse response) {
		try {
			if (token == null) return ResponseEntity.status(401).build();
			return ResponseEntity.ok(authServiceKt.getUserMetadata(token));
		} catch (Exception e) {
			Cookie cookie = new Cookie("sb_token", null);
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			return ResponseEntity.status(401).body("Session expired");
		}
	}

	@GetMapping("/logout")
	public String logout(HttpServletResponse response, @CookieValue(name = "sb_token", required = false) String accessToken, @CookieValue(name = "sb_refresh", required = false) String refreshToken) {

		// 1. If we have the tokens, tell Supabase to invalidate them
		if (accessToken != null && refreshToken != null) {
			try {
				authServiceKt.logout(accessToken, refreshToken);
			} catch (Exception e) {
				// Log the error but proceed with clearing cookies anyway
			}
		}

		// 2. Clear both cookies
		Cookie accessCookie = new Cookie("sb_token", null);
		accessCookie.setMaxAge(0);
		accessCookie.setPath("/");
		response.addCookie(accessCookie);

		Cookie refreshCookie = new Cookie("sb_refresh", null);
		refreshCookie.setMaxAge(0);
		refreshCookie.setPath("/");
		response.addCookie(refreshCookie);

		return "redirect:/login?msg=logged_out";
	}

	private String parseToken(String fragment, String key) {
		// Splits the string by '&' then by '=' to find the value for the key
		for (String part : fragment.split("&")) {
			String[] pair = part.split("=");
			if (pair.length == 2 && pair[0].equals(key)) {
				return pair[1];
			}
		}
		return null;
	}
}