package py.edu.ctn.sca.padres.ui.coordinacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.data.coordinacion.CoordinacionApi
import py.edu.ctn.sca.padres.data.coordinacion.CoordinacionRepository
import py.edu.ctn.sca.padres.ui.common.HeaderAction
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.RoleTab
import py.edu.ctn.sca.padres.ui.common.rememberCommonTabs
import py.edu.ctn.sca.padres.ui.graphViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RUTAS = listOf("inicio", "quejas", "anotaciones", "avisos", "perfil")
private const val DETALLE = "anotacion/{id}"

@Composable
fun CoordinacionApp(graph: Graph) {
    val common = rememberCommonTabs(graph, Role.COORDINACION)
    val vm: CoordinacionViewModel = graphViewModel { CoordinacionViewModel(CoordinacionRepository(it.api<CoordinacionApi>())) }
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: RUTAS[0]
    LaunchedEffect(route) {
        common.refresh()
        when (route) {
            "quejas" -> vm.cargarQuejas()
            "anotaciones" -> vm.cargarAnotaciones()
        }
    }
    val quejas by vm.quejas.collectAsState()
    val userName = common.avisos.ui.collectAsState().value.userName

    val tabs = listOf(
        RoleTab("Inicio", Icons.Outlined.Home),
        RoleTab("Quejas", Icons.Outlined.ChatBubbleOutline, quejas.pendientes),
        RoleTab("Anotaciones", Icons.Outlined.PhotoCamera),
        RoleTab("Avisos", Icons.Outlined.Notifications, common.unread),
        RoleTab("Perfil", Icons.Outlined.Person),
    )
    fun goTab(i: Int) = nav.navigate(RUTAS[i]) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    @Composable
    fun Tabbed(index: Int, title: String, subtitle: String?, action: HeaderAction? = null, content: @Composable () -> Unit) =
        RoleScaffold(title, subtitle = subtitle, headerAction = action, tabs = tabs, selectedTab = index, onTabSelected = ::goTab, content = content)

    common.ProfileHost {
        NavHost(nav, startDestination = RUTAS[0], modifier = Modifier) {
            composable("inicio") {
                val hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d", Locale("es")))
                val nombre = userName.trim().substringBefore(' ')
                Tabbed(0, if (nombre.isEmpty()) "Hola" else "Hola, $nombre", "Coordinación Pedagógica · $hoy") {
                    InicioScreen(vm, onQuejas = { goTab(1) }, onAnotaciones = { goTab(2) }, onAnotacion = { nav.navigate("anotacion/$it") })
                }
            }
            composable("quejas") { Tabbed(1, "Quejas", "Coordinación Pedagógica") { QuejasScreen(vm) } }
            composable("anotaciones") {
                Tabbed(2, "Anotaciones", "Uniforme y conducta · últimos 7 días") { AnotacionesScreen(vm) { nav.navigate("anotacion/$it") } }
            }
            composable("avisos") { Tabbed(3, "Avisos", common.subtitle, common.avisosAction) { common.AvisosContent() } }
            composable("perfil") { Tabbed(4, "Perfil", common.subtitle) { common.PerfilContent() } }
            composable(DETALLE, arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                RoleScaffold("Anotación", subtitle = "Uniforme y conducta", onBack = { nav.popBackStack() }) {
                    AnotacionDetalleScreen(vm, e.arguments?.getInt("id") ?: 0)
                }
            }
        }
    }
}
