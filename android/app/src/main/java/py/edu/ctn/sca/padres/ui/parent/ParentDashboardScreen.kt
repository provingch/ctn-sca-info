package py.edu.ctn.sca.padres.ui.parent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.R
import py.edu.ctn.sca.padres.data.ChildDto
import py.edu.ctn.sca.padres.data.ReportRepository
import py.edu.ctn.sca.padres.data.ReportResult
import py.edu.ctn.sca.padres.data.SubjectDto
import py.edu.ctn.sca.padres.data.TaskDto
import py.edu.ctn.sca.padres.ui.components.ContentMaxWidth
import py.edu.ctn.sca.padres.ui.components.Eyebrow
import py.edu.ctn.sca.padres.ui.components.GradeChip
import py.edu.ctn.sca.padres.ui.components.MetricBox
import py.edu.ctn.sca.padres.ui.components.Panel
import py.edu.ctn.sca.padres.ui.components.ProgressTrack
import py.edu.ctn.sca.padres.ui.components.StatusPill
import py.edu.ctn.sca.padres.ui.graphViewModel
import py.edu.ctn.sca.padres.ui.theme.scaColors
import java.io.File
import java.time.LocalDate

private val MESES = arrayOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

/**
 * Parent dashboard, mirroring the web `frontend/src/pages/parent/ParentPage.tsx`:
 * child selector cards, an academic-summary panel with stat boxes and per-stage
 * averages, a pill stage switcher, subject cards with a progress bar and grade
 * chip, and a per-subject task breakdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(graph: Graph, onOpenProfile: () -> Unit = {}) {
    val vm: ParentViewModel = graphViewModel { ParentViewModel(it.parentRepository, it.authRepository) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Notas de mis hijos", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.sca_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .height(28.dp),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Mi perfil")
                    }
                    IconButton(onClick = vm::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                ui.loading -> CenteredMessage { CircularProgressIndicator() }
                ui.error != null && ui.data == null -> CenteredMessage {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ui.error!!, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Deslizá hacia abajo para reintentar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> DashboardContent(ui, vm, graph.reportRepository)
            }
        }
    }
}

@Composable
private fun DashboardContent(ui: ParentUiState, vm: ParentViewModel, reports: ReportRepository) {
    val data = ui.data ?: return
    val selectedChild = data.hijos.firstOrNull { it.id == data.selectedAlumnoId }
    val subjects = ui.subjectsForStage
    val selectedSubject = ui.selectedSubject
    val latestActivity = data.materias.flatMap { it.tareas }.mapNotNull { it.fecha }.maxOrNull()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (data.hijos.isNotEmpty()) {
                item { Eyebrow("Hijos vinculados") }
                items(data.hijos, key = { it.id }) { child ->
                    ChildCard(
                        child = child,
                        selected = child.id == data.selectedAlumnoId,
                        onClick = { vm.selectChild(child.id) },
                    )
                }
            }

            if (selectedChild != null) {
                item { OverviewPanel(child = selectedChild, ui = ui, latestActivity = latestActivity) }
                item {
                    ReportsPanel(
                        alumnoId = selectedChild.id,
                        libretaDisponible = data.libretaDisponible,
                        reports = reports,
                    )
                }
            }

            item { StageSwitcher(ui = ui, onSelect = vm::selectStage, count = subjects.size) }

            if (subjects.isEmpty()) {
                item {
                    Panel {
                        Text(
                            "Sin calificaciones en ${ui.stage.label.lowercase()}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Todavía no hay materias ni tareas publicadas para este alumno en la etapa seleccionada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item {
                    Column {
                        Eyebrow("Materias")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Promedios de ${ui.stage.label.lowercase()}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Seleccioná una materia para ver sus tareas y calificaciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(subjects, key = { it.planillaId }) { subject ->
                    SubjectCard(
                        subject = subject,
                        selected = subject.planillaId == selectedSubject?.planillaId,
                        onClick = { vm.selectSubject(subject.planillaId) },
                    )
                }
                selectedSubject?.let { subject ->
                    item { SubjectDetailPanel(subject) }
                }
                item { CalculationNote() }
            }
        }
    }
}

/** Web `.child-card` / `.nav-card`: accent rule, especialidad eyebrow, average in accent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildCard(child: ChildDto, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scaColors.accentSoft else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Column(Modifier.padding(18.dp)) {
                Eyebrow(child.especialidad ?: "—")
                Spacer(Modifier.height(8.dp))
                Text(
                    "${child.apellido}, ${child.nombre}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Promedio general: ${child.promedio}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scaColors.accentDeep,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Web `.parent-overview` panel. */
@Composable
private fun OverviewPanel(child: ChildDto, ui: ParentUiState, latestActivity: String?) {
    Panel {
        Eyebrow("Resumen académico")
        Spacer(Modifier.height(4.dp))
        Text("${child.nombre} ${child.apellido}", style = MaterialTheme.typography.titleMedium)
        Text(
            "${child.especialidad ?: "—"} · Actividad hasta ${formatDate(latestActivity)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(
                "Promedio general",
                "${child.promedio}%",
                Modifier.weight(1f),
                hint = "Todas las etapas",
            )
            MetricBox(
                ui.stage.label,
                "${ui.stagePercent}%",
                Modifier.weight(1f),
                hint = "${ui.subjectsForStage.size} " + if (ui.subjectsForStage.size == 1) "materia" else "materias",
            )
            MetricBox(
                "Por revisar",
                ui.pendingTasks.toString(),
                Modifier.weight(1f),
                hint = if (ui.missingTasks > 0) "${ui.missingTasks} sin entregar" else "Al día",
                hintTone = if (ui.missingTasks > 0) MaterialTheme.colorScheme.error else null,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            Stage.entries.forEach { stage ->
                StageAveragePill(stage = stage, ui = ui, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Web `.parent-stage-comparison > div`: a bordered pill, "Etapa · NN%". */
@Composable
private fun StageAveragePill(stage: Stage, ui: ParentUiState, modifier: Modifier = Modifier) {
    val items = ui.data?.materias?.filter { Stage.from(it.etapa) == stage } ?: emptyList()
    val points = items.sumOf { it.puntos }
    val total = items.sumOf { it.total }
    val avg = if (total > 0) Math.round(points * 100.0 / total).toInt() else null
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stage.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            if (avg == null) "Sin datos" else "$avg%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Web `.parent-stage-tabs`: a rounded pill container with a filled active segment. */
@Composable
private fun StageSwitcher(ui: ParentUiState, onSelect: (Stage) -> Unit, count: Int) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(scaColors.bgSoft)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Stage.entries.forEach { stage ->
                val active = ui.stage == stage
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(stage) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stage.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$count " + if (count == 1) "materia publicada" else "materias publicadas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Web `.parent-subject-card`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectCard(subject: SubjectDto, selected: Boolean, onClick: () -> Unit) {
    val pending = subject.tareas.count { it.estado != "CALIFICADA" }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (selected) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Materia")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            subject.materia,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    GradeChip(subject.nota)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "${subject.porcentaje}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Promedio de la materia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ProgressTrack(subject.porcentaje / 100f, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${subject.puntos} de ${subject.total} puntos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${subject.tareas.size} tareas" + if (pending > 0) " · $pending por revisar" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Web `.parent-subject-detail` panel. */
@Composable
private fun SubjectDetailPanel(subject: SubjectDto) {
    Panel {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Detalle de tareas")
                Spacer(Modifier.height(4.dp))
                Text(subject.materia, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Stage.from(subject.etapa).label} · ${subject.porcentaje}% de promedio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            GradeChip(subject.nota)
        }
        Spacer(Modifier.height(12.dp))
        if (subject.tareas.isEmpty()) {
            Text(
                "Esta materia todavía no tiene actividades publicadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            subject.tareas.forEachIndexed { index, task ->
                if (index > 0) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                }
                TaskRow(index + 1, task)
            }
        }
    }
}

/** Web `.parent-task-row`. */
@Composable
private fun TaskRow(number: Int, task: TaskDto) {
    val inset = when (task.estado) {
        "NO_ENTREGADA" -> MaterialTheme.colorScheme.error
        "ENTREGADA_PENDIENTE" -> scaColors.warning
        else -> null
    }
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (inset != null) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(inset),
            )
            Spacer(Modifier.width(10.dp))
        }
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scaColors.bgSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(task.titulo, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                formatDate(task.fecha),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        TaskResult(task)
    }
}

@Composable
private fun TaskResult(task: TaskDto) {
    if (task.estado == "CALIFICADA") {
        val pct = if (task.total > 0) Math.round((task.puntos ?: 0) * 100.0 / task.total).toInt() else 0
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${task.puntos ?: 0} / ${task.total}",
                style = MaterialTheme.typography.bodyMedium,
                color = scaColors.success,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "$pct% · Calificada",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val (label, tone) = when (task.estado) {
        "ENTREGADA_PENDIENTE" -> "Entregada · sin calificar" to scaColors.warning
        "NO_ENTREGADA" -> "No entregada" to MaterialTheme.colorScheme.error
        else -> "Pendiente" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.End) {
        StatusPill(label, tone)
        Spacer(Modifier.height(3.dp))
        Text(
            "de ${task.total} puntos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Web `.parent-calculation-note` <details>. */
@Composable
private fun CalculationNote() {
    var open by remember { mutableStateOf(false) }
    Panel {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "¿Cómo se calcula el promedio?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(if (open) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = open) {
            Text(
                "El porcentaje de cada materia se obtiene dividiendo los puntos logrados entre los puntos " +
                    "posibles de las tareas publicadas. El promedio general combina los puntos de todas las " +
                    "materias disponibles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/**
 * Descarga el reporte mensual / la libreta del alumno seleccionado y los abre con
 * un visor de PDF externo. Espeja los botones de la web `ParentPage.tsx`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsPanel(alumnoId: Int, libretaDisponible: Boolean, reports: ReportRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mes by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var menuOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun run(kind: String, block: suspend () -> ReportResult) {
        if (busy != null) return
        busy = kind
        message = null
        scope.launch {
            message = when (val result = block()) {
                is ReportResult.Ok -> openReportPdf(context, result.file)
                is ReportResult.Error -> result.message
            }
            busy = null
        }
    }

    Panel {
        Eyebrow("Reportes")
        Spacer(Modifier.height(10.dp))
        Box {
            OutlinedButton(onClick = { menuOpen = true }, enabled = busy == null) {
                Text("Mes: ${MESES[mes - 1]}")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                MESES.forEachIndexed { index, label ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { mes = index + 1; menuOpen = false })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { run("mensual") { reports.reporteMensual(alumnoId, mes, LocalDate.now().year) } },
            enabled = busy == null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy == "mensual") "Generando…" else "Descargar reporte mensual", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { run("libreta") { reports.libreta(alumnoId) } },
            enabled = busy == null && libretaDisponible,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy == "libreta") "Generando…" else "Descargar libreta", fontWeight = FontWeight.Bold)
        }
        if (!libretaDisponible) {
            Spacer(Modifier.height(6.dp))
            Text(
                "La libreta estará disponible cuando el colegio cierre la Segunda Etapa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** Abre el PDF con un visor externo vía FileProvider. Devuelve un mensaje si falla, o null. */
private fun openReportPdf(context: Context, file: File): String? = try {
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

private val MONTHS_ES = arrayOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
)

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Sin fecha"
    return runCatching {
        val d = LocalDate.parse(value)
        "%02d %s %d".format(d.dayOfMonth, MONTHS_ES[d.monthValue - 1], d.year)
    }.getOrDefault(value)
}
