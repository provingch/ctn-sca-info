package py.edu.ctn.sca.padres.ui.profesor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import py.edu.ctn.sca.padres.data.profesor.AlumnoRiesgoDto
import py.edu.ctn.sca.padres.data.profesor.BloqueHoyDto
import py.edu.ctn.sca.padres.data.profesor.PlanHistorialDto
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ListCard
import py.edu.ctn.sca.padres.ui.common.ListRow
import py.edu.ctn.sca.padres.ui.common.MetricTile
import py.edu.ctn.sca.padres.ui.common.ScaCard
import py.edu.ctn.sca.padres.ui.common.ScaChip
import py.edu.ctn.sca.padres.ui.common.SectionLabel
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.scaColors
import java.time.LocalTime

// ---------------------------------------------------------------- Hoy

@Composable
fun HoyContent(ui: HoyUi, onRetry: () -> Unit, onIniciar: (BloqueHoyDto) -> Unit) {
    Cargable(ui.loading, ui.error, ui.bloques != null, onRetry) {
        val bloques = ui.bloques.orEmpty()
        var plan by remember { mutableStateOf<PlanHistorialDto?>(null) }
        var riesgo by remember { mutableStateOf<List<AlumnoRiesgoDto>?>(null) }
        val proximo = indiceProximo(bloques, LocalTime.now())
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(bloques.count { !it.registrada }.toString(), "Clases por registrar", Modifier.fillMaxWidth())
            SectionLabel("Tus clases de hoy")
            if (bloques.isEmpty()) {
                ScaCard { Text("No tenés clases cargadas en tu horario para hoy.", color = scaColors.muted, style = MaterialTheme.typography.bodyMedium) }
            } else {
                ListCard(bloques.indices.toList()) { i ->
                    val b = bloques[i]
                    ListRow(
                        title = b.materiaNombre,
                        subtitle = listOfNotNull(b.cursoDescripcion, b.salaNombre?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        leading = { Text(hora(b.horaInicio), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(min = 44.dp)) },
                        titleWeight = FontWeight.SemiBold,
                        trailing = {
                            when {
                                b.registrada -> Tag("Registrada", scaColors.success)
                                i == proximo -> Button({ onIniciar(b) }, shape = RoundedCornerShape(6.dp), modifier = Modifier.height(40.dp)) { Text("Iniciar", fontWeight = FontWeight.SemiBold) }
                                else -> OutlinedButton(
                                    { onIniciar(b) }, shape = RoundedCornerShape(6.dp), modifier = Modifier.height(40.dp),
                                    border = BorderStroke(1.dp, scaColors.lineStrong),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                ) { Text("Iniciar", fontWeight = FontWeight.SemiBold) }
                            }
                        },
                    )
                }
            }
            val grupos = riesgoPorCurso(ui.riesgo)
            if (ui.planes.isNotEmpty() || grupos.isNotEmpty()) {
                SectionLabel("Pendiente")
                ListCard(ui.planes.map { Either.Plan(it) } + grupos.map { Either.Riesgo(it) }) { item ->
                    when (item) {
                        is Either.Plan -> ListRow(
                            title = "${item.p.materiaNombre} ${item.p.cursoOrdinal} ${item.p.seccion}".trim(),
                            subtitle = "Rechazado · " + (item.p.observacionesEvaluador?.lineSequence()?.firstOrNull { it.isNotBlank() } ?: "corregí y volvé a subir"),
                            leading = { Tag("Plan", scaColors.danger) }, onClick = { plan = item.p },
                        )
                        is Either.Riesgo -> {
                            val f = item.alumnos.first()
                            val n = item.alumnos.size
                            ListRow(
                                title = "$n ${if (n == 1) "alumno en riesgo" else "alumnos en riesgo"}",
                                subtitle = "${f.cursoNombre} ${f.especialidadNombre} ${f.seccion}".trim(),
                                leading = { Tag("Riesgo", scaColors.warning) }, onClick = { riesgo = item.alumnos },
                            )
                        }
                    }
                }
            }
        }
        plan?.let { p ->
            AlertDialog(
                onDismissRequest = { plan = null },
                title = { Text("${p.materiaNombre} ${p.cursoOrdinal} ${p.seccion}".trim(), style = MaterialTheme.typography.titleMedium) },
                text = { Text(p.observacionesEvaluador?.takeIf { it.isNotBlank() } ?: "El evaluador rechazó el plan. Corregilo y volvé a subirlo desde la web.") },
                confirmButton = { TextButton({ plan = null }) { Text("Cerrar") } },
            )
        }
        riesgo?.let { lista ->
            AlertDialog(
                onDismissRequest = { riesgo = null },
                title = { Text("Alumnos en riesgo", style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lista.forEach { a ->
                            Column {
                                Text(a.nombreCompleto, fontWeight = FontWeight.Medium)
                                Text(
                                    a.motivos.joinToString(" · ").ifEmpty { "${a.tareasNoEntregadas} tareas sin entregar · ${a.notasConductuales} notas de conducta" },
                                    style = MaterialTheme.typography.bodySmall, color = scaColors.muted,
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton({ riesgo = null }) { Text("Cerrar") } },
            )
        }
    }
}

private sealed interface Either {
    data class Plan(val p: PlanHistorialDto) : Either
    data class Riesgo(val alumnos: List<AlumnoRiesgoDto>) : Either
}

// ---------------------------------------------------------------- Clases

@Composable
fun ClasesContent(ui: ClasesUi, onRetry: () -> Unit, onClase: (Int) -> Unit) {
    Cargable(ui.loading, ui.error, ui.clases != null, onRetry) {
        val grupos = clasesPorFecha(ui.clases.orEmpty())
        if (grupos.isEmpty()) {
            EmptyState("Todavía no registraste clases")
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grupos.forEach { (fecha, clases) ->
                    item { SectionLabel(fecha?.let(::fechaLarga) ?: "Sin fecha") }
                    item {
                        ListCard(clases) { c ->
                            ListRow(
                                title = c.materiaNombre ?: c.tema,
                                subtitle = listOf(c.cursoDescripcion, c.tema.takeIf { c.materiaNombre != null }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                                onClick = { onClase(c.id) },
                                trailing = if (c.totalAusentes > 0) ({ Tag("${c.totalAusentes} aus.", scaColors.danger) }) else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClaseDetalleContent(ui: ClaseDetalleUi, onRetry: () -> Unit) {
    Cargable(ui.loading, ui.error, ui.clase != null, onRetry) {
        val c = ui.clase!!
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScaCard {
                Text("Contenido desarrollado", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                Text(c.tema.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            SectionLabel("Asistencia")
            if (c.asistencias.isEmpty()) {
                ScaCard { Text("Sin asistencias registradas.", color = scaColors.muted) }
            } else {
                ListCard(c.asistencias) { a ->
                    ListRow(
                        title = a.alumnoNombreCompleto,
                        subtitle = a.codigos.takeIf { it.isNotEmpty() }?.joinToString(" "),
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                a.codigos.forEach { Tag(it, scaColors.warning) }
                                when (a.estado) {
                                    "presente" -> Tag("Presente", scaColors.success)
                                    "ausente_justificado" -> Tag("Justificado", scaColors.info)
                                    else -> Tag("Ausente", scaColors.danger)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Planillas / tareas

@Composable
fun PlanillasContent(ui: PlanillasUi, onRetry: () -> Unit, onEtapa: (Int) -> Unit, onPlanilla: (Int) -> Unit) {
    Column {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScaChip("1ra etapa", ui.etapa == 1, { onEtapa(1) })
            ScaChip("2da etapa", ui.etapa == 2, { onEtapa(2) })
        }
        Cargable(ui.loading, ui.error, ui.planillas != null, onRetry) {
            val l = ui.planillas.orEmpty()
            if (l.isEmpty()) {
                EmptyState("No tenés planillas en esta etapa")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    ListCard(l) { p ->
                        ListRow(
                            title = p.materiaNombre,
                            subtitle = "${p.cursoOrdinal} ${p.seccion} · ${p.especialidadNombre}".trim(),
                            onClick = { onPlanilla(p.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TareasContent(ui: TareasUi, onRetry: () -> Unit, onTarea: (Int) -> Unit) {
    Cargable(ui.loading, ui.error, ui.tareas != null, onRetry) {
        val tareas = ui.tareas.orEmpty()
        if (tareas.isEmpty()) {
            EmptyState("Esta planilla no tiene tareas")
        } else {
            val rows = ui.detalle?.rows.orEmpty()
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                ListCard(tareas) { t ->
                    val cargadas = rows.count { r -> r.grades.any { it.tareaId == t.id && it.puntos != null } }
                    ListRow(
                        title = t.titulo.ifBlank { "Tarea ${t.id}" },
                        subtitle = listOfNotNull(fechaCorta(t.fecha), if (rows.isNotEmpty()) "$cargadas de ${rows.size} cargadas" else null).joinToString(" · "),
                        onClick = { onTarea(t.id) },
                        trailing = if (t.googleCourseworkId != null) ({ Tag("Classroom", scaColors.info) }) else null,
                    )
                }
            }
        }
    }
}
