package py.edu.ctn.sca.padres.ui.profesor

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.data.profesor.ProfesorApi
import py.edu.ctn.sca.padres.data.profesor.ProfesorRepository
import py.edu.ctn.sca.padres.ui.common.CommonTabs
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.RoleTab
import py.edu.ctn.sca.padres.ui.common.rememberCommonTabs
import py.edu.ctn.sca.padres.ui.graphViewModel
import java.time.LocalDate

private val TAB_ROUTES = listOf("hoy", "clases", "planillas", "avisos", "perfil")

private fun repo(g: Graph) = ProfesorRepository(g.api<ProfesorApi>(), g.network.json)

@Composable
fun ProfesorApp(graph: Graph) {
    val common = rememberCommonTabs(graph, Role.PROFESOR)
    val nav = rememberNavController()

    common.ProfileHost {
        NavHost(nav, startDestination = "hoy") {
            composable("hoy") {
                val vm: HoyViewModel = graphViewModel { HoyViewModel(repo(it)) }
                val ui by vm.ui.collectAsState()
                LaunchedEffect(Unit) { vm.load() }
                val nombre = common.avisos.ui.collectAsState().value.userName
                TabFrame(
                    common, nav, 0,
                    title = if (nombre.isBlank()) "Hola" else "Hola, $nombre",
                    subtitle = listOf(fechaLarga(LocalDate.now()), materiasDelDia(ui.bloques.orEmpty())).filter { it.isNotBlank() }.joinToString(" · "),
                ) {
                    HoyContent(ui, vm::load) { b -> nav.navigate("registro/${b.asignacionId}/${Uri.encode(b.horaInicio)}") }
                }
            }
            composable("clases") {
                val vm: ClasesViewModel = graphViewModel { ClasesViewModel(repo(it)) }
                val ui by vm.ui.collectAsState()
                LaunchedEffect(Unit) { vm.load() }
                TabFrame(common, nav, 1, title = "Clases") { ClasesContent(ui, vm::load) { nav.navigate("clase/$it") } }
            }
            composable("planillas") {
                val vm: PlanillasViewModel = graphViewModel { PlanillasViewModel(repo(it)) }
                val ui by vm.ui.collectAsState()
                TabFrame(common, nav, 2, title = "Planillas") {
                    PlanillasContent(ui, { vm.load() }, vm::load) { nav.navigate("tareas/$it") }
                }
            }
            composable("avisos") {
                TabFrame(common, nav, 3, title = "Avisos", subtitle = common.subtitle, headerAction = common.avisosAction) { common.AvisosContent() }
            }
            composable("perfil") {
                TabFrame(common, nav, 4, title = "Perfil") { common.PerfilContent() }
            }

            composable(
                "registro/{asignacionId}/{hora}",
                listOf(navArgument("asignacionId") { type = NavType.IntType }, navArgument("hora") { type = NavType.StringType }),
            ) { e ->
                val asig = e.arguments!!.getInt("asignacionId")
                val hora = e.arguments!!.getString("hora").orEmpty()
                val vm: RegistroViewModel = graphViewModel { RegistroViewModel(repo(it), asig, hora) }
                RegistroScreen(vm, onBack = { nav.popBackStack() }, onGuardada = { nav.popBackStack() })
            }
            composable("clase/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                val id = e.arguments!!.getInt("id")
                val vm: ClaseDetalleViewModel = graphViewModel { ClaseDetalleViewModel(repo(it), id) }
                val ui by vm.ui.collectAsState()
                RoleScaffold(
                    title = "Clase", onBack = { nav.popBackStack() },
                    subtitle = ui.clase?.fechaClase?.let { fechaCorta(it) },
                ) { ClaseDetalleContent(ui, vm::load) }
            }
            composable("tareas/{planillaId}", listOf(navArgument("planillaId") { type = NavType.IntType })) { e ->
                val pid = e.arguments!!.getInt("planillaId")
                val vm: TareasViewModel = graphViewModel { TareasViewModel(repo(it), pid) }
                val ui by vm.ui.collectAsState()
                LaunchedEffect(Unit) { vm.load() }
                val d = ui.detalle
                RoleScaffold(
                    title = d?.planilla?.materiaNombre?.ifBlank { null } ?: "Tareas",
                    subtitle = d?.let { listOfNotNull(it.curso?.let { c -> "${c.nivel}° ${c.seccion}" }, "Etapa ${it.planilla.etapaIndex}").joinToString(" · ") },
                    onBack = { nav.popBackStack() },
                ) { TareasContent(ui, vm::load) { tid -> nav.navigate("correccion/$pid/$tid") } }
            }
            composable(
                "correccion/{planillaId}/{tareaId}",
                listOf(navArgument("planillaId") { type = NavType.IntType }, navArgument("tareaId") { type = NavType.IntType }),
            ) { e ->
                val pid = e.arguments!!.getInt("planillaId")
                val tid = e.arguments!!.getInt("tareaId")
                val vm: CorreccionViewModel = graphViewModel { CorreccionViewModel(repo(it), pid, tid) }
                CorreccionScreen(vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** Marco de las 5 pestañas: header + barra inferior; navegar entre pestañas guarda/restaura el estado. */
@Composable
private fun TabFrame(
    common: CommonTabs,
    nav: NavHostController,
    selected: Int,
    title: String,
    subtitle: String? = null,
    headerAction: py.edu.ctn.sca.padres.ui.common.HeaderAction? = null,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(selected) { common.refresh() }
    val tabs = listOf(
        RoleTab("Hoy", Icons.Outlined.CalendarToday),
        RoleTab("Clases", Icons.Outlined.Description),
        RoleTab("Planillas", Icons.Outlined.GridView),
        RoleTab("Avisos", Icons.Outlined.Notifications, common.unread),
        RoleTab("Perfil", Icons.Outlined.Person),
    )
    RoleScaffold(
        title = title, subtitle = subtitle, headerAction = headerAction, tabs = tabs, selectedTab = selected,
        onTabSelected = { i ->
            if (i != selected) nav.navigate(TAB_ROUTES[i]) {
                popUpTo("hoy") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        content = content,
    )
}
