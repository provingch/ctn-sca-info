package py.edu.ctn.sca.padres.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted persistence for the long-lived refresh cookie and a couple of
 * lightweight flags. The short-lived access token is never persisted; it lives
 * only in [Session] in memory.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sca_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Serialized OkHttp cookies for the backend host (refresh + session cookies). */
    var cookies: Set<String>
        get() = prefs.getStringSet(KEY_COOKIES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_COOKIES, value).apply()

    var lastFcmTokenSynced: String?
        get() = prefs.getString(KEY_FCM, null)
        set(value) = prefs.edit().putString(KEY_FCM, value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_COOKIES = "auth_cookies"
        const val KEY_FCM = "fcm_token_synced"
        const val KEY_REMEMBER = "remember_me"
    }
}
