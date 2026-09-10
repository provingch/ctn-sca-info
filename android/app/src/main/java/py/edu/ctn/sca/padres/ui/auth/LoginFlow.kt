package py.edu.ctn.sca.padres.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
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
import py.edu.ctn.sca.padres.R
import py.edu.ctn.sca.padres.ui.components.AccentTopCard
import py.edu.ctn.sca.padres.ui.graphViewModel
import py.edu.ctn.sca.padres.ui.theme.scaColors

/**
 * Login, mirroring the web `.auth-page`: a gradient ground, the brand mark and
 * tagline stacked above a paper card whose top edge carries a 5px accent strip.
 */
@Composable
fun LoginFlow(graph: Graph) {
    val vm: AuthViewModel = graphViewModel { AuthViewModel(it.authRepository, it.pushRepository) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(scaColors.authGradient)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(24.dp))
            AccentTopCard {
                when (ui.stage) {
                    AuthUiState.Stage.CREDENTIALS -> CredentialsForm(ui, vm)
                    AuthUiState.Stage.TWO_FACTOR -> TwoFactorForm(ui, vm)
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.sca_logo),
            contentDescription = "Sistema de Carpetas Académicas",
            modifier = Modifier.height(72.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Sistema de Carpetas Académicas".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "La gestión académica, clara y conectada.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Colegio Técnico Nacional · Asunción",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
    unfocusedContainerColor = scaColors.bgSoft,
    focusedContainerColor = scaColors.bgSoft,
    disabledContainerColor = scaColors.bgSoft,
)

@Composable
private fun CredentialsForm(ui: AuthUiState, vm: AuthViewModel) {
    var passwordVisible by remember { mutableStateOf(false) }

    Text(
        "Iniciar sesión",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))

    FieldLabel("Usuario o Cédula")
    OutlinedTextField(
        value = ui.username,
        onValueChange = vm::onUsername,
        singleLine = true,
        enabled = !ui.loading,
        shape = RoundedCornerShape(9.dp),
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))

    FieldLabel("Contraseña")
    OutlinedTextField(
        value = ui.password,
        onValueChange = vm::onPassword,
        singleLine = true,
        enabled = !ui.loading,
        shape = RoundedCornerShape(9.dp),
        colors = fieldColors(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))

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
        Spacer(Modifier.width(6.dp))
        Text("Recordarme en este dispositivo", style = MaterialTheme.typography.bodyMedium)
    }

    ui.error?.let { msg ->
        Spacer(Modifier.height(14.dp))
        AuthFeedback(
            title = if (ui.isLocked) "Acceso temporalmente pausado" else "No se pudo iniciar sesión",
            message = msg,
            locked = ui.isLocked,
        )
    }

    Spacer(Modifier.height(20.dp))
    SubmitButton(
        text = "Ingresar",
        loadingText = "Ingresando…",
        loading = ui.loading,
        enabled = !ui.loading && ui.username.isNotBlank() && ui.password.isNotBlank() && !ui.isLocked,
        onClick = vm::submitCredentials,
    )
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
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(10.dp))
            Text(loadingText, fontWeight = FontWeight.Bold)
        } else {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

/** Web `.auth-feedback`: tinted notice with a coloured left rule. */
@Composable
private fun AuthFeedback(title: String, message: String, locked: Boolean) {
    val tone = if (locked) scaColors.warning else MaterialTheme.colorScheme.error
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
