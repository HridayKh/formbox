package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.config.ViewRegistry;
import in.hridaykh.formbox.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
	private final EmailService emailService;


	public IndexController(EmailService emailService) {
		this.emailService = emailService;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("ab", "hi");
		return ViewRegistry.index;
	}

	@GetMapping("/e")
	public String emails(Model model) {
		model.addAttribute("ab", emailService.listEmails());
		return ViewRegistry.index;
	}

}
