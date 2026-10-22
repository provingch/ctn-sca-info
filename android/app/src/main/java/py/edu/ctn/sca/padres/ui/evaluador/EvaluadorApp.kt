package py.edu.ctn.sca.padres.ui.evaluador

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.data.evaluador.EvaluadorApi
import py.edu.ctn.sca.padres.data.evaluador.EvaluadorRepository
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.RoleTab
import py.edu.ctn.sca.padres.ui.common.rememberCommonTabs
import py.edu.ctn.sca.padres.ui.graphViewModel

private const val INICIO = 0
private const val PLANES = 1
private const val SEGUIMIENTO = 2
private const val AVISOS = 3
private const val PERFIL = 4

@Composable
fun EvaluadorApp(graph: Graph) {
    val common = rememberCommonTabs(graph, Role.EVALUADOR)
    val context = LocalContext.current
    val appContext = context.applicationContext
    val vm: EvaluadorViewModel = graphViewModel {
        EvaluadorViewModel(EvaluadorRepository(appContext, it.api<EvaluadorApi>(), it.network.json))
    }
    val ui by vm.ui.collectAsState()
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableIntStateOf(INICIO) }
    LaunchedEffect(tab) { common.refresh() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.messageShown()
        }
    }

    val pendientes = (ui.planes as? Load.Ok)?.data?.size ?: 0
    val tabs = listOf(
        RoleTab("Inicio", Icons.Outlined.Home),
        RoleTab("Planes", Icons.Outlined.Description, pendientes),
        RoleTab("Seguimiento", Icons.Outlined.ShowChart),
        RoleTab("Avisos", Icons.Outlined.Notifications, common.unread),
        RoleTab("Perfil", Icons.Outlined.Person),
    )
    val nombre = common.avisos.ui.collectAsState().value.userName

    common.ProfileHost {
        NavHost(nav, startDestination = "tabs") {
            composable("tabs") {
                RoleScaffold(
                    title = if (tab == INICIO) (if (nombre.isEmpty()) "Hola" else "Hola, $nombre") else tabs[tab].label,
                    subtitle = when (tab) {
                        INICIO -> "Evaluación · ${diaLargo()}"
                        AVISOS -> common.subtitle
                        else -> null
                    },
                    headerAction = if (tab == AVISOS) common.avisosAction else null,
                    tabs = tabs,
                    selectedTab = tab,
                    onTabSelected = { tab = it },
                ) {
                    when (tab) {
                        INICIO -> InicioScreen(
                            ui, vm::refresh,
                            onPlan = { nav.navigate("plan/$it") },
                            onIncumplimiento = { tab = SEGUIMIENTO },
                        )
                        PLANES -> PlanesScreen(ui.planes, vm::loadPlanes) { nav.navigate("plan/$it") }
                        SEGUIMIENTO -> SeguimientoScreen(ui, vm::loadIncumplimientos, vm::resolver)
                        AVISOS -> common.AvisosContent()
                        else -> common.PerfilContent()
                    }
                }
            }
            composable("plan/{id}", listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                val id = entry.arguments?.getInt("id") ?: 0
                PlanDetalleScreen(id, vm, ui, onBack = { nav.popBackStack() })
            }
        }
    }
}
