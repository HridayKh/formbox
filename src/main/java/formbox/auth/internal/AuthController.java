package formbox.auth.internal;

import formbox.shared.PathRegistry;
import formbox.shared.TurnstileAuthException;
import formbox.shared.TurnstileVerifierUtil;
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
class AuthController {

	private final AuthService authService;
	private final AuthServiceKt authServiceKt;
	private final TurnstileVerifierUtil turnstileVerifierUtil;
	private final AuthConfig authConfig;

	@GetMapping(PathRegistry.Auth.LOGIN)
	@WithSpan
	public String loginPage(@RequestParam(required = false) String msg, @RequestAttribute(required = false) JwtPayload userMetadata, HttpServletResponse response, Model model) {
		if (userMetadata != null) {
			log.debug("Active user session detected during login page evaluation. Rerouting to dashboard.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}
		if (msg != null && !msg.isBlank()) {
			log.debug("Purging client session cookies context due to explicit path trigger code: '{}'", msg);
			authService.clearAuthCookies(response);
		}
		model.addAttribute("msg", msg);
		return "auth/login";
	}

	@GetMapping(PathRegistry.Auth.SIGNUP)
	@WithSpan
	public String signupPage(@RequestAttribute(required = false) JwtPayload userMetadata) {
		log.trace("Processing HTTP GET for Signup view rendering. Active User Context: [{}]", userMetadata != null ? userMetadata.getSub() : "anonymous");
		if (userMetadata != null) {
			log.debug("Active user session detected during signup page evaluation. Rerouting to dashboard.");
			return "redirect:" + PathRegistry.DASHBOARD;
		}
		return "auth/register";
	}

	@PostMapping(PathRegistry.Auth.SIGNUP)
	@WithSpan
	public String handleSignup(@RequestParam String email, @RequestParam String password, @RequestParam(value = "termsConsent", defaultValue = "false") boolean termsConsent, @RequestParam("cf-turnstile-response") String turnstileResponse, @RequestAttribute SupabaseClient supabaseClient, HttpServletResponse response, Model model) {
		log.debug("Signing up a new user hell yeah");
		try {
			if (!termsConsent) {
				model.addAttribute("error", "You must agree to the Privacy Policy and Terms of Service to register.");
				return "auth/error-alert";
			}
			turnstileVerifierUtil.verufyTurnstileWithException(turnstileResponse, authConfig.getTurnstileSecretKey());
			authServiceKt.signUp(supabaseClient, new SignUpRequest(email, password));

			model.addAttribute("message", "Check your autoresponder for confirmation link!");
			response.setHeader("HX-Redirect", PathRegistry.Auth.LoginRedirs.LOGIN_CHECK_EMAIL);
			return "empty";
		} catch (TurnstileAuthException e) {
			model.addAttribute("error", e.getMessage());
			return "auth/error-alert";
		} catch (AuthWeakPasswordException e) {
			log.warn("Registration rejected due to weak password");
			model.addAttribute("error", "Password must be at least 8 characters long and contain uppercase, lowercase, digits, and symbols");
			return "auth/error-alert";
		} catch (Exception e) {
			log.error("critical random ahh error during signup", e);
			model.addAttribute("error", "An internal processing error occurred. " + e.getClass().getName());
			return "auth/error-alert";
		}
	}

	@PostMapping(PathRegistry.Auth.LOGIN)
	@WithSpan
	public String handleLogin(
		@RequestParam String email,
		@RequestParam(required = false) String password,
		@RequestParam(value = "action", defaultValue = "password_login") String action,
		@RequestParam("cf-turnstile-response") String turnstileResponse,
		@RequestAttribute SupabaseClient supabaseClient,
		HttpServletResponse response,
		Model model) {

		log.debug("logging in user with action: {}", action);

		try {
			if ("magic_link".equalsIgnoreCase(action)) {
				turnstileVerifierUtil.verufyTurnstileWithException(turnstileResponse, authConfig.getTurnstileSecretKey());
				authServiceKt.sendLoginMagicLink(supabaseClient, email);

				model.addAttribute("message", "Magic link sent! Please check your email inbox.");
			} else {
				if (password == null || password.isBlank()) {
					model.addAttribute("error", "Password is required for password login.");
					return "auth/error-alert";
				}
				turnstileVerifierUtil.verufyTurnstileWithException(turnstileResponse, authConfig.getTurnstileSecretKey());
				authService.loginUser(supabaseClient, new LoginRequest(email, password), response);
				response.setHeader("HX-Redirect", PathRegistry.DASHBOARD);
				model.addAttribute("message", "Login Successfully!");
			}
			return "auth/success-alert";
		} catch (TurnstileAuthException e) {
			model.addAttribute("error", e.getMessage());
			return "auth/error-alert";
		} catch (InvalidCredentialsException e) {
			log.warn("Authentication failed");
			model.addAttribute("error", e.getMessage());
			return "auth/error-alert";
		} catch (Exception e) {
			log.error("Internal orchestration failure detected inside security pipeline", e);
			model.addAttribute("error", "Authentication engine service currently unavailable.");
			return "auth/error-alert";
		}
	}

	@GetMapping(PathRegistry.Auth.LOGOUT)
	@WithSpan
	public String logout(HttpServletResponse response, @CookieValue(name = "sb_token", required = false) String accessToken, @CookieValue(name = "sb_refresh", required = false) String refreshToken, @RequestAttribute SupabaseClient supabaseClient) {
		log.debug("Processing HTTP POST logout sequence. Access token present: {}, Refresh token present: {}", accessToken != null, refreshToken != null);
		authService.terminateSession(supabaseClient, accessToken, refreshToken, response);
		return "redirect:/";
	}

	@PostMapping(PathRegistry.Auth.RESEND_CONFIRMATION)
	@WithSpan
	public String resendConfirmationEmail(@RequestParam String email, @RequestParam("cf-turnstile-response") String turnstileResponse, Model model, @RequestAttribute SupabaseClient supabaseClient) {
		log.debug("re-sending confirmation email");
		try {
			turnstileVerifierUtil.verufyTurnstileWithException(turnstileResponse, authConfig.getTurnstileSecretKey());
			authServiceKt.resendConfirmationEmail(supabaseClient, email);
			model.addAttribute("message", "Confirmation validation token successfully transmitted!");
			return "auth/success-alert";
		} catch (TurnstileAuthException e) {
			log.warn("Resend confirmation blocked. Cloudflare Turnstile validation failed for autoresponder");
			model.addAttribute("error", e.getMessage());
			return "auth/error-alert";
		} catch (Exception e) {
			log.error("Unable to execute validation token re-issuance routine to target", e);
			model.addAttribute("error", "Failed to dispatch confirmation link. Please check parameters.");
			return "auth/error-alert";
		}
	}

	@PostMapping(PathRegistry.Auth.SESSION_CALLBACK)
	@ResponseBody
	@WithSpan
	public void handleSessionCallback(@RequestParam("access_token") String accessToken, @RequestParam("refresh_token") String refreshToken, @RequestAttribute SupabaseClient supabaseClient, HttpServletResponse response) {
		log.debug("auth session callback - creating signed in session");
		try {
			authService.handleOAuthCallback(supabaseClient, accessToken, refreshToken, response);
		} catch (Exception e) {
			log.error("Critical token initialization breakdown running security payload validation callback.", e);
			response.setHeader("HX-Redirect", PathRegistry.Auth.LOGIN + "?error=callback_failed");
		}
	}

	@GetMapping(PathRegistry.Auth.CALLBACK)
	@WithSpan
	public String sessionCallback() {
		log.trace("GET /auth/callback.");
		return "auth/callback";
	}
}