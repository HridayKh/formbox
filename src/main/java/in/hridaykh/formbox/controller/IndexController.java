package in.hridaykh.formbox.controller;

import in.hridaykh.formbox.config.ViewRegistry;
import in.hridaykh.formbox.repository.TodoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
	private final TodoRepository todoRepository;

	public IndexController(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("ab", todoRepository.findAll().getFirst());
		return ViewRegistry.index;
	}
}
