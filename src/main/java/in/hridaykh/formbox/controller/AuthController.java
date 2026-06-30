package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.LoginRequest;
import in.hridaykh.formbox.SignUpRequest;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.exception.auth.InvalidCredentialsException;
import in.hridaykh.formbox.exception.auth.UserAlreadyExistsException;
import in.hridaykh.formbox.service.AuthService;
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(PathRegistry.Auth.BASE)
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping(PathRegistry.Auth.LOGIN)
	public String loginPage(@RequestParam(name = "msg", required = false, defaultValue = "") String msg, HttpServletResponse response) {
		authService.processLoginPage(msg, response);
		return ViewRegistry.Auth.LOGIN;
	}

	@GetMapping(PathRegistry.Auth.SIGNUP)
	public String signupPage() {
		return ViewRegistry.Auth.REGISTER;
	}

	@PostMapping(PathRegistry.Auth.SIGNUP)
	public String handleSignup(@RequestParam String email, @RequestParam String password, @RequestParam String username, HttpServletResponse response, Model model) {
		try {
			authService.registerUser(new SignUpRequest(email, password, username), response);
			return ViewRegistry.Auth.Fragments.EMPTY;
		} catch (UserAlreadyExistsException e) {
			log.warn("Registration rejected: Account conflict tracking email source: {}", email);
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (AuthWeakPasswordException e) {
			log.warn("Registration rejected: Validation rule exception handling password parameters.");
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (Exception e) {
			log.error("Critical infrastructure handling exception during registration process: ", e);
			model.addAttribute("error", "An internal processing error occurred.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping(PathRegistry.Auth.LOGIN)
	public String handleLogin(@RequestParam String email, @RequestParam String password, HttpServletResponse response, Model model) {
		try {
			authService.authenticateUser(new LoginRequest(email, password), response);
			return ViewRegistry.Auth.Fragments.EMPTY;
		} catch (InvalidCredentialsException e) {
			log.warn("Authentication clearance challenge failed for matching entity root: {}", email);
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (Exception e) {
			log.error("Internal orchestration failure detected inside security pipeline: ", e);
			model.addAttribute("error", "Authentication engine service currently unavailable.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping(PathRegistry.Auth.LOGOUT)
	public String logout(HttpServletResponse response, @CookieValue(name = "sb_token", required = false) String accessToken, @CookieValue(name = "sb_refresh", required = false) String refreshToken) {
		authService.terminateSession(accessToken, refreshToken, response);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping(PathRegistry.Auth.RESEND_CONFIRMATION)
	public String resend(@RequestParam String email, Model model) {
		try {
			authService.resendVerification(email);
			model.addAttribute("message", "Confirmation validation token successfully transmitted!");
			return ViewRegistry.Auth.Fragments.SUCCESS_ALERT;
		} catch (Exception e) {
			log.error("Unable to execute validation token re-issuance routine to target: {}", email, e);
			model.addAttribute("error", "Failed to dispatch confirmation link. Please check parameters.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping(PathRegistry.Auth.SESSION_CALLBACK)
	@ResponseBody
	public void handleSessionCallback(@RequestParam("access_token") String accessToken, @RequestParam("refresh_token") String refreshToken, @RequestParam("expires_in") int expiresInSeconds, HttpServletResponse response) {
		try {
			authService.handleOAuthCallback(accessToken, refreshToken, expiresInSeconds, response);
		} catch (Exception e) {
			log.error("Critical token initialization breakdown running security payload validation callback.", e);
			response.setHeader("HX-Redirect", PathRegistry.Auth.LOGIN + "?error=callback_failed");
		}
	}

	@GetMapping(PathRegistry.Auth.CALLBACK)
	public String sessionCallback() {
		return ViewRegistry.Auth.CALLBACK;
	}
}