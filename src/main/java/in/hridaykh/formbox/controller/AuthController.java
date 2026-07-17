package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.LoginRequest;
import in.hridaykh.formbox.SignUpRequest;
import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.exception.TurnstileException;
import in.hridaykh.formbox.exception.auth.InvalidCredentialsException;
import in.hridaykh.formbox.service.AuthService;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException;
import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(PathRegistry.Auth.BASE)
@Slf4j
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping(PathRegistry.Auth.LOGIN)
	@WithSpan
	public String loginPage(@RequestParam(required = false) String msg, @RequestAttribute(required = false) JwtPayload userMetadata, HttpServletResponse response) {
		log.trace("Processing HTTP GET for Login view rendering. Message parameter: [{}], Active User Context: [{}]", msg, userMetadata != null ? userMetadata.getSub() : "anonymous");
		if (userMetadata != null) {
			log.debug("Active user session detected during login page evaluation. Rerouting to dashboard.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}
		authService.processLoginPage(msg, response);
		return ViewRegistry.Auth.LOGIN;
	}

	@GetMapping(PathRegistry.Auth.SIGNUP)
	@WithSpan
	public String signupPage(@RequestAttribute(required = false) JwtPayload userMetadata) {
		log.trace("Processing HTTP GET for Signup view rendering. Active User Context: [{}]", userMetadata != null ? userMetadata.getSub() : "anonymous");
		if (userMetadata != null) {
			log.debug("Active user session detected during signup page evaluation. Rerouting to dashboard.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}
		return ViewRegistry.Auth.REGISTER;
	}

	@PostMapping(PathRegistry.Auth.SIGNUP)
	@WithSpan
	public String handleSignup(@RequestParam String email, @RequestParam String password, @RequestParam("cf-turnstile-response") String turnstileResponse, @RequestAttribute SupabaseClient supabaseClient, HttpServletResponse response, Model model) {
		log.debug("Processing HTTP POST registration payload submission for email: {}", email);
		try {
			authService.registerUser(supabaseClient, new SignUpRequest(email, password), turnstileResponse, response);
			return ViewRegistry.Auth.Fragments.EMPTY;
		} catch (TurnstileException e) {
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (AuthWeakPasswordException e) {
			log.warn("Registration rejected. Security constraints failed due to weak password for email: {}", email);
			model.addAttribute("error", "Password must be at least 8 characters long and contain uppercase, lowercase, digits, and symbols");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (Exception e) {
			log.error("Critical infrastructure handling exception during registration process for email: {}", email, e);
			model.addAttribute("error", "An internal processing error occurred. " + e.getClass().getName());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping(PathRegistry.Auth.LOGIN)
	@WithSpan
	public String handleLogin(@RequestParam String email, @RequestParam String password, @RequestParam("cf-turnstile-response") String turnstileResponse, @RequestAttribute SupabaseClient supabaseClient, HttpServletResponse response, Model model) {
		log.debug("Processing HTTP POST authentication payload submission for email: {}", email);
		try {
			authService.loginUser(supabaseClient, new LoginRequest(email, password), turnstileResponse, response);
			return ViewRegistry.Auth.Fragments.EMPTY;
		} catch (TurnstileException e) {
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (InvalidCredentialsException e) {
			log.warn("Authentication failed for user: {}", email);
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (Exception e) {
			log.error("Internal orchestration failure detected inside security pipeline for email: {}", email, e);
			model.addAttribute("error", "Authentication engine service currently unavailable.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping(PathRegistry.Auth.LOGOUT)
	@WithSpan
	public String logout(HttpServletResponse response, @CookieValue(name = "sb_token", required = false) String accessToken, @CookieValue(name = "sb_refresh", required = false) String refreshToken, @RequestAttribute SupabaseClient supabaseClient) {
		log.debug("Processing HTTP POST logout sequence. Access token present: {}, Refresh token present: {}", accessToken != null, refreshToken != null);
		authService.terminateSession(supabaseClient, accessToken, refreshToken, response);
		return ViewRegistry.Auth.Fragments.EMPTY;
	}

	@PostMapping(PathRegistry.Auth.RESEND_CONFIRMATION)
	@WithSpan
	public String resend(@RequestParam String email, @RequestParam("cf-turnstile-response") String turnstileResponse, Model model, @RequestAttribute SupabaseClient supabaseClient) {
		log.debug("Processing HTTP POST request for verification email resend pipeline targeting: {}", email);
		try {
			authService.resendVerification(supabaseClient, email, turnstileResponse);

			model.addAttribute("message", "Confirmation validation token successfully transmitted!");
			return ViewRegistry.Auth.Fragments.SUCCESS_ALERT;
		} catch (TurnstileException e) {
			log.warn("Resend confirmation blocked. Cloudflare Turnstile validation failed for email: {}", email);
			model.addAttribute("error", e.getMessage());
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		} catch (Exception e) {
			log.error("Unable to execute validation token re-issuance routine to target: {}", email, e);
			model.addAttribute("error", "Failed to dispatch confirmation link. Please check parameters.");
			return ViewRegistry.Auth.Fragments.ERROR_ALERT;
		}
	}

	@PostMapping(PathRegistry.Auth.SESSION_CALLBACK)
	@ResponseBody
	@WithSpan
	public void handleSessionCallback(@RequestParam("access_token") String accessToken, @RequestParam("refresh_token") String refreshToken, @RequestParam("expires_in") int expiresInSeconds, @RequestAttribute SupabaseClient supabaseClient, HttpServletResponse response) {
		log.debug("Processing HTTP POST OAuth session callback hook. Evaluated expiration lifecycle limit: {}s", expiresInSeconds);
		try {
			authService.handleOAuthCallback(supabaseClient, accessToken, refreshToken, expiresInSeconds, response);
		} catch (Exception e) {
			log.error("Critical token initialization breakdown running security payload validation callback.", e);
			response.setHeader("HX-Redirect", PathRegistry.Auth.LOGIN + "?error=callback_failed");
		}
	}

	@GetMapping(PathRegistry.Auth.CALLBACK)
	@WithSpan
	public String sessionCallback() {
		log.trace("Processing HTTP GET for standard OAuth redirect UI interceptor view rendering.");
		return ViewRegistry.Auth.CALLBACK;
	}
}