package in.hridaykh.formbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FormboxApplication {
	static void main(String[] args) {
		SpringApplication.run(FormboxApplication.class, args);
	}
}
