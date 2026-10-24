package py.edu.ctn.sca.padres.ui.profesor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import py.edu.ctn.sca.padres.data.profesor.StudentRowDto
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.SecondaryButton
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.scaColors

@Composable
fun CorreccionScreen(vm: CorreccionViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val t = ui.tarea
    val d = ui.detalle
    val subtitulo = if (t != null && d != null) {
        listOfNotNull(
            d.planilla.materiaNombre.takeIf { it.isNotBlank() },
            d.curso?.let { "${it.nivel}° ${it.seccion}" },
            "${d.planilla.etapaIndex}${if (d.planilla.etapaIndex == 1) "ra" else "da"} etapa",
        ).joinToString(" · ")
    } else null
    RoleScaffold(
        title = t?.titulo?.ifBlank { null } ?: "Corrección",
        subtitle = subtitulo,
        onBack = onBack,
        bottomAction = if (t != null && ui.rows.isNotEmpty()) ({
            BarraResumen(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) { append("${ui.cargadas} de ${ui.rows.size}") }
                append(" cargadas")
            }) { SecondaryButton("Siguiente sin nota", vm::siguienteSinNota, enabled = ui.cargadas < ui.rows.size) }
        }) else null,
    ) {
        Cargable(ui.loading, ui.error, t != null, vm::load) {
            if (ui.rows.isEmpty()) EmptyState("Esta planilla no tiene alumnos") else Lista(ui, vm)
        }
    }
}

@Composable
private fun Lista(ui: CorreccionUi, vm: CorreccionViewModel) {
    val t = ui.tarea!!
    val rows = ui.rows
    val state = rememberLazyListState()
    LaunchedEffect(ui.focusAlumnoId) {
        val i = rows.indexOfFirst { it.alumnoId == ui.focusAlumnoId }
        if (i >= 0) state.animateScrollToItem(i + 1)
    }
    LazyColumn(state = state, contentPadding = PaddingValues(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (t.googleCourseworkId != null) {
                    Tag("Classroom", scaColors.info)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "${ui.cargadas} cargadas · ${rows.size - ui.cargadas} sin nota",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = scaColors.muted,
                )
                Text(
                    if (ui.ordenSinNotaPrimero) "Orden original" else "Ordenar",
                    Modifier.clickable(onClick = vm::toggleOrden).padding(4.dp),
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.Underline,
                )
            }
        }
        items(rows, key = { it.alumnoId }) { r ->
            Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
                FilaNota(r, ui.notas[r.alumnoId], t.total, ui.focusAlumnoId == r.alumnoId, t.googleCourseworkId == null) { vm.setNota(r.alumnoId, it) }
            }
            HorizontalDivider(color = scaColors.line)
        }
        item {
            Column(Modifier.padding(top = 12.dp)) {
                ui.saveError?.let { Text(it, color = scaColors.danger.fg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp)) }
                Text(
                    "Escala: 1–4 rojo · 5–6 naranja · 7–8 verde claro · 9–10 verde. " +
                        if (t.googleCourseworkId != null) "Las notas de esta tarea vienen de Google Classroom: solo se pueden editar allá." else "Los cambios se guardan solos.",
                    style = MaterialTheme.typography.bodySmall, color = scaColors.muted,
                )
            }
        }
    }
}

@Composable
private fun FilaNota(r: StudentRowDto, puntos: Int?, total: Int, enfocar: Boolean, editable: Boolean, onChange: (String) -> Unit) {
    val tone = puntos?.let { scaColors.score10(sobreDiez(it, total)) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(enfocar) { if (enfocar) runCatching { fr.requestFocus() } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(r.alumnoNombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(if (puntos == null) "Sin nota" else "Con nota", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
        }
        Surface(
            shape = RoundedCornerShape(6.dp), color = tone?.bg ?: MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, tone?.fg ?: MaterialTheme.colorScheme.primary),
        ) {
            BasicTextField(
                value = puntos?.toString() ?: "", onValueChange = onChange, singleLine = true, readOnly = !editable,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = tone?.fg ?: MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize, textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.width(52.dp).height(36.dp).focusRequester(fr),
                decorationBox = { inner -> Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) { inner() } },
            )
        }
        Text("/$total", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
    }
}

