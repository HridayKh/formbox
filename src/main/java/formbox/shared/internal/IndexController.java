package formbox.shared.internal;

import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
class IndexController {

	@GetMapping("/")
	@WithSpan
	public String index(Model model, @RequestAttribute(required = false) JwtPayload userMetadata) {
		model.addAttribute("loggedIn", userMetadata != null && userMetadata.getSub() != null);
		return "index";
	}

	@GetMapping("/error")
	@WithSpan
	public String indexErr() {
		try {
			throw new RuntimeException("Intentional Exception!");
		} catch (RuntimeException e) {
			log.error("Runtime exception occurred!", e);
		}
		User user = new User();
		user.setId("ID-1234");
		Sentry.setUser(user);
		return "empty";
	}

}