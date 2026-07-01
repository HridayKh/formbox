package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.constant.PathRegistry;
import in.hridaykh.formbox.constant.ViewRegistry;
import in.hridaykh.formbox.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping(PathRegistry.ROOT)
public class IndexController {

	private final AuthService authService;

	public IndexController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping
	public String index(@CookieValue(name = "sb_token", required = false) String token, Model model) {
		model.addAttribute("loggedIn", token != null && !token.isBlank() && authService.isValidToken(token));
		return ViewRegistry.INDEX;
	}

	@PostMapping(PathRegistry.WAITLIST)
	public String waitlist(@RequestParam String ignoredEmail) {
		// TODO: store emails
		return ViewRegistry.Auth.Fragments.EMPTY;
	}
}
