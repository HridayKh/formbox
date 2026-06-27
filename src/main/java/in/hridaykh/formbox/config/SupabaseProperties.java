package in.hridaykh.formbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {
	public String url;
	public String publishabeKey;
	public String secretKey;
	public String jwksUrl;
}