package in.hridaykh.formbox.config;

import in.hridaykh.formbox.filter.SupabaseSessionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class FilterConfig {

	@Bean
	public FilterRegistrationBean<SupabaseSessionFilter> loggingFilter(SupabaseSessionFilter sessionFilter) {
		log.info("Initializing SupabaseSessionFilter registration...");

		FilterRegistrationBean<SupabaseSessionFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(sessionFilter);

		List<String> urlPatterns = Arrays.asList("/", "/*", "/**");
		registrationBean.setUrlPatterns(urlPatterns);

		log.debug("SupabaseSessionFilter mapped to URL patterns: {}", urlPatterns);
		log.info("SupabaseSessionFilter registration complete.");
		return registrationBean;
	}
}