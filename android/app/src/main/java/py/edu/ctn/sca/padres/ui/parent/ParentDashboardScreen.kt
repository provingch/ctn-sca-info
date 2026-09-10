package py.edu.ctn.sca.padres.ui.parent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.data.ChildDto
import py.edu.ctn.sca.padres.data.SubjectDto
import py.edu.ctn.sca.padres.data.TaskDto
import py.edu.ctn.sca.padres.ui.graphViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(graph: Graph) {
    val vm: ParentViewModel = graphViewModel { ParentViewModel(it.parentRepository, it.authRepository) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas de mis hijos") },
                actions = {
                    IconButton(onClick = vm::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                },
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
                else -> DashboardContent(ui, vm)
            }
        }
    }
}

@Composable
private fun DashboardContent(ui: ParentUiState, vm: ParentViewModel) {
    val data = ui.data ?: return
    val selectedChild = data.hijos.firstOrNull { it.id == data.selectedAlumnoId }
    val subjects = ui.subjectsForStage
    val selectedSubject = ui.selectedSubject

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (data.hijos.size > 1) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    data.hijos.forEach { child ->
                        ChildCard(
                            child = child,
                            selected = child.id == data.selectedAlumnoId,
                            onClick = { vm.selectChild(child.id) },
                        )
                    }
                }
            }
        }

        if (selectedChild != null) {
            item { OverviewCard(child = selectedChild, ui = ui) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stage.entries.forEach { stage ->
                    FilterChip(
                        selected = ui.stage == stage,
                        onClick = { vm.selectStage(stage) },
                        label = { Text(stage.label) },
                    )
                }
            }
        }

        if (subjects.isEmpty()) {
            item {
                EmptyCard("Sin calificaciones en ${ui.stage.label.lowercase()}. Todavía no hay materias ni tareas publicadas para este alumno.")
            }
        } else {
            item {
                Text(
                    "Materias · ${ui.stage.label.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(subjects, key = { it.planillaId }) { subject ->
                SubjectCard(
                    subject = subject,
                    selected = subject.planillaId == selectedSubject?.planillaId,
                    onClick = { vm.selectSubject(subject.planillaId) },
                )
            }
            selectedSubject?.let { subject ->
                item { SubjectDetailCard(subject) }
            }
        }
    }
}

@Composable
private fun ChildCard(child: ChildDto, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.width(220.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                child.especialidad ?: "—",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${child.apellido}, ${child.nombre}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Promedio general: ${child.promedio}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun OverviewCard(child: ChildDto, ui: ParentUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Resumen académico", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${child.nombre} ${child.apellido}", style = MaterialTheme.typography.titleMedium)
            child.especialidad?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Promedio general", "${child.promedio}%", Modifier.weight(1f))
                Metric(ui.stage.label, "${ui.stagePercent}%", Modifier.weight(1f))
                Metric(
                    "Por revisar",
                    ui.pendingTasks.toString(),
                    Modifier.weight(1f),
                    hint = if (ui.missingTasks > 0) "${ui.missingTasks} sin entregar" else null,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, hint: String? = null) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SubjectCard(subject: SubjectDto, selected: Boolean, onClick: () -> Unit) {
    val pending = subject.tareas.count { it.estado != "CALIFICADA" }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = if (selected) null else CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    subject.materia,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                GradePill(subject.nota)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${subject.porcentaje}%  ·  ${subject.puntos} de ${subject.total} pts",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (subject.porcentaje.coerceIn(0, 100)) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${subject.tareas.size} tareas" + if (pending > 0) " · $pending por revisar" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubjectDetailCard(subject: SubjectDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Detalle de tareas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subject.materia, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (subject.tareas.isEmpty()) {
                Text(
                    "Esta materia todavía no tiene actividades publicadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                subject.tareas.forEachIndexed { index, task ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    TaskRow(index + 1, task)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(number: Int, task: TaskDto) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(task.titulo, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatDate(task.fecha), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        TaskResult(task)
    }
}

@Composable
private fun TaskResult(task: TaskDto) {
    if (task.estado == "CALIFICADA") {
        val pct = if (task.total > 0) Math.round((task.puntos ?: 0) * 100.0 / task.total).toInt() else 0
        Column(horizontalAlignment = Alignment.End) {
            Text("${task.puntos ?: 0} / ${task.total}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("$pct% · Calificada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val label = when (task.estado) {
        "ENTREGADA_PENDIENTE" -> "Entregada · sin calificar"
        "NO_ENTREGADA" -> "No entregada"
        else -> "Pendiente"
    }
    val tone = if (task.estado == "NO_ENTREGADA") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tone)
        Text("de ${task.total} pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GradePill(nota: Int) {
    val bg = when {
        nota >= 4 -> MaterialTheme.colorScheme.secondary
        nota == 3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(color = bg, shape = CircleShape) {
        Text(
            nota.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
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
