package in.hridaykh.formbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// Keeps your controller clean of simple static view mappings
		registry.addViewController("/login").setViewName("login");
		registry.addViewController("/signup").setViewName("register");
		registry.addViewController("/dashboard").setViewName("dashboard");
	}
}