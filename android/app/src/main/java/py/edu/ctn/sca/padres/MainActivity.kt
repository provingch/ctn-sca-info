package py.edu.ctn.sca.padres

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import py.edu.ctn.sca.padres.core.Session
import py.edu.ctn.sca.padres.ui.AppRoot
import py.edu.ctn.sca.padres.ui.theme.ScaPadresTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appGraph = graph
        // Kick off a silent session restore from the stored refresh cookie.
        if (appGraph.session.state.value == Session.AuthState.UNKNOWN) {
            lifecycleScope.launch { appGraph.authRepository.restore() }
        }

        setContent {
            ScaPadresTheme {
                val authState by appGraph.session.state.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    AppRoot(
                        modifier = Modifier.padding(padding),
                        authState = authState,
                        graph = appGraph,
                        onAuthenticated = { maybeAskNotificationPermission() },
                    )
                }
            }
        }
    }

    private fun maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
