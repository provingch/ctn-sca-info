package py.edu.ctn.sca.padres.ui.profesor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import py.edu.ctn.sca.padres.data.profesor.AlumnoDto
import py.edu.ctn.sca.padres.data.profesor.CodigoConductaDto
import py.edu.ctn.sca.padres.ui.common.PrimaryButton
import py.edu.ctn.sca.padres.ui.common.RoleScaffold
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.scaColors

@Composable
fun RegistroScreen(vm: RegistroViewModel, onBack: () -> Unit, onGuardada: () -> Unit) {
    val ui by vm.ui.collectAsState()
    androidx.compose.runtime.LaunchedEffect(ui.guardada) { if (ui.guardada) onGuardada() }
    val b = ui.bloque
    val subtitulo = b?.let {
        listOfNotNull(it.materiaNombre, it.cursoDescripcion, hora(it.horaInicio), it.salaNombre?.takeIf { s -> s.isNotBlank() }).joinToString(" · ")
    }
    val r = ui.resumen
    RoleScaffold(
        title = "Registro de clase",
        subtitle = subtitulo,
        onBack = onBack,
        bottomAction = if (b != null && !b.registrada && ui.alumnos.isNotEmpty()) ({
            Column {
                ui.saveError?.let { Text(it, color = scaColors.danger.fg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp)) }
                BarraResumen(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) { append("${r.presentes} de ${ui.alumnos.size}") }
                        append(" presentes · ${r.conductas} ${if (r.conductas == 1) "conducta" else "conductas"}")
                    },
                ) { PrimaryButton("Guardar clase", { vm.guardar() }, enabled = ui.puedeGuardar, loading = ui.saving) }
            }
        }) else null,
    ) {
        Cargable(ui.loading, ui.error, hasData = b != null, onRetry = vm::load) {
            when {
                b!!.registrada -> py.edu.ctn.sca.padres.ui.common.EmptyState("Esta clase ya está registrada", message = "Podés verla en la pestaña Clases.")
                ui.alumnos.isEmpty() -> py.edu.ctn.sca.padres.ui.common.EmptyState("Este curso no tiene alumnos cargados")
                else -> Formulario(ui, vm)
            }
        }
    }
    if (ui.pedirJustificacion) JustificacionDialog(onCancel = vm::cancelarJustificacion, onEnviar = { vm.guardar(it) })
}

@Composable
private fun Formulario(ui: RegistroUi, vm: RegistroViewModel) {
    var editando by remember { mutableStateOf<AlumnoDto?>(null) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Desplegable("Modalidad", ui.modalidad, "", MODALIDADES, { vm.setModalidad(MODALIDADES[it]) }, Modifier.weight(1f))
            Desplegable(
                "Tipo de clase", ui.instrumentos.firstOrNull { it.id == ui.instrumentoId }?.nombre, "Seleccioná",
                ui.instrumentos.map { it.nombre }, { vm.setInstrumento(ui.instrumentos[it].id) }, Modifier.weight(1f),
            )
        }
        TemaField(ui.tema, ui.temas, vm::setTema)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Asistencia", style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp))
            Text("Tocá al alumno para marcar ausente", style = MaterialTheme.typography.bodySmall, color = scaColors.muted, modifier = Modifier.weight(1f))
            Tag("${ui.resumen.presentes}", scaColors.success)
            Tag("${ui.resumen.ausentes}", scaColors.danger)
        }
        ui.alumnos.forEachIndexed { i, a ->
            FilaAlumno(i + 1, a, a.id in ui.ausentes, ui.codigosPorAlumno[a.id].orEmpty(), { vm.toggleAusente(a.id) }, { editando = a })
        }
        Text(
            "La columna de la derecha agrega un código de conducta sin cambiar la asistencia.",
            style = MaterialTheme.typography.bodySmall, color = scaColors.muted,
        )
    }
    editando?.let { a ->
        CodigosDialog(a, ui.codigos, ui.codigosPorAlumno[a.id].orEmpty(), onClose = { editando = null }) { vm.setCodigos(a.id, it); editando = null }
    }
}

@Composable
private fun TemaField(tema: String, sugeridos: List<String>, onChange: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        Text("Contenido desarrollado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedTextField(
                value = tema, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp),
                placeholder = { Text("Ej.: Modelo entidad-relación") }, maxLines = 3,
                trailingIcon = if (sugeridos.isNotEmpty()) ({
                    IconButton({ abierto = true }) { Icon(Icons.Filled.ArrowDropDown, "Ver sugeridos", tint = scaColors.muted) }
                }) else null,
            )
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                sugeridos.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { abierto = false; onChange(t) }) }
            }
        }
        if (sugeridos.isNotEmpty()) {
            Text("Sugerido por tu plan curricular", style = MaterialTheme.typography.bodySmall, color = scaColors.muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun FilaAlumno(n: Int, a: AlumnoDto, ausente: Boolean, codigos: Set<String>, onToggle: () -> Unit, onCodigos: () -> Unit) {
    val tone = if (ausente) scaColors.danger else scaColors.success
    Surface(
        shape = RoundedCornerShape(6.dp), color = tone.bg.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, tone.fg.copy(alpha = if (ausente) 0.4f else 0.2f)),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Row(Modifier.weight(1f).clickable(onClick = onToggle).heightIn(min = 52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$n. ${a.nombreCompleto}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (ausente) TextDecoration.LineThrough else null, maxLines = 2,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (ausente) "Ausente" else "Presente", color = tone.fg, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
            VerticalDivider(Modifier.fillMaxHeight(), color = tone.fg.copy(alpha = 0.2f))
            Box(
                Modifier.width(64.dp).fillMaxHeight().background(if (codigos.isEmpty()) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else scaColors.warning.bg)
                    .clickable(onClick = onCodigos),
                contentAlignment = Alignment.Center,
            ) {
                if (codigos.isEmpty()) Text("+ N", color = scaColors.muted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                else Text(codigos.sorted().joinToString(" "), color = scaColors.warning.fg, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CodigosDialog(a: AlumnoDto, catalogo: List<CodigoConductaDto>, actuales: Set<String>, onClose: () -> Unit, onListo: (Set<String>) -> Unit) {
    var sel by remember { mutableStateOf(actuales) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(a.nombreCompleto, style = MaterialTheme.typography.titleMedium) },
        text = {
            if (catalogo.isEmpty()) Text("Sin códigos cargados. Los carga Coordinación o Administración.")
            else Column(Modifier.verticalScroll(rememberScrollState())) {
                catalogo.forEach { c ->
                    val on = c.codigo in sel
                    Row(
                        Modifier.fillMaxWidth().clickable { sel = if (on) sel - c.codigo else sel + c.codigo },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(on, { sel = if (on) sel - c.codigo else sel + c.codigo })
                        Text("${c.codigo} · ${c.descripcion}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton({ onListo(sel) }) { Text("Listo") } },
        dismissButton = { TextButton(onClose) { Text("Cancelar") } },
    )
}

@Composable
private fun JustificacionDialog(onCancel: () -> Unit, onEnviar: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Tema atrasado", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text("El tema está atrasado según el plan curricular. Contá el motivo para poder registrar la clase.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(texto, { texto = it }, label = { Text("Justificación") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton({ onEnviar(texto.trim()) }, enabled = texto.isNotBlank()) { Text("Registrar clase") } },
        dismissButton = { TextButton(onCancel) { Text("Cancelar") } },
    )
}
