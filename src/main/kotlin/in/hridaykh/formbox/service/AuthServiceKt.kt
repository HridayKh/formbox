package `in`.hridaykh.formbox.service

import `in`.hridaykh.formbox.config.SupabaseProperties
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.jwt.JwtPayload
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.springframework.stereotype.Service

data class SignUpRequest(val email: String, val password: String, val username: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val userId: String, val accessToken: String, val refreshToken: String)

@Service
class AuthServiceKt(private val supabaseProps: SupabaseProperties) {

	private fun createIsolatedClient() = createSupabaseClient(supabaseProps.url, supabaseProps.secretKey) {
		install(Auth) {
			autoLoadFromStorage = false
			autoSaveToStorage = false
			alwaysAutoRefresh = false
		}
	}

	fun signUp(request: SignUpRequest): Unit = runBlocking {
		val client = createIsolatedClient()
		try {
			val user: UserInfo? = client.auth.signUpWith(Email) {
				email = request.email
				password = request.password
				data = buildJsonObject {
					put("first_name", request.username)
				}
			}
			user?.id ?: throw RuntimeException("Registration failed")
		} finally {
			client.close()
		}
	}

	fun resendConfirmation(email: String) = runBlocking {
		val client = createIsolatedClient()
		try {
			client.auth.resendEmail(OtpType.Email.SIGNUP, email)
		} finally {
			client.close()
		}
	}

	fun login(request: LoginRequest): AuthResponse = runBlocking {
		val client = createIsolatedClient()
		try {
			client.auth.signInWith(Email) {
				email = request.email
				password = request.password
			}

			val currentSession = client.auth.currentSessionOrNull()
				?: throw RuntimeException("Login failed: Session is null")

			AuthResponse(
				userId = currentSession.user?.id ?: throw RuntimeException("Missing User ID"),
				accessToken = currentSession.accessToken,
				refreshToken = currentSession.refreshToken
			)
		} finally {
			client.close()
		}
	}

	fun logout(accessToken: String, refreshToken: String): Unit = runBlocking {
		val client = createIsolatedClient()
		try {
			// Reconstruct the proper session object required by the SDK
			val dummySession = UserSession(
				accessToken = accessToken,
				refreshToken = refreshToken,
				expiresIn = 3600,
				tokenType = "Bearer",
				user = null
			)
			client.auth.importSession(dummySession)
			client.auth.signOut()
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
}