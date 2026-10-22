package py.edu.ctn.sca.padres.ui.profesor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import py.edu.ctn.sca.padres.ui.common.ErrorState
import py.edu.ctn.sca.padres.ui.common.LoadingState
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** Carga / error con reintento / contenido. Con datos previos y error, se sigue mostrando el contenido. */
@Composable
internal fun Cargable(loading: Boolean, error: String?, hasData: Boolean, onRetry: () -> Unit, content: @Composable () -> Unit) {
    when {
        !hasData && error != null -> ErrorState(error, onRetry)
        !hasData && loading -> LoadingState()
        !hasData -> Box(Modifier.fillMaxWidth())
        else -> content()
    }
}

/** Campo desplegable de una sola elección: etiqueta arriba, valor + flecha. */
@Composable
internal fun Desplegable(
    label: String,
    valor: String?,
    placeholder: String,
    opciones: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                onClick = { abierto = true },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, scaColors.lineStrong),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 12.dp, end = 6.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        valor ?: placeholder,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (valor != null) FontWeight.Medium else FontWeight.Normal,
                        color = if (valor != null) MaterialTheme.colorScheme.onSurface else scaColors.muted,
                        maxLines = 1,
                    )
                    Icon(Icons.Filled.ArrowDropDown, null, tint = scaColors.muted)
                }
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEachIndexed { i, o ->
                    DropdownMenuItem(text = { Text(o) }, onClick = { abierto = false; onSelect(i) })
                }
            }
        }
    }
}

@Composable
internal fun BarraResumen(resumen: androidx.compose.ui.text.AnnotatedString, modifier: Modifier = Modifier, accion: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(resumen, style = MaterialTheme.typography.bodyMedium, color = scaColors.muted, modifier = Modifier.weight(1f).padding(end = 12.dp))
        accion()
    }
}
