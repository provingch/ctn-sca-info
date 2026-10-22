package py.edu.ctn.sca.padres.ui.padre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.data.padre.PadreApi
import py.edu.ctn.sca.padres.data.padre.PadreRepository
import py.edu.ctn.sca.padres.ui.common.ChipRow
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ErrorState
import py.edu.ctn.sca.padres.ui.common.LoadingState
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.RoleTab
import py.edu.ctn.sca.padres.ui.common.ScaChip
import py.edu.ctn.sca.padres.ui.common.rememberCommonTabs
import py.edu.ctn.sca.padres.ui.graphViewModel

private const val HIJOS = "hijos"
private const val NOTAS = "notas"
private const val CONDUCTA = "conducta"
private const val AVISOS = "avisos"
private const val PERFIL = "perfil"
private const val ANOTACION = "anotacion/{id}"
private val RUTAS = listOf(HIJOS, NOTAS, CONDUCTA, AVISOS, PERFIL)

@Composable
fun PadreApp(graph: Graph) {
    val common = rememberCommonTabs(graph, Role.PADRE)
    val vm: PadreViewModel = graphViewModel { PadreViewModel(it.parentRepository, PadreRepository(it.api<PadreApi>())) }
    val ui by vm.ui.collectAsState()
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: HIJOS
    // El detalle de anotación se muestra bajo la pestaña Conducta.
    val tabIndex = RUTAS.indexOf(route).takeIf { it >= 0 } ?: RUTAS.indexOf(CONDUCTA)
    LaunchedEffect(route) { common.refresh() }

    val tabs = listOf(
        RoleTab("Hijos", Icons.Outlined.Group),
        RoleTab("Notas", Icons.Outlined.BarChart),
        RoleTab("Conducta", Icons.Outlined.PhotoCamera),
        RoleTab("Avisos", Icons.Outlined.Notifications, common.unread),
        RoleTab("Perfil", Icons.Outlined.Person),
    )
    val userName = common.avisos.ui.collectAsState().value.userName
    val hijo = ui.hijo
    val hijoSub = hijo?.let { h -> listOfNotNull(ui.hijoNombre, ui.cursoDe(h)).joinToString(" · ") }
    val (title, subtitle) = when (route) {
        HIJOS -> "Hola, ${userName.substringBefore(' ')}".trimEnd(',', ' ') to hijo?.let { h -> listOfNotNull(h.nombre, ui.cursoDe(h)).joinToString(" · ") }
        NOTAS -> "Notas" to hijoSub
        CONDUCTA -> "Conducta" to hijoSub
        AVISOS -> "Avisos" to common.subtitle
        PERFIL -> "Perfil" to null
        else -> "Anotación" to hijoSub
    }

    common.ProfileHost {
        RoleScaffold(
            title = title,
            subtitle = subtitle,
            onBack = if (route == ANOTACION) ({ nav.popBackStack() }) else null,
            headerAction = if (route == AVISOS) common.avisosAction else null,
            tabs = tabs,
            selectedTab = tabIndex,
            onTabSelected = { i ->
                nav.navigate(RUTAS[i]) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        ) {
            NavHost(nav, startDestination = HIJOS) {
                composable(HIJOS) {
                    PadreTab(ui, vm) { HijosContent(ui, vm, onOpen = { nav.navigate("anotacion/$it") }) }
                }
                composable(NOTAS) {
                    PadreTab(ui, vm) { NotasContent(ui, vm, graph.reportRepository) }
                }
                composable(CONDUCTA) {
                    PadreTab(ui, vm) { ConductaContent(ui, vm, onOpen = { nav.navigate("anotacion/$it") }) }
                }
                composable(AVISOS) { common.AvisosContent() }
                composable(PERFIL) { common.PerfilContent() }
                composable(ANOTACION) { entry ->
                    AnotacionDetalle(
                        ui, vm,
                        id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0,
                        onOpen = { nav.navigate("anotacion/$it") { popUpTo(ANOTACION) { inclusive = true } } },
                    )
                }
            }
        }
    }
}

/** Carga / error / sin hijos, y selector de hijo arriba; [content] va debajo, dentro de un scroll. */
@Composable
private fun PadreTab(ui: PadreUiState, vm: PadreViewModel, content: @Composable () -> Unit) {
    when {
        ui.loading && ui.hijos.isEmpty() -> LoadingState()
        ui.error != null && ui.summary == null -> ErrorState(ui.error, onRetry = { vm.load() })
        ui.hijos.isEmpty() -> EmptyState("Sin hijos vinculados", message = "Pedile a Secretaría que vincule a tu hijo con tu cuenta.")
        else -> Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChipRow {
                ui.hijos.forEach { h ->
                    ScaChip(
                        label = listOfNotNull(h.nombre, cursoCorto(ui.cursos[h.id])).joinToString(" · "),
                        selected = h.id == ui.selectedId,
                        onClick = { vm.selectChild(h.id) },
                    )
                }
            }
            when {
                ui.loading -> LoadingState(Modifier.padding(vertical = 48.dp))
                ui.error != null -> ErrorState(ui.error, onRetry = { vm.load() })
                else -> content()
            }
        }
    }
}

/** "Informática · 1° A" -> "1° A" */
private fun cursoCorto(curso: String?): String? = curso?.substringAfter(" · ", "")?.takeIf { it.isNotBlank() }
