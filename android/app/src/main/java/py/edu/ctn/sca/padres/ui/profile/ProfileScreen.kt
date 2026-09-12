package py.edu.ctn.sca.padres.ui.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Bitmap
import android.graphics.Matrix
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoError by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            photoError = null
            val encoded = withContext(Dispatchers.IO) { encodeImageToBase64(context, uri) }
            if (encoded == null) {
                photoError = "No se pudo procesar la imagen."
            } else {
                vm.onFotoPerfil(encoded)
            }
        }
    }

    Column(
        Modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (ui.showFotoPanel) {
            Panel {
                Eyebrow("00 · Foto de perfil")
                Text(
                    "Se mostrará en la barra de navegación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProfilePhotoPreview(
                        base64OrDataUrl = ui.fotoPerfil,
                        fallbackInitials = buildInitials(ui.nombre, ui.apellido, ui.usuario),
                    )
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { photoPicker.launch("image/*") },
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Subir foto", fontWeight = FontWeight.Bold) }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { vm.onFotoPerfil("") },
                            shape = RoundedCornerShape(9.dp),
                            enabled = !ui.fotoPerfil.isNullOrBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Quitar foto") }
                    }
                }
                photoError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

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

        Panel {
            Eyebrow("04 · Seguridad")
            Text(
                "Cambiá la contraseña de tu cuenta. Se cerrarán tus otras sesiones.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PasswordField(
                label = "Contraseña actual",
                value = ui.currentPassword,
                onValueChange = vm::onCurrentPassword,
            )
            Spacer(Modifier.height(10.dp))
            PasswordField(
                label = "Nueva contraseña",
                value = ui.newPassword,
                onValueChange = vm::onNewPassword,
            )
            Spacer(Modifier.height(10.dp))
            PasswordField(
                label = "Confirmar nueva contraseña",
                value = ui.confirmPassword,
                onValueChange = vm::onConfirmPassword,
                imeAction = ImeAction.Done,
            )
            ui.passwordError?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (ui.passwordChanged && ui.passwordError == null) {
                Text(
                    "Contraseña actualizada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scaColors.success,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = vm::changePassword,
                enabled = !ui.changingPassword,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    if (ui.changingPassword) "Actualizando…" else "Cambiar contraseña",
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Panel {
            Eyebrow("05 · Registros")
            Text(
                "Actividad reciente de tu cuenta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            if (ui.activityLog.isEmpty()) {
                Text(
                    "Todavía no hay actividad registrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.activityLog.forEach { entry ->
                        Text(
                            entry,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    var visible by remember { mutableStateOf(false) }
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
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
            trailingIcon = {
                androidx.compose.material3.TextButton(onClick = { visible = !visible }) {
                    Text(if (visible) "Ocultar" else "Ver")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfilePhotoPreview(base64OrDataUrl: String?, fallbackInitials: String) {
    val bitmap = remember(base64OrDataUrl) { decodeDataUrlToBitmap(base64OrDataUrl) }
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                fallbackInitials,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildInitials(nombre: String, apellido: String, usuario: String): String {
    val a = nombre.firstOrNull()?.takeIf { it.isLetter() }
        ?: usuario.firstOrNull()?.takeIf { it.isLetter() }
        ?: 'S'
    val b = apellido.firstOrNull()?.takeIf { it.isLetter() } ?: ' '
    return "$a$b".trim().uppercase()
}

private fun decodeDataUrlToBitmap(value: String?): Bitmap? {
    if (value.isNullOrBlank()) return null
    val commaIdx = value.indexOf(',')
    val payload = if (commaIdx >= 0) value.substring(commaIdx + 1) else value
    return try {
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Throwable) {
        null
    }
}

/**
 * Lee la imagen elegida, la reescala a 512 px (lado mayor) y la comprime a JPEG
 * al 82 % para mantener el payload bien por debajo del límite del backend (1.5 MB).
 */
private fun encodeImageToBase64(context: Context, uri: Uri): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        val maxSide = 512
        val sample = generateSequence(1) { it * 2 }
            .first { it * maxSide >= maxOf(srcW, srcH) / 2 }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
        val scaled = if (scale < 1f) {
            val m = Matrix().apply { postScale(scale, scale) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        } else decoded

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (_: Throwable) {
        null
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
