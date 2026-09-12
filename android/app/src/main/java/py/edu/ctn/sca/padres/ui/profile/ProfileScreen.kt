package py.edu.ctn.sca.padres.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.ui.components.ContentMaxWidth
import py.edu.ctn.sca.padres.ui.components.Eyebrow
import py.edu.ctn.sca.padres.ui.components.Panel
import py.edu.ctn.sca.padres.ui.graphViewModel
import py.edu.ctn.sca.padres.ui.theme.scaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(graph: Graph, onBack: () -> Unit) {
    val vm: ProfileViewModel = graphViewModel { ProfileViewModel(it.profileRepository) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.padding(top = 48.dp))
                ui.loadError != null -> Column(
                    Modifier
                        .widthIn(max = ContentMaxWidth)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(ui.loadError!!, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = vm::load) { Text("Reintentar") }
                }
                else -> ProfileForm(ui, vm)
            }
        }
    }
}

@Composable
private fun ProfileForm(ui: ProfileUiState, vm: ProfileViewModel) {
    Column(
        Modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Panel {
            Eyebrow("01 · Información personal")
            Text(
                "Datos que identifican tu cuenta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            ProfileField(
                label = "Nombre",
                value = ui.nombre,
                onValueChange = vm::onNombre,
                enabled = ui.canEditIdentity,
                supporting = if (!ui.canEditIdentity) "Solo el colegio puede cambiar tu nombre." else null,
            )
            ProfileField(
                label = "Apellido",
                value = ui.apellido,
                onValueChange = vm::onApellido,
                enabled = ui.canEditIdentity,
                supporting = if (!ui.canEditIdentity) "Solo el colegio puede cambiar tu apellido." else null,
            )
            ProfileField(
                label = "Cédula",
                value = ui.ci,
                onValueChange = vm::onCi,
                enabled = ui.canEditIdentity,
                keyboardType = KeyboardType.Number,
                supporting = if (!ui.canEditIdentity) "Solo el colegio puede cambiar tu cédula." else null,
            )
        }

        Panel {
            Eyebrow("02 · Contacto")
            Text(
                "Canales para comunicaciones del colegio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            ProfileField(
                label = "Correo",
                value = ui.correo,
                onValueChange = vm::onCorreo,
                keyboardType = KeyboardType.Email,
            )
            ProfileField(
                label = "Teléfono",
                value = ui.telefono,
                onValueChange = vm::onTelefono,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            )
        }

        Panel {
            Eyebrow("03 · Cuenta")
            Text(
                "Nombre de usuario para iniciar sesión.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            ProfileField(
                label = "Usuario",
                value = ui.usuario,
                onValueChange = {},
                enabled = false,
            )
            if (ui.roleLabel.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Rol asignado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        ui.roleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        ui.saveError?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (ui.saved && ui.saveError == null) {
            Text(
                "Cambios guardados.",
                style = MaterialTheme.typography.bodySmall,
                color = scaColors.success,
                fontWeight = FontWeight.Bold,
            )
        }

        Button(
            onClick = vm::save,
            enabled = !ui.saving,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(if (ui.saving) "Guardando…" else "Guardar cambios", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    supporting: String? = null,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            supportingText = supporting?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
