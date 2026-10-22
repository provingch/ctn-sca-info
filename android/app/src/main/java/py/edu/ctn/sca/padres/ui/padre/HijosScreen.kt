package py.edu.ctn.sca.padres.ui.padre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ListRow
import py.edu.ctn.sca.padres.ui.common.MetricTile
import py.edu.ctn.sca.padres.ui.common.ScaCard
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** m-44: métricas, tabla de materias y última anotación. */
@Composable
fun HijosContent(ui: PadreUiState, vm: PadreViewModel, onOpen: (Int) -> Unit) {
    val sinEntregar = ui.tareasSinEntregar
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricTile("${ui.hijo?.promedio ?: 0}%", "Promedio general", Modifier.weight(1f))
        MetricTile(
            sinEntregar.toString(),
            if (sinEntregar == 1) "Tarea sin entregar" else "Tareas sin entregar",
            Modifier.weight(1f),
            valueColor = if (sinEntregar > 0) scaColors.danger.fg else null,
        )
    }

    val materias = ui.subjectsForStage
    if (materias.isEmpty()) {
        ScaCard { Text("Todavía no hay notas cargadas en esta etapa.", color = scaColors.muted, style = MaterialTheme.typography.bodyMedium) }
    } else {
        ScaCard(contentPadding = 0.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                HeaderCell("Materia", Modifier.weight(1f))
                HeaderCell("%", Modifier.width(56.dp), TextAlign.End)
                HeaderCell("Nota", Modifier.width(64.dp), TextAlign.Center)
            }
            materias.forEach { m ->
                HorizontalDivider(color = scaColors.line)
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.materia, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${m.porcentaje}%", Modifier.width(56.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.width(64.dp), horizontalArrangement = Arrangement.Center) {
                        if (m.nota > 0) Tag(m.nota.toString(), scaColors.grade(m.nota))
                        else Text("–", color = scaColors.muted)
                    }
                }
            }
        }
    }

    ui.ultimaAnotacion?.let { a ->
        ScaCard(contentPadding = 0.dp) {
            ListRow(
                title = "${a.tipo} · ${a.observacion.orEmpty()}",
                subtitle = cuandoConducta(anotacionItem(a)) + " · " + origenAnotacion(a),
                onClick = { onOpen(a.id) },
                leading = { FotoThumb(vm, ui.selectedId ?: a.alumnoId, a) },
                titleWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text.uppercase(), modifier, textAlign = align, color = scaColors.muted,
        style = MaterialTheme.typography.labelSmall, letterSpacing = 0.6.sp,
    )
}
