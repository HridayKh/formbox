package in.hridaykh.formbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
@EnableAsync
public class FormboxApplication {
	static void main(String[] args) {
		SpringApplication.run(FormboxApplication.class, args);
	}
}
