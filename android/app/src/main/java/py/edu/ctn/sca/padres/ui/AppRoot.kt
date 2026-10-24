package py.edu.ctn.sca.padres.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Session
import py.edu.ctn.sca.padres.ui.auth.LoginFlow
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.ui.coordinacion.CoordinacionApp
import py.edu.ctn.sca.padres.ui.evaluador.EvaluadorApp
import py.edu.ctn.sca.padres.ui.padre.PadreApp
import py.edu.ctn.sca.padres.ui.profesor.ProfesorApp
import py.edu.ctn.sca.padres.ui.theme.ScaTheme
import py.edu.ctn.sca.padres.ui.theme.rememberDarkTheme

@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
    authState: Session.AuthState,
    graph: Graph,
    onAuthenticated: () -> Unit,
) {
    LaunchedEffect(authState) {
        if (authState == Session.AuthState.AUTHENTICATED) onAuthenticated()
    }

    AnimatedContent(
        targetState = authState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "auth-gate",
        modifier = modifier.fillMaxSize(),
    ) { state ->
        when (state) {
            Session.AuthState.UNKNOWN -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            Session.AuthState.LOGGED_OUT -> LoginFlow(graph = graph)
            Session.AuthState.AUTHENTICATED -> {
                val role = graph.session.role ?: Role.PADRE
                SessionViewModelScope {
                    ScaTheme(role, darkTheme = rememberDarkTheme(graph.themePrefs)) {
                        when (role) {
                            Role.PROFESOR -> ProfesorApp(graph)
                            Role.EVALUADOR -> EvaluadorApp(graph)
                            Role.COORDINACION -> CoordinacionApp(graph)
                            // ADMIN no llega acá (AuthRepository lo rechaza).
                            Role.PADRE, Role.ADMIN -> PadreApp(graph)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Los ViewModels de la sesión viven en un store propio que se limpia al salir de
 * AUTHENTICATED: así, si cierra sesión uno y entra otro usuario en el mismo celular,
 * no ve datos cacheados del anterior.
 */
@Composable
private fun SessionViewModelScope(content: @Composable () -> Unit) {
    val store = remember { ViewModelStore() }
    val owner = remember(store) { object : ViewModelStoreOwner { override val viewModelStore = store } }
    DisposableEffect(store) { onDispose { store.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
