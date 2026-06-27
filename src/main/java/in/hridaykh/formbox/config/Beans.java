package in.hridaykh.formbox.config;

import io.github.jan.supabase.SupabaseClient;
import in.hridaykh.formbox.SupabaseBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Beans {
	private SupabaseProperties sp;

	public Beans(SupabaseProperties sp) {
		this.sp = sp;
	}

	@Bean
	public SupabaseClient supabaseClient() {
		return SupabaseBridge.createClient(sp.url, sp.secretKey);
	}
}