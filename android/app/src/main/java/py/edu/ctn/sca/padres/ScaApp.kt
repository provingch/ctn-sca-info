package py.edu.ctn.sca.padres

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import py.edu.ctn.sca.padres.core.Network
import py.edu.ctn.sca.padres.core.PersistentCookieJar
import py.edu.ctn.sca.padres.core.Session
import py.edu.ctn.sca.padres.core.TokenStore
import py.edu.ctn.sca.padres.data.AuthRepository
import py.edu.ctn.sca.padres.data.ParentRepository
import py.edu.ctn.sca.padres.data.PushRepository

class ScaApp : Application() {

    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            getString(R.string.fcm_channel_id),
            getString(R.string.fcm_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = getString(R.string.fcm_channel_desc) }
        mgr.createNotificationChannel(channel)
    }
}

/** Hand-rolled service locator. The app is tiny; a DI framework would be overkill. */
class Graph(context: Context) {
    val session = Session()
    private val tokenStore = TokenStore(context)
    private val cookieJar = PersistentCookieJar(tokenStore)

    private val network = Network(
        session = session,
        cookieJar = cookieJar,
        onSessionExpired = { session.onLoggedOut() },
    )

    val authRepository = AuthRepository(network.authApi, session, cookieJar, network.json)
    val parentRepository = ParentRepository(network.parentApi)
    val pushRepository = PushRepository(network.pushApi, tokenStore)
}

val Context.graph: Graph
    get() = (applicationContext as ScaApp).graph
