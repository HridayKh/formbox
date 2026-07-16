package in.hridaykh.formbox;

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
public class FormboxApplication {
	static void main(String[] args) {
		SpringApplication.run(FormboxApplication.class, args);
	}
}
