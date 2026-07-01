package `in`.hridaykh.formbox

import `in`.hridaykh.formbox.config.SupabaseProperties
import `in`.hridaykh.formbox.exception.auth.AuthException
import `in`.hridaykh.formbox.exception.auth.InvalidCredentialsException
import `in`.hridaykh.formbox.exception.auth.SessionExpiredException
import `in`.hridaykh.formbox.exception.auth.UserAlreadyExistsException
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.exception.InvalidJwtException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.auth.exception.TokenExpiredException
import io.github.jan.supabase.auth.jwt.JwtPayload
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class SignUpRequest(val email: String, val password: String, val username: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val userId: String, val accessToken: String, val refreshToken: String)

@Service
class AuthServiceKt(private val supabaseProps: SupabaseProperties) {

	private val log = LoggerFactory.getLogger(AuthServiceKt::class.java)

	private fun createIsolatedClient() = createSupabaseClient(supabaseProps.url, supabaseProps.secretKey) {
		install(Auth.Companion) {
			autoLoadFromStorage = false
			autoSaveToStorage = false
			alwaysAutoRefresh = false
		}
	}

	fun signUp(request: SignUpRequest): Unit = runBlocking {
		val client = createIsolatedClient()
		try {
			log.info("Processing Supabase registration chain for: {}", request.email)
			val user: UserInfo? = client.auth.signUpWith(Email) {
				email = request.email
				password = request.password
				data = buildJsonObject {
					put("first_name", request.username)
				}
			}
			if (user?.id == null) {
				throw AuthException("Registration failed: Service did not assign a valid User UID.")
			}
		} catch (e: AuthWeakPasswordException) {
			log.warn("Sign-up rejected: Weak password rules unmet for target address {}", request.email)
			throw e
		} catch (e: AuthRestException) {
			log.error(
				"Supabase engine error during user registration [Error Code: {}]: {}",
				e.errorCode,
				e.errorDescription
			)
			if (e.errorDescription.contains("already registered", ignoreCase = true)) {
				throw UserAlreadyExistsException("An account with this email already exists.")
			}
			throw AuthException("Registration API error: ${e.errorDescription}")
		} catch (e: Exception) {
			log.error("Unhandled error context during registration for: {}", request.email, e)
			throw AuthException("An unexpected registration error occurred.", e)
		} finally {
			client.close()
		}
	}

	fun resendConfirmation(email: String) = runBlocking {
		val client = createIsolatedClient()
		try {
			log.info("Triggering remote token reissue request to: {}", email)
			client.auth.resendEmail(OtpType.Email.SIGNUP, email)
		} catch (e: AuthRestException) {
			log.error("Supabase endpoint error during token dispatch to {}: {}", email, e.errorDescription)
			throw AuthException("Verification server error: ${e.errorDescription}")
		} catch (e: Exception) {
			log.error("Failed handling automated token renewal flow for target destination: {}", email, e)
			throw AuthException("Failed to reissue validation email.", e)
		} finally {
			client.close()
		}
	}

	fun login(request: LoginRequest): AuthResponse = runBlocking {
		val client = createIsolatedClient()
		try {
			log.info("Forwarding authentication request parameters for user: {}", request.email)
			client.auth.signInWith(Email) {
				email = request.email
				password = request.password
			}

			val currentSession = client.auth.currentSessionOrNull()
				?: throw SessionExpiredException("Session missing from authentication response engine.")

			AuthResponse(
				userId = currentSession.user?.id
					?: throw AuthException("Missing User ID context in valid payload profile."),
				accessToken = currentSession.accessToken,
				refreshToken = currentSession.refreshToken
			)
		} catch (e: AuthRestException) {
			log.warn(
				"Supabase reject authenticating credential profile [{}]: {}",
				request.email,
				e.errorDescription
			)
			throw InvalidCredentialsException("Invalid email or password.")
		} catch (_: AuthSessionMissingException) {
			log.error("Identity management pool returned valid confirmation status but zero session payload.")
			throw InvalidCredentialsException("Session initialization failure. Please try again.")
		} catch (_: InvalidJwtException) {
			log.error("Token structural formatting rejection error during credential payload exchange.")
			throw InvalidCredentialsException("Authentication structure corrupted.")
		} catch (e: Exception) {
			log.error("Unexpected login failure context tracking profile address: {}", request.email, e)
			throw AuthException("Authentication server encountered an unexpected error.", e)
		} finally {
			client.close()
		}
	}

	fun logout(accessToken: String, refreshToken: String): Unit = runBlocking {
		val client = createIsolatedClient()
		try {
			log.info("Terminating authorization states via infrastructure pipeline.")
			val dummySession = UserSession(
				accessToken = accessToken,
				refreshToken = refreshToken,
				expiresIn = 3600,
				tokenType = "Bearer",
				user = null
			)
			client.auth.importSession(dummySession)
			client.auth.signOut()
		} catch (_: TokenExpiredException) {
			log.warn("Local authentication credential payload was already expired at the server node.")
		} catch (_: SessionRequiredException) {
			log.warn("Supabase active lifecycle tracking engine confirms no session footprint requires removal.")
		} catch (e: Exception) {
			log.warn("Supabase safe sign-out failed cleanly: {}", e.message)
		} finally {
			client.close()
		}
	}

	fun getUserMetadata(accessToken: String): JwtPayload = runBlocking {
		val client = createIsolatedClient()
		try {
			client.auth.getClaims(accessToken).claims
		} finally {
			client.close()
		}
	}

	fun refreshSession(refreshToken: String): UserSession = runBlocking {
		val client = createIsolatedClient()
		try {
			client.auth.refreshSession(refreshToken)
		} finally {
			client.close()
		}
	}
}