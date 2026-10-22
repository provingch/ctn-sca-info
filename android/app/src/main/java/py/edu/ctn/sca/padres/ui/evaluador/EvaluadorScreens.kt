package py.edu.ctn.sca.padres.ui.evaluador

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import py.edu.ctn.sca.padres.data.evaluador.Categoria
import py.edu.ctn.sca.padres.data.evaluador.IncumplimientoDto
import py.edu.ctn.sca.padres.data.evaluador.PlanResumenDto
import py.edu.ctn.sca.padres.data.evaluador.ResolucionDto
import py.edu.ctn.sca.padres.data.evaluador.categoriaDe
import py.edu.ctn.sca.padres.data.evaluador.esBloqueo
import py.edu.ctn.sca.padres.data.evaluador.fechaCorta
import py.edu.ctn.sca.padres.data.evaluador.filtrarIncumplimientos
import py.edu.ctn.sca.padres.data.evaluador.mesesDelPlan
import py.edu.ctn.sca.padres.data.evaluador.profesor
import py.edu.ctn.sca.padres.ui.common.ChipRow
import py.edu.ctn.sca.padres.ui.common.DangerTextButton
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ErrorState
import py.edu.ctn.sca.padres.ui.common.ListCard
import py.edu.ctn.sca.padres.ui.common.ListRow
import py.edu.ctn.sca.padres.ui.common.LoadingState
import py.edu.ctn.sca.padres.ui.common.MetricTile
import py.edu.ctn.sca.padres.ui.common.PrimaryButton
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.ScaChip
import py.edu.ctn.sca.padres.ui.common.SectionLabel
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.scaColors
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale.forLanguageTag("es-PY")

fun diaLargo(hoy: LocalDate = LocalDate.now()): String =
    hoy.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES))

private fun planSubtitle(p: PlanResumenDto) =
    "${p.materiaNombre} ${p.cursoDescripcion}".trim() + (fechaCorta(p.fechaSubida)?.let { " · subido $it" } ?: "")

private fun IncumplimientoDto.subtitle() =
    listOfNotNull(materiaNombre?.takeIf { it.isNotBlank() }, descripcion.takeIf { it.isNotBlank() }).joinToString(" · ")

@Composable
private fun IncumplimientoTag(item: IncumplimientoDto) = when (categoriaDe(item.tipo)) {
    Categoria.BLOQUEOS -> Tag("Bloqueo", scaColors.danger)
    Categoria.ATRASOS -> Tag("Atraso", scaColors.warning)
    Categoria.RETROACTIVAS -> Tag("Retroactiva", scaColors.warning)
    null -> Tag("Caso", scaColors.info)
}

@Composable
private fun <T> LoadBody(state: Load<T>, retry: () -> Unit, content: @Composable (T) -> Unit) = when (state) {
    Load.Loading -> LoadingState()
    is Load.Error -> ErrorState(state.message, retry)
    is Load.Ok -> content(state.data)
}

// ---- Inicio ----

@Composable
fun InicioScreen(ui: EvaluadorUi, retry: () -> Unit, onPlan: (Int) -> Unit, onIncumplimiento: () -> Unit) {
    val planesS = ui.planes
    val incS = ui.incumplimientos
    when {
        planesS is Load.Error -> ErrorState(planesS.message, retry)
        incS is Load.Error -> ErrorState(incS.message, retry)
        planesS !is Load.Ok || incS !is Load.Ok -> LoadingState()
        else -> {
            val planes = planesS.data
            val inc = incS.data.sortedBy { if (it.esBloqueo()) 0 else 1 }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricTile(planes.size.toString(), "Planes por revisar", Modifier.weight(1f))
                    val bloqueos = inc.count { it.esBloqueo() }
                    MetricTile(bloqueos.toString(), "Bloqueo de Iniciar clase", Modifier.weight(1f), valueColor = if (bloqueos > 0) scaColors.danger.fg else null)
                }
                SectionLabel("Para revisar hoy")
                if (inc.isEmpty() && planes.isEmpty()) {
                    EmptyState("Todo al día", message = "No hay planes ni casos por revisar.", modifier = Modifier.height(160.dp))
                } else {
                    ListCard(inc.map { it to null } + planes.map { null to it }) { (i, p) ->
                        if (i != null) {
                            ListRow(i.profesor(), subtitle = i.subtitle(), onClick = onIncumplimiento, leading = { IncumplimientoTag(i) })
                        } else if (p != null) {
                            ListRow(p.profesor(), subtitle = planSubtitle(p), onClick = { onPlan(p.id) }, leading = { Tag("Plan", scaColors.info) })
                        }
                    }
                }
            }
        }
    }
}

// ---- Planes ----

@Composable
fun PlanesScreen(state: Load<List<PlanResumenDto>>, retry: () -> Unit, onPlan: (Int) -> Unit) {
    LoadBody(state, retry) { planes ->
        if (planes.isEmpty()) {
            EmptyState("No hay planes por revisar", message = "Los planes que suban los profesores aparecen acá.")
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Pendientes de revisión (${planes.size})")
                ListCard(planes) { p ->
                    ListRow(p.profesor(), subtitle = planSubtitle(p), onClick = { onPlan(p.id) }, leading = { Tag("Plan", scaColors.info) })
                }
            }
        }
    }
}

// ---- Detalle de plan ----

@Composable
fun PlanDetalleScreen(id: Int, vm: EvaluadorViewModel, ui: EvaluadorUi, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(id) { vm.loadDetalle(id) }
    var observacion by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) } // "aprobar" | "rechazar"
    val plan = (ui.detalle as? Load.Ok)?.data
    val pendiente = plan?.estado.equals("PENDIENTE", ignoreCase = true)

    RoleScaffold(
        title = "Plan curricular",
        subtitle = plan?.let { listOf(it.materiaNombre, it.cursoDescripcion, it.profesor()).filter { s -> s.isNotBlank() }.joinToString(" · ") },
        onBack = onBack,
        bottomAction = if (plan != null && pendiente) ({
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DangerTextButton("Rechazar", onClick = {
                    if (observacion.isBlank()) Toast.makeText(context, "Escribí la observación para rechazar.", Toast.LENGTH_SHORT).show()
                    else confirm = "rechazar"
                }, modifier = Modifier.weight(1f), enabled = !ui.busy)
                PrimaryButton("Aprobar", onClick = { confirm = "aprobar" }, modifier = Modifier.weight(1f), loading = ui.busy)
            }
        }) else null,
    ) {
        LoadBody(ui.detalle, { vm.loadDetalle(id) }) { p ->
            val meses = mesesDelPlan(p.temas)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pendiente) Tag("En revisión", scaColors.warning)
                    fechaCorta(p.fechaSubida)?.let { Text("Subido $it", style = MaterialTheme.typography.bodySmall, color = scaColors.muted) }
                }
                if (meses.isEmpty()) {
                    Text("El plan no tiene temas cargados.", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                } else {
                    ListCard(meses) { m -> ListRow(m.mes, subtitle = m.contenidos.ifEmpty { null }, titleWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
                }
                ClickableText(
                    AnnotatedString("Ver el documento completo (Excel)"),
                    style = TextStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, fontSize = MaterialTheme.typography.bodySmall.fontSize),
                    onClick = { if (!ui.busy) vm.abrirDocumento(id) { f -> abrirDocumento(context, f)?.let { m -> Toast.makeText(context, m, Toast.LENGTH_LONG).show() } } },
                )
                if (pendiente) {
                    SectionLabel("Observación para el profesor")
                    OutlinedTextField(
                        observacion, { observacion = it },
                        Modifier.fillMaxWidth(), minLines = 3,
                        placeholder = { Text("Obligatoria si rechazás") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    confirm?.let { accion ->
        val aprobar = accion == "aprobar"
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (aprobar) "¿Aprobar el plan?" else "¿Rechazar el plan?") },
            text = { Text(if (aprobar) "El profesor va a ser notificado." else "Se le enviará tu observación al profesor.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (aprobar) vm.aprobar(id, onBack) else vm.rechazar(id, observacion, onBack)
                }) { Text(if (aprobar) "Aprobar" else "Rechazar") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
        )
    }
}

private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** Abre el documento con un visor externo vía FileProvider. Devuelve un mensaje si falla, o null. */
private fun abrirDocumento(context: Context, file: File): String? = try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, XLSX)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
    null
} catch (e: ActivityNotFoundException) {
    "No hay una app instalada para abrir el documento."
} catch (e: Exception) {
    "No se pudo abrir el documento."
}

// ---- Seguimiento ----

@Composable
fun SeguimientoScreen(ui: EvaluadorUi, retry: () -> Unit, onResolver: (IncumplimientoDto, ResolucionDto) -> Unit) {
    var categoria by rememberSaveable { mutableStateOf<Categoria?>(null) }
    var dialogo by remember { mutableStateOf<IncumplimientoDto?>(null) }
    LoadBody(ui.incumplimientos, retry) { todos ->
        if (todos.isEmpty()) {
            EmptyState("No hay incumplimientos pendientes", message = "Los atrasos, incongruencias y bloqueos por resolver aparecen acá.")
        } else {
            val visibles = filtrarIncumplimientos(todos, categoria)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipRow {
                    ScaChip("Todos ${todos.size}", categoria == null, { categoria = null })
                    Categoria.entries.forEach { c ->
                        ScaChip("${c.label} ${filtrarIncumplimientos(todos, c).size}", categoria == c, { categoria = c })
                    }
                }
                if (visibles.isEmpty()) {
                    Text("Ningún caso coincide con el filtro.", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                } else {
                    ListCard(visibles) { i ->
                        ListRow(
                            i.profesor(), subtitle = i.subtitle(),
                            leading = { IncumplimientoTag(i) },
                            trailing = {
                                TextButton(onClick = { dialogo = i }, enabled = !ui.busy) { Text(if (i.esBloqueo()) "Levantar bloqueo" else "Resolver") }
                            },
                        )
                    }
                }
            }
        }
    }
    dialogo?.let { item ->
        ResolverDialog(item, onDismiss = { dialogo = null }) { r -> dialogo = null; onResolver(item, r) }
    }
}

@Composable
private fun ResolverDialog(item: IncumplimientoDto, onDismiss: () -> Unit, onConfirm: (ResolucionDto) -> Unit) {
    var nota by remember { mutableStateOf("") }
    val bloqueo = item.esBloqueo()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bloqueo) "Reactivar Iniciar clase" else "Resolver caso") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.profesor(), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                if (item.subtitle().isNotEmpty()) Text(item.subtitle(), style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                if (bloqueo) {
                    Text("El bloqueo no vence solo. La nota queda registrada y se le envía al profesor.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(nota, { nota = it }, Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("Por qué se reactiva y a qué se llegó") })
                } else {
                    item.fechaClase?.let { Text("Clase del $it", style = MaterialTheme.typography.bodySmall) }
                    item.temaIngresado?.let { Text("Tema ingresado: $it", style = MaterialTheme.typography.bodySmall) }
                    item.temaEsperado?.let { Text("Tema esperado: $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Justificación: ${item.justificacionProfesor?.trim().takeUnless { it.isNullOrEmpty() } ?: "sin justificación cargada"}", style = MaterialTheme.typography.bodySmall)
                    Text("Aceptar no cuenta como falta. Rechazar suma una falta.", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                }
            }
        },
        confirmButton = {
            if (bloqueo) {
                TextButton(enabled = nota.isNotBlank(), onClick = { onConfirm(ResolucionDto("PERMITIDO", nota.trim())) }) { Text("Levantar bloqueo") }
            } else {
                TextButton(onClick = { onConfirm(ResolucionDto("PERMITIDO")) }) { Text("Aceptar") }
            }
        },
        dismissButton = {
            Row {
                if (!bloqueo) TextButton(onClick = { onConfirm(ResolucionDto("RECHAZADO")) }) { Text("Rechazar", color = scaColors.danger.fg) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
