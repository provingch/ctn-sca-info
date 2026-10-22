package py.edu.ctn.sca.padres.ui.padre

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.ReportRepository
import py.edu.ctn.sca.padres.data.ReportResult
import py.edu.ctn.sca.padres.data.TaskDto
import py.edu.ctn.sca.padres.ui.common.ChipRow
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ScaCard
import py.edu.ctn.sca.padres.ui.common.ScaChip
import py.edu.ctn.sca.padres.ui.common.SecondaryButton
import py.edu.ctn.sca.padres.ui.common.SectionLabel
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.parent.Stage
import py.edu.ctn.sca.padres.ui.parent.agruparTareasPorMes
import py.edu.ctn.sca.padres.ui.theme.scaColors
import java.io.File
import java.time.LocalDate

private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

/** Notas por materia: etapa, materia, tareas del mes y reportes PDF. */
@Composable
fun NotasContent(ui: PadreUiState, vm: PadreViewModel, reports: ReportRepository) {
    ChipRow {
        Stage.entries.forEach { s -> ScaChip(s.label, s == ui.stage, { vm.selectStage(s) }) }
    }
    val materias = ui.subjectsForStage
    if (materias.isEmpty()) {
        EmptyState("Sin notas en esta etapa", message = "Cuando los profesores carguen tareas, las vas a ver acá.", modifier = Modifier.padding(vertical = 32.dp))
    } else {
        val sel = ui.selectedSubject
        ChipRow {
            materias.forEach { m -> ScaChip(m.materia, m.planillaId == sel?.planillaId, { vm.selectSubject(m.planillaId) }) }
        }
        if (sel != null) {
            ScaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sel.materia, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("${sel.puntos} / ${sel.total} puntos · ${sel.porcentaje}%", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                    }
                    if (sel.nota > 0) Tag("Nota ${sel.nota}", scaColors.grade(sel.nota))
                }
            }
            if (sel.tareas.isEmpty()) {
                Text("Esta materia todavía no tiene tareas.", color = scaColors.muted, style = MaterialTheme.typography.bodyMedium)
            }
            agruparTareasPorMes(sel.tareas).forEach { grupo ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(grupo.label)
                    ScaCard(contentPadding = 0.dp) {
                        grupo.tareas.forEachIndexed { i, t ->
                            if (i > 0) HorizontalDivider(color = scaColors.line)
                            TareaRow(t.tarea)
                        }
                    }
                }
            }
        }
    }
    ReportesCard(ui, reports)
}

@Composable
private fun TareaRow(task: TaskDto) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(task.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(fechaCorta(task.fecha), style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
        }
        when (task.estado) {
            "CALIFICADA" -> {
                // Puntaje sobre 10 para el color, como en el resto del diseño.
                val sobre10 = if (task.total > 0) (task.puntos ?: 0) * 10.0 / task.total else 0.0
                Tag("${task.puntos ?: 0} / ${task.total}", scaColors.score10(sobre10))
            }
            "ENTREGADA_PENDIENTE" -> Tag("Entregada · sin calificar", scaColors.warning)
            "NO_ENTREGADA" -> Tag("No entregada", scaColors.danger)
            else -> Tag("Pendiente · ${task.total} pts", scaColors.info)
        }
    }
}

private fun fechaCorta(value: String?): String =
    runCatching { LocalDate.parse(value).let { "%02d/%02d/%02d".format(it.dayOfMonth, it.monthValue, it.year % 100) } }
        .getOrDefault("Sin fecha")

@Composable
private fun ReportesCard(ui: PadreUiState, reports: ReportRepository) {
    val alumnoId = ui.selectedId ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mes by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val libreta = ui.summary?.libretaDisponible == true

    fun run(kind: String, block: suspend () -> ReportResult) {
        if (busy != null) return
        busy = kind
        message = null
        scope.launch {
            message = when (val r = block()) {
                is ReportResult.Ok -> openPdf(context, r.file)
                is ReportResult.Error -> r.message
            }
            busy = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("Reportes")
        ScaCard(Modifier, contentPadding = 12.dp) {
            ChipRow {
                MESES.forEachIndexed { i, m -> ScaChip(m, i + 1 == mes, { mes = i + 1 }) }
            }
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    if (busy == "mensual") "Generando…" else "Reporte mensual · ${MESES[mes - 1]}",
                    { run("mensual") { reports.reporteMensual(alumnoId, mes, LocalDate.now().year) } },
                    Modifier.fillMaxWidth(), enabled = busy == null,
                )
                SecondaryButton(
                    if (busy == "libreta") "Generando…" else "Libreta",
                    { run("libreta") { reports.libreta(alumnoId) } },
                    Modifier.fillMaxWidth(), enabled = busy == null && libreta,
                )
                if (!libreta) {
                    Text("La libreta estará disponible cuando el colegio cierre la Segunda Etapa.", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scaColors.danger.fg) }
            }
        }
    }
}

/** Abre el PDF con un visor externo vía FileProvider. Devuelve un mensaje si falla, o null. */
private fun openPdf(context: Context, file: File): String? = try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
    )
    null
} catch (e: ActivityNotFoundException) {
    "No hay una app instalada para abrir PDF. El archivo quedó guardado en la app."
} catch (e: Exception) {
    "No se pudo abrir el PDF."
}
