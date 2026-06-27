package `in`.hridaykh.formbox

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

object SupabaseBridge {

    @JvmStatic
    fun createClient(url: String, anonKey: String): SupabaseClient {
        return createSupabaseClient(url, anonKey) {
            install(Auth)
        }
    }

}