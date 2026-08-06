package formbox.auth.internal

import formbox.shared.CacheNames
import formbox.shared.GenericAuthException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.exception.InvalidJwtException
import io.github.jan.supabase.auth.jwt.JwtPayload
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

data class SignUpRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val userId: String, val accessToken: String, val refreshToken: String)

@Service
internal class AuthServiceKt(private val supabaseProps: AuthConfig) {

	private val log = LoggerFactory.getLogger(AuthServiceKt::class.java)

	fun createIsolatedClient() = createSupabaseClient(supabaseProps.supabaseUrl, supabaseProps.supabaseSecretKey) {
		defaultLogLevel = LogLevel.WARNING
		install(Auth.Companion) {
			autoLoadFromStorage = false
			autoSaveToStorage = false
			alwaysAutoRefresh = false
		}
	}

	fun closeIsolatedClient(client: SupabaseClient) = runBlocking {
		try {
			client.close()
		} catch (e: Exception) {
			log.warn("[KT] Failed to close Supabase client", e)
		}
	}

	@WithSpan
	fun signUp(client: SupabaseClient, request: SignUpRequest): Unit = runBlocking {
		try {
			val user: UserInfo? = client.auth.signUpWith(Email) {
				email = request.email
				password = request.password
			}
			if (user?.id == null) {
				log.error("[KT] Sign-up succeeded but no user ID was returned")
				throw GenericAuthException("Registration failed: Service did not assign a valid User UID.")
			}
			log.info("[KT] New user signed up: {}", user.id)
		} catch (e: AuthWeakPasswordException) {
			log.warn("[KT] Sign-up rejected: password too weak", e)
			throw e
		} catch (e: AuthRestException) {
			log.warn("[KT] Sign-up failed [code={}]: {}", e.errorCode, e.errorDescription, e)
			throw GenericAuthException("Registration API error: ${e.errorDescription}")
		} catch (e: Exception) {
			log.error("[KT] Sign-up failed unexpectedly", e)
			throw GenericAuthException("An unexpected registration error occurred.", e)
		}
	}

	@WithSpan
	fun resendConfirmation(client: SupabaseClient, email: String) = runBlocking {
		try {
			client.auth.resendEmail(OtpType.Email.SIGNUP, email)
		} catch (e: AuthRestException) {
			log.warn("[KT] Failed to resend confirmation [code={}]: {}", e.errorCode, e.errorDescription, e)
			throw GenericAuthException("Verification server error: ${e.errorDescription}")
		} catch (e: Exception) {
			log.error("[KT] Failed to resend confirmation", e)
			throw GenericAuthException("Failed to reissue validation autoresponder.", e)
		}
	}

	@WithSpan
	fun sendLoginMagicLink(client: SupabaseClient, userEmail: String) = runBlocking {
		try {
			client.auth.signInWith(OTP) { email = userEmail }
		} catch (e: AuthRestException) {
			log.warn("[KT] Failed to send magic link [code={}]: {}", e.errorCode, e.errorDescription, e)
			throw GenericAuthException("Verification server error: ${e.errorDescription}")
		} catch (e: Exception) {
			log.error("[KT] Failed to send magic link", e)
			throw GenericAuthException("Failed to send magic link", e)
		}
	}

	@WithSpan
	fun login(client: SupabaseClient, request: LoginRequest): AuthResponse = runBlocking {
		try {
			client.auth.signInWith(Email) {
				email = request.email
				password = request.password
			}

			val currentSession = client.auth.currentSessionOrNull()
				?: throw GenericAuthException("Session missing from authentication response engine.")

			val assignedUserId = currentSession.user?.id
				?: throw GenericAuthException("Missing User ID context in valid payload profile.")

			AuthResponse(
				userId = assignedUserId,
				accessToken = currentSession.accessToken,
				refreshToken = currentSession.refreshToken
			)
		} catch (e: AuthRestException) {
			log.warn("[KT] Login rejected [code={}]: {}", e.errorCode, e.errorDescription)
			throw InvalidCredentialsException("Invalid autoresponder/password or autoresponder unverified!")
		} catch (e: AuthSessionMissingException) {
			log.error("[KT] Login succeeded but no session was returned", e)
			throw InvalidCredentialsException("Session initialization failure. Please try again.")
		} catch (e: InvalidJwtException) {
			log.warn("[KT] Login failed: malformed token", e)
			throw InvalidCredentialsException("Authentication structure corrupted.")
		} catch (e: Exception) {
			log.error("[KT] Login failed unexpectedly", e)
			throw GenericAuthException("Authentication server encountered an unexpected error.", e)
		}
	}

	@WithSpan
	fun logout(client: SupabaseClient, accessToken: String, refreshToken: String): Unit = runBlocking {
		val dummySession = UserSession(
			accessToken = accessToken,
			refreshToken = refreshToken,
			expiresIn = 3600,
			tokenType = "Bearer",
			user = null
		)
		try {
			client.auth.importSession(dummySession)
			client.auth.signOut()
		} catch (e: Exception) {
			log.warn("[KT] Logout failed to revoke session remotely", e)
			throw e
		}
	}

	@WithSpan
	@Cacheable(value = [CacheNames.JWT_TOKEN], key = "#accessToken")
	fun getUserMetadata(client: SupabaseClient, accessToken: String?): JwtPayload? = runBlocking {
		if (accessToken.isNullOrBlank()) {
			return@runBlocking null
		}
		try {
			return@runBlocking client.auth.getClaims(accessToken).claims
		} catch (e: Exception) {
			log.warn("[KT] Failed to decode claims from access token", e)
			null
		}
	}

	@WithSpan
	fun refreshSession(client: SupabaseClient, refreshToken: String): UserSession = runBlocking {
		try {
			return@runBlocking client.auth.refreshSession(refreshToken)
		} catch (e: Exception) {
			log.warn("[KT] Failed to refresh session", e)
			throw e
		}
	}
}