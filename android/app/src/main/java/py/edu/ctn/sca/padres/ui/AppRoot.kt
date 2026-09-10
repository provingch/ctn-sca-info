package py.edu.ctn.sca.padres.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Session
import py.edu.ctn.sca.padres.ui.auth.LoginFlow
import py.edu.ctn.sca.padres.ui.parent.ParentDashboardScreen
import py.edu.ctn.sca.padres.ui.profile.ProfileScreen

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
                var showProfile by rememberSaveable { mutableStateOf(false) }
                if (showProfile) {
                    ProfileScreen(graph = graph, onBack = { showProfile = false })
                } else {
                    ParentDashboardScreen(graph = graph, onOpenProfile = { showProfile = true })
                }
            }
        }
    }
}
