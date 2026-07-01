package in.hridaykh.formbox.config;

import in.hridaykh.formbox.filter.SupabaseSessionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class FilterConfig {

	@Bean
	public FilterRegistrationBean<SupabaseSessionFilter> loggingFilter(SupabaseSessionFilter sessionFilter) {
		FilterRegistrationBean<SupabaseSessionFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(sessionFilter);
		registrationBean.setUrlPatterns(Arrays.asList("/", "/*", "/**"));
		return registrationBean;
	}
}