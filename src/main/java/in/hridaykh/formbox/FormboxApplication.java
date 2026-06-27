package in.hridaykh.formbox;

import in.hridaykh.formbox.config.SupabaseProperties;
import io.github.jan.supabase.auth.Auth;
import io.github.jan.supabase.network.SupabaseApi;
import io.github.jan.supabase.network.SupabaseApiKt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SupabaseProperties.class)
public class FormboxApplication {

	static void main(String[] args) {
		SpringApplication.run(FormboxApplication.class, args);
	}

}
