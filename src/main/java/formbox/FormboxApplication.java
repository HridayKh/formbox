package formbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import sh.polar.spring.PolarAutoConfiguration;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
@Import(PolarAutoConfiguration.class)
// TODO: Make sure to pull before you push and run Modulith tests
// TODO: These 2 todos make IntelliJ stop the commit and point here for reminders
public class FormboxApplication {
	static void main(String[] args) {
		SpringApplication.run(FormboxApplication.class, args);
	}
}
