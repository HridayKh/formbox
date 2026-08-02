package formbox.shared.internal;

import io.github.jan.supabase.auth.jwt.JwtPayload;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.sentry.protocol.User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SentryUserProvider implements io.sentry.spring7.SentryUserProvider {

	@Override
	@WithSpan
	public User provideUser() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) return null;

		Object userMetadata = attrs.getRequest().getAttribute("user_metadata");
		if (!(userMetadata instanceof JwtPayload jwtPayload)) return null;

		User user = new User();
		user.setId(jwtPayload.getSub());
		return user;
	}
}