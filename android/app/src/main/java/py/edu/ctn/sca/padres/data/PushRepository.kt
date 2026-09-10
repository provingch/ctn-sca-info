package py.edu.ctn.sca.padres.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import py.edu.ctn.sca.padres.core.TokenStore

/**
 * Registers this device's FCM token with the backend so the school can push
 * "nueva tarea" / "tarea calificada" notices to the parent. Safe to call after
 * every successful login and from [py.edu.ctn.sca.padres.fcm.ScaMessagingService]
 * on token rotation; it de-dupes via [TokenStore.lastFcmTokenSynced].
 */
class PushRepository(
    private val pushApi: PushApi,
    private val store: TokenStore,
) {
    suspend fun syncToken(force: Boolean = false) {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        if (!force && token == store.lastFcmTokenSynced) return
        val ok = runCatching { pushApi.register(FcmTokenRequest(token)).isSuccessful }.getOrDefault(false)
        if (ok) {
            store.lastFcmTokenSynced = token
        } else {
            Log.w(TAG, "No se pudo registrar el token FCM; se reintentará luego")
        }
    }

    suspend fun onTokenRefreshed(token: String) {
        val ok = runCatching { pushApi.register(FcmTokenRequest(token)).isSuccessful }.getOrDefault(false)
        if (ok) store.lastFcmTokenSynced = token
    }

    suspend fun unregister() {
        val token = store.lastFcmTokenSynced ?: return
        runCatching { pushApi.unregister(FcmTokenRequest(token)) }
        store.lastFcmTokenSynced = null
    }

    private companion object {
        const val TAG = "PushRepository"
    }
}
