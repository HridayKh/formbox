package `in`.hridaykh.formbox

import `in`.hridaykh.formbox.config.SupabaseProperties
import `in`.hridaykh.formbox.exception.auth.AuthException
import `in`.hridaykh.formbox.exception.auth.InvalidCredentialsException
import `in`.hridaykh.formbox.exception.auth.SessionExpiredException
import `in`.hridaykh.formbox.exception.auth.UserAlreadyExistsException
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
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class SignUpRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val userId: String, val accessToken: String, val refreshToken: String)

@Service
class AuthServiceKt(private val supabaseProps: SupabaseProperties) {

	private val log = LoggerFactory.getLogger(AuthServiceKt::class.java)

	fun createIsolatedClient() = createSupabaseClient(supabaseProps.url, supabaseProps.secretKey) {
		logging = false
		install(Auth.Companion) {
			autoLoadFromStorage = false
			autoSaveToStorage = false
			alwaysAutoRefresh = false
		}
	}

	fun closeIsolatedClient(client: SupabaseClient) = runBlocking {
		log.trace("Closing isolated Supabase client connection instance.")
		client.close()
	}

	fun signUp(client: SupabaseClient, request: SignUpRequest): Unit = runBlocking {
		log.debug("Initiating Supabase authentication signup pipeline for email: {}", request.email)
		try {
			log.trace("Processing Supabase registration chain for: {}", request.email)
			val user: UserInfo? = client.auth.signUpWith(Email) {
				email = request.email
				password = request.password
			}
			if (user?.id == null) {
				log.error(
					"Supabase registration transaction completed but assigned user reference object was null for: {}",
					request.email
				)
				throw AuthException("Registration failed: Service did not assign a valid User UID.")
			}
			log.info("Supabase signup transaction successfully finalized for email: {}", request.email)
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
		}
	}

	fun resendConfirmation(client: SupabaseClient, email: String) = runBlocking {
		log.debug("Initiating verification recovery flow dispatcher targeting email: {}", email)
		try {
			log.trace("Triggering remote token reissue request to: {}", email)
			client.auth.resendEmail(OtpType.Email.SIGNUP, email)
			log.info(
				"Successfully dispatched confirmation email trigger sequence via Supabase providers for: {}",
				email
			)
		} catch (e: AuthRestException) {
			log.error("Supabase endpoint error during token dispatch to {}: {}", email, e.errorDescription)
			throw AuthException("Verification server error: ${e.errorDescription}")
		} catch (e: Exception) {
			log.error("Failed handling automated token renewal flow for target destination: {}", email, e)
			throw AuthException("Failed to reissue validation email.", e)
		}
	}

	fun login(client: SupabaseClient, request: LoginRequest): AuthResponse = runBlocking {
		log.debug("Initiating primary authentication verification loop for address: {}", request.email)
		try {
			log.trace("Forwarding authentication request parameters for user: {}", request.email)
			client.auth.signInWith(Email) {
				email = request.email
				password = request.password
			}

			val currentSession = client.auth.currentSessionOrNull()
				?: throw SessionExpiredException("Session missing from authentication response engine.")

			val assignedUserId = currentSession.user?.id
				?: throw AuthException("Missing User ID context in valid payload profile.")

			log.info(
				"Supabase session generation handshake completed cleanly for user UID: {}",
				assignedUserId
			)

			AuthResponse(
				userId = assignedUserId,
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
		} catch (e: AuthSessionMissingException) {
			log.error(
				"Identity management pool returned valid confirmation status but zero session payload for email: {}",
				request.email,
				e
			)
			throw InvalidCredentialsException("Session initialization failure. Please try again.")
		} catch (e: InvalidJwtException) {
			log.error(
				"Token structural formatting rejection error during credential payload exchange for target: {}",
				request.email,
				e
			)
			throw InvalidCredentialsException("Authentication structure corrupted.")
		} catch (e: Exception) {
			log.error("Unexpected login failure context tracking profile address: {}", request.email, e)
			throw AuthException("Authentication server encountered an unexpected error.", e)
		}
	}

	fun logout(client: SupabaseClient, accessToken: String, refreshToken: String): Unit = runBlocking {
		log.debug("Initiating backend logout state invalidation pipeline sequence.")
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
			log.info("Remote session revocation completed successfully against network endpoint.")
		} catch (e: Exception) {
			log.error(
				"Supabase failed to properly acknowledge explicit session termination tracking arguments.",
				e
			)
			throw e
		}
	}

	fun getUserMetadata(client: SupabaseClient, accessToken: String?): JwtPayload? = runBlocking {
		log.trace("Resolving claims parsing profile layer context against active token mapping.")
		if (accessToken.isNullOrBlank()) {
			log.debug("Claims token payload tracking verification skipped. Argument provided is completely blank.")
			return@runBlocking null
		}
		try {
			val claims = client.auth.getClaims(accessToken).claims
			log.trace("User session security claims metadata decoded successfully.")
			return@runBlocking claims
		} catch (e: Exception) {
			log.error(
				"Failed to decode user security claims context array from raw access token payload string.",
				e
			)
			null
		}
	}

	fun refreshSession(client: SupabaseClient, refreshToken: String): UserSession = runBlocking {
		log.debug("Issuing downstream token rotation refresh verification exchange handler sequence.")
		try {
			val session = client.auth.refreshSession(refreshToken)
			log.info("Session context tokens rolled successfully through asynchronous infrastructure channel.")
			return@runBlocking session
		} catch (e: Exception) {
			log.error(
				"Supabase network authorization loop failed to spin credentials using existing refresh token.",
				e
			)
			throw e
		}
	}
}
