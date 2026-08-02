package formbox.shared.internal;

import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.sentry.protocol.User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SentryUserProvider implements io.sentry.spring7.SentryUserProvider {

	@Override
	public User provideUser() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) return null;
		JwtPayload userMetadata = (JwtPayload) attrs.getRequest().getAttribute("user_metadata");
		if (userMetadata == null) return null;
		User user = new User();
		user.setId(userMetadata.getSub());
		return user;
	}
}