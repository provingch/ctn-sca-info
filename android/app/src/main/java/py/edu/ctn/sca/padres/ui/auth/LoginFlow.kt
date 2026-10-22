package py.edu.ctn.sca.padres.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.ui.common.AccentTopLine
import py.edu.ctn.sca.padres.ui.common.PrimaryButton
import py.edu.ctn.sca.padres.ui.common.ScaCard
import py.edu.ctn.sca.padres.ui.graphViewModel
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** Login (diseño m-38): acento azul, una sola pantalla para todos los roles. */
@Composable
fun LoginFlow(graph: Graph) {
    val vm: AuthViewModel = graphViewModel { AuthViewModel(it.authRepository, it.pushRepository) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
            AccentTopLine()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("SCA", style = MaterialTheme.typography.titleLarge)
                    Text("Colegio Técnico Nacional", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                }
            }
        }
        HorizontalDivider(color = scaColors.line)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Ancho acotado para que en tablets/foldables el formulario no se estire.
            Column(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                when (ui.stage) {
                    AuthUiState.Stage.CREDENTIALS -> CredentialsForm(ui, vm)
                    AuthUiState.Stage.TWO_FACTOR -> TwoFactorForm(ui, vm)
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = scaColors.line,
)

@Composable
private fun CredentialsForm(ui: AuthUiState, vm: AuthViewModel) {
    var passwordVisible by remember { mutableStateOf(false) }

    Icon(Icons.Filled.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
    Spacer(Modifier.height(24.dp))
    Text(
        "Ingresá con tu usuario del SCA",
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "Una sola app para profesores, familias, evaluación y coordinación. Ves lo que corresponde a tu rol.",
        style = MaterialTheme.typography.bodyLarge,
        color = scaColors.muted,
    )
    Spacer(Modifier.height(24.dp))

    FieldLabel("Usuario o cédula")
    OutlinedTextField(
        value = ui.username,
        onValueChange = vm::onUsername,
        singleLine = true,
        enabled = !ui.loading,
        shape = RoundedCornerShape(6.dp),
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))

    FieldLabel("Contraseña")
    OutlinedTextField(
        value = ui.password,
        onValueChange = vm::onPassword,
        singleLine = true,
        enabled = !ui.loading,
        shape = RoundedCornerShape(6.dp),
        colors = fieldColors(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "Ocultar" else "Mostrar", color = scaColors.muted, fontWeight = FontWeight.Medium)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = ui.rememberMe,
                enabled = !ui.loading,
                onValueChange = vm::onRememberMe,
            ),
    ) {
        Checkbox(checked = ui.rememberMe, onCheckedChange = null)
        Spacer(Modifier.width(10.dp))
        Text("Recordarme en este celular", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }

    ui.error?.let { msg ->
        Spacer(Modifier.height(14.dp))
        AuthFeedback(
            title = if (ui.isLocked) "Acceso temporalmente pausado" else "No se pudo iniciar sesión",
            message = msg,
            locked = ui.isLocked,
        )
    }

    Spacer(Modifier.height(16.dp))
    PrimaryButton(
        text = if (ui.loading) "Ingresando…" else "Ingresar",
        onClick = vm::submitCredentials,
        enabled = !ui.loading && ui.username.isNotBlank() && ui.password.isNotBlank() && !ui.isLocked,
        loading = ui.loading,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "¿Olvidaste la contraseña? Pedila en Administración.",
        style = MaterialTheme.typography.bodySmall,
        color = scaColors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    ScaCard(contentPadding = 14.dp) {
        Text("Las cuentas de Administración se usan desde la computadora, en la web del SCA.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TwoFactorForm(ui: AuthUiState, vm: AuthViewModel) {
    Text(
        "Verificación en dos pasos",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Ingresá el código de tu app de autenticación.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))

    FieldLabel("Código")
    OutlinedTextField(
        value = ui.code,
        onValueChange = vm::onCode,
        singleLine = true,
        enabled = !ui.loading,
        shape = RoundedCornerShape(9.dp),
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )

    ui.error?.let { msg ->
        Spacer(Modifier.height(14.dp))
        AuthFeedback(
            title = if (ui.isLocked) "Acceso temporalmente pausado" else "No pudimos verificar el código",
            message = msg,
            locked = ui.isLocked,
        )
    }

    Spacer(Modifier.height(20.dp))
    SubmitButton(
        text = "Verificar",
        loadingText = "Verificando…",
        loading = ui.loading,
        enabled = !ui.loading && ui.code.length == 6 && !ui.isLocked,
        onClick = vm::submitCode,
    )
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = vm::backToCredentials, enabled = !ui.loading) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.height(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Volver")
        }
    }
}

@Composable
private fun SubmitButton(
    text: String,
    loadingText: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) = PrimaryButton(
    text = if (loading) loadingText else text,
    onClick = onClick,
    enabled = enabled,
    loading = loading,
    modifier = Modifier.fillMaxWidth(),
)

/** Web `.auth-feedback`: tinted notice with a coloured left rule. */
@Composable
private fun AuthFeedback(title: String, message: String, locked: Boolean) {
    val tone = if (locked) scaColors.warning.fg else MaterialTheme.colorScheme.error
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(tone.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(tone),
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = tone,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
