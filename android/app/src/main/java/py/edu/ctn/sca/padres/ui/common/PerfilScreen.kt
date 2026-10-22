package py.edu.ctn.sca.padres.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.BuildConfig
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.data.AuthRepository
import py.edu.ctn.sca.padres.data.ProfileLoad
import py.edu.ctn.sca.padres.data.ProfileRepository
import py.edu.ctn.sca.padres.data.PushRepository
import py.edu.ctn.sca.padres.ui.graphViewModel
import py.edu.ctn.sca.padres.ui.profile.ProfileTab
import py.edu.ctn.sca.padres.ui.theme.scaColors

data class PerfilUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val nombre: String = "",
    val usuario: String = "",
    val ci: String = "",
    val roleLabel: String = "",
    val totp: Boolean = false,
)

class PerfilViewModel(
    private val profile: ProfileRepository,
    private val auth: AuthRepository,
    private val push: PushRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(PerfilUiState())
    val ui: StateFlow<PerfilUiState> = _ui.asStateFlow()

    fun load() {
        _ui.update { it.copy(loading = it.nombre.isEmpty(), error = null) }
        viewModelScope.launch {
            when (val r = profile.load()) {
                is ProfileLoad.Ok -> {
                    val o = r.data.profileOwner
                    _ui.value = PerfilUiState(
                        loading = false,
                        nombre = o.fullName ?: listOfNotNull(o.nombre, o.apellido).joinToString(" "),
                        usuario = o.usuario.orEmpty(),
                        ci = o.ci?.toString().orEmpty(),
                        roleLabel = r.data.profileRoleLabel.orEmpty(),
                        totp = r.data.totpEnabled,
                    )
                }
                is ProfileLoad.Error -> _ui.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { push.unregister() }
            auth.logout()
        }
    }
}

/** Pestaña Perfil (diseño m-40). [onOpen] abre el ProfileScreen existente en la pestaña pedida. */
@Composable
fun PerfilScreen(graph: Graph, role: Role, onOpen: (ProfileTab) -> Unit, modifier: Modifier = Modifier) {
    val vm: PerfilViewModel = graphViewModel { PerfilViewModel(it.profileRepository, it.authRepository, it.pushRepository) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    when {
        ui.loading -> LoadingState(modifier)
        ui.error != null -> ErrorState(ui.error!!, vm::load, modifier)
        else -> Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScaCard(contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(iniciales(ui.nombre), color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(ui.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(
                            listOfNotNull(ui.usuario.takeIf { it.isNotEmpty() }?.let { "@$it" }, ui.ci.takeIf { it.isNotEmpty() }?.let { "CI $it" })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = scaColors.muted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Tag(ui.roleLabel.ifEmpty { role.label }, scaColors.info)
                    }
                }
            }
            SectionLabel("Cuenta")
            ListCard(listOf(0, 1, 2)) { i ->
                when (i) {
                    0 -> ListRow("Datos personales", subtitle = "Nombre, correo y teléfono", onClick = { onOpen(ProfileTab.PROFILE) })
                    1 -> ListRow("Contraseña", subtitle = "Cambiala cuando quieras", onClick = { onOpen(ProfileTab.SECURITY) })
                    else -> ListRow(
                        "Verificación en dos pasos",
                        subtitle = if (ui.totp) "Activada" else "Desactivada",
                        onClick = { onOpen(ProfileTab.SECURITY) },
                        trailing = { Tag(if (ui.totp) "Sí" else "No", if (ui.totp) scaColors.success else scaColors.warning) },
                    )
                }
            }
            SectionLabel("App")
            ListCard(listOf(0, 1, 2)) { i ->
                when (i) {
                    0 -> ListRow("Notificaciones", subtitle = "Avisos en este celular", onClick = { onOpen(ProfileTab.APP) })
                    1 -> ListRow("Tema", subtitle = "Igual que el celular")
                    else -> ListRow("Versión", subtitle = BuildConfig.VERSION_NAME)
                }
            }
            DangerTextButton("Cerrar sesión", vm::logout, Modifier.fillMaxWidth())
        }
    }
}

private fun Modifier.fillMaxWidth() = this.then(Modifier.fillMaxWidth())

private fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
