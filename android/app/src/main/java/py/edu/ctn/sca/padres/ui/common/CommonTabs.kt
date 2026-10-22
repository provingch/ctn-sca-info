package py.edu.ctn.sca.padres.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.data.common.NotificacionesApi
import py.edu.ctn.sca.padres.data.common.NotificacionesRepository
import py.edu.ctn.sca.padres.ui.graphViewModel
import py.edu.ctn.sca.padres.ui.profile.ProfileScreen
import py.edu.ctn.sca.padres.ui.profile.ProfileTab

/**
 * Cableado de las pestañas Avisos y Perfil, igual para todos los roles.
 *
 * Uso en `XApp`:
 * ```
 * val common = rememberCommonTabs(graph, role)
 * common.ProfileHost {            // muestra ProfileScreen a pantalla completa si está abierto
 *     RoleScaffold(..., tabs = listOf(..., RoleTab("Avisos", icon, common.unread), ...), headerAction = if (avisos) common.avisosAction else null) {
 *         if (avisos) common.AvisosContent() else if (perfil) common.PerfilContent() else ...
 *     }
 * }
 * ```
 * Llamar [refresh] al cambiar de pestaña actualiza el badge de Avisos.
 */
@Stable
class CommonTabs(val graph: Graph, val role: Role, val avisos: AvisosViewModel) {
    var openProfileTab by mutableStateOf<ProfileTab?>(null)

    /** Cantidad de avisos sin leer para el badge. */
    val unread: Int @Composable get() = avisos.ui.collectAsState().value.unread

    /** "Subtítulo" del header de Avisos / Perfil: "Nombre · Rol". */
    val subtitle: String
        @Composable get() = avisos.ui.collectAsState().value.userName.let { n -> if (n.isEmpty()) role.label else "$n · ${role.label}" }

    /** Acción "Marcar leídos" del header de Avisos (null si no hay nada sin leer). */
    val avisosAction: HeaderAction?
        @Composable get() = if (unread > 0) HeaderAction("Marcar leídos", avisos::marcarTodas) else null

    fun refresh() = avisos.refreshCount()

    @Composable
    fun AvisosContent(modifier: Modifier = Modifier) = AvisosScreen(avisos, modifier)

    @Composable
    fun PerfilContent(modifier: Modifier = Modifier) =
        PerfilScreen(graph, role, onOpen = { openProfileTab = it }, modifier = modifier)

    @Composable
    fun ProfileHost(content: @Composable () -> Unit) {
        val tab = openProfileTab
        if (tab != null) ProfileScreen(graph, onBack = { openProfileTab = null }, initialTab = tab) else content()
    }
}

@Composable
fun rememberCommonTabs(graph: Graph, role: Role): CommonTabs {
    val vm: AvisosViewModel = graphViewModel {
        AvisosViewModel(NotificacionesRepository(it.api<NotificacionesApi>()), it.profileRepository, it.session)
    }
    LaunchedEffect(Unit) {
        vm.loadUserName()
        vm.refreshCount()
    }
    return remember(graph, role, vm) { CommonTabs(graph, role, vm) }
}
