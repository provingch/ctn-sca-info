package py.edu.ctn.sca.padres.ui.coordinacion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.coordinacion.AnotChip
import py.edu.ctn.sca.padres.data.coordinacion.AnotacionDto
import py.edu.ctn.sca.padres.data.coordinacion.QuejaDto
import py.edu.ctn.sca.padres.data.coordinacion.QuejaEstado
import py.edu.ctn.sca.padres.data.coordinacion.QuejaFiltro
import py.edu.ctn.sca.padres.data.coordinacion.UMBRAL_QUEJAS
import py.edu.ctn.sca.padres.data.coordinacion.alumno
import py.edu.ctn.sca.padres.data.coordinacion.cantidadPorObjetivo
import py.edu.ctn.sca.padres.data.coordinacion.creada
import py.edu.ctn.sca.padres.data.coordinacion.cuandoAnotacion
import py.edu.ctn.sca.padres.data.coordinacion.cuenta
import py.edu.ctn.sca.padres.data.coordinacion.cursoLabel
import py.edu.ctn.sca.padres.data.coordinacion.estadoEfectivo
import py.edu.ctn.sca.padres.data.coordinacion.fecha
import py.edu.ctn.sca.padres.data.coordinacion.fechaCorta
import py.edu.ctn.sca.padres.data.coordinacion.filtrarAnotaciones
import py.edu.ctn.sca.padres.data.coordinacion.filtrarQuejas
import py.edu.ctn.sca.padres.data.coordinacion.objetivo
import py.edu.ctn.sca.padres.data.coordinacion.sobreCurso
import py.edu.ctn.sca.padres.ui.common.ChipRow
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ErrorState
import py.edu.ctn.sca.padres.ui.common.ListCard
import py.edu.ctn.sca.padres.ui.common.ListRow
import py.edu.ctn.sca.padres.ui.common.LoadingState
import py.edu.ctn.sca.padres.ui.common.MetricTile
import py.edu.ctn.sca.padres.ui.common.PrimaryButton
import py.edu.ctn.sca.padres.ui.common.ScaCard
import py.edu.ctn.sca.padres.ui.common.ScaChip
import py.edu.ctn.sca.padres.ui.common.SectionLabel
import py.edu.ctn.sca.padres.ui.common.SecondaryButton
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.Tone
import py.edu.ctn.sca.padres.ui.theme.scaColors

private val Pad = 16.dp

// ------------------------------------------------------------------ Inicio

@Composable
fun InicioScreen(vm: CoordinacionViewModel, onQuejas: () -> Unit, onAnotaciones: () -> Unit, onAnotacion: (Int) -> Unit) {
    val q by vm.quejas.collectAsState()
    val a by vm.anotaciones.collectAsState()
    if (q.loading && a.loading) return LoadingState()
    if (q.error != null && a.error != null && q.items.isEmpty() && a.items.isEmpty()) {
        return ErrorState(q.error!!, { vm.cargarQuejas(); vm.cargarAnotaciones() })
    }
    val pendientes = q.items.filter { it.estadoEfectivo() == QuejaEstado.PENDIENTE }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Pad), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(if (q.error != null) "–" else q.pendientes.toString(), "Quejas pendientes", Modifier.weight(1f),
                valueColor = if (q.pendientes > 0) scaColors.danger.fg else null)
            MetricTile(if (a.error != null) "–" else a.items.size.toString(), "Anotaciones (7 días)", Modifier.weight(1f))
        }
        SectionLabel("Últimas quejas pendientes")
        when {
            q.error != null && q.items.isEmpty() -> ErrorState(q.error!!, vm::cargarQuejas, Modifier.height(160.dp))
            pendientes.isEmpty() -> ScaCard { Text("No hay quejas pendientes.", color = scaColors.muted, style = MaterialTheme.typography.bodyMedium) }
            else -> ListCard(pendientes.take(3)) { it ->
                ListRow(it.objetivo(), subtitle = "${it.motivo} · ${fechaCorta(it.creada())}", onClick = onQuejas)
            }
        }
        SectionLabel("Últimas anotaciones")
        when {
            a.error != null && a.items.isEmpty() -> ErrorState(a.error!!, vm::cargarAnotaciones, Modifier.height(160.dp))
            a.items.isEmpty() -> ScaCard { Text("No hay anotaciones en los últimos 7 días.", color = scaColors.muted, style = MaterialTheme.typography.bodyMedium) }
            else -> ListCard(a.items.take(3)) { it ->
                ListRow(it.alumno(), subtitle = "${it.curso.orEmpty()} · ${it.tipo} · ${cuandoAnotacion(it.fecha())}", onClick = { onAnotacion(it.id) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Ver quejas", onQuejas, Modifier.weight(1f))
            SecondaryButton("Ver anotaciones", onAnotaciones, Modifier.weight(1f))
        }
    }
}

// ------------------------------------------------------------------ Quejas

private sealed interface Accion {
    val queja: QuejaDto
    data class Aceptar(override val queja: QuejaDto) : Accion
    data class Rechazar(override val queja: QuejaDto) : Accion
}

@Composable
fun QuejasScreen(vm: CoordinacionViewModel) {
    val s by vm.quejas.collectAsState()
    var accion by remember { mutableStateOf<Accion?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var motivo by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun cerrar() { accion = null; motivo = ""; error = null }

    Column(Modifier.fillMaxSize()) {
        ChipRow(Modifier.padding(horizontal = Pad, vertical = 12.dp)) {
            ScaChip("Pendientes ${s.pendientes}", s.filtro == QuejaFiltro.PENDIENTES, { vm.setFiltro(QuejaFiltro.PENDIENTES) })
            ScaChip("Contra profesores", s.filtro == QuejaFiltro.CONTRA_PROFESORES, { vm.setFiltro(QuejaFiltro.CONTRA_PROFESORES) })
            ScaChip("Sobre cursos", s.filtro == QuejaFiltro.SOBRE_CURSOS, { vm.setFiltro(QuejaFiltro.SOBRE_CURSOS) })
        }
        val visibles = filtrarQuejas(s.items, s.filtro)
        Box(Modifier.weight(1f)) {
            when {
                s.loading -> LoadingState()
                s.error != null && s.items.isEmpty() -> ErrorState(s.error!!, vm::cargarQuejas)
                visibles.isEmpty() -> EmptyState(
                    if (s.filtro == QuejaFiltro.PENDIENTES) "No hay quejas pendientes" else "No hay quejas",
                    message = "Cuando haya quejas nuevas van a aparecer acá.",
                )
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = Pad)) {
                    item {
                        if (s.error != null) Text(s.error!!, color = scaColors.danger.fg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    item {
                        ListCard(visibles) { q ->
                            QuejaCard(q, s, onAceptar = { accion = Accion.Aceptar(q) }, onRechazar = { accion = Accion.Rechazar(q) })
                        }
                        Spacer(Modifier.height(Pad))
                    }
                }
            }
        }
    }

    when (val a = accion) {
        is Accion.Aceptar -> AlertDialog(
            onDismissRequest = { if (!ocupado) cerrar() },
            title = { Text("Aceptar queja") },
            text = {
                Column {
                    Text("Se acepta la queja contra ${a.queja.objetivo()} y se avisa al profesor con una advertencia general.")
                    error?.let { Text(it, color = scaColors.danger.fg, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                Button(enabled = !ocupado, shape = RoundedCornerShape(6.dp), onClick = {
                    ocupado = true; error = null
                    scope.launch {
                        val e = vm.aceptar(a.queja.id)
                        ocupado = false
                        if (e == null) cerrar() else error = e
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(enabled = !ocupado, onClick = ::cerrar) { Text("Cancelar") } },
        )
        is Accion.Rechazar -> AlertDialog(
            onDismissRequest = { if (!ocupado) cerrar() },
            title = { Text("Rechazar queja") },
            text = {
                Column {
                    OutlinedTextField(
                        value = motivo, onValueChange = { if (it.length <= 5000) motivo = it },
                        label = { Text("Motivo del rechazo") }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                    )
                    error?.let { Text(it, color = scaColors.danger.fg, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                Button(enabled = !ocupado && motivo.isNotBlank(), shape = RoundedCornerShape(6.dp), onClick = {
                    ocupado = true; error = null
                    scope.launch {
                        val e = vm.rechazar(a.queja.id, motivo.trim())
                        ocupado = false
                        if (e == null) cerrar() else error = e
                    }
                }) { Text("Rechazar") }
            },
            dismissButton = { TextButton(enabled = !ocupado, onClick = ::cerrar) { Text("Cancelar") } },
        )
        null -> Unit
    }
}

@Composable
private fun QuejaCard(q: QuejaDto, s: QuejasState, onAceptar: () -> Unit, onRechazar: () -> Unit) {
    val estado = q.estadoEfectivo()
    val cantidad = cantidadPorObjetivo(s.items, q)
    val tone = when {
        cantidad > UMBRAL_QUEJAS -> scaColors.danger
        cantidad >= 2 -> scaColors.warning
        else -> scaColors.info
    }
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q.objetivo(), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (estado != QuejaEstado.PENDIENTE) Tag(estado.label, estadoTone(estado))
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(tone.bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(cantidad.toString(), color = tone.fg, fontWeight = FontWeight.SemiBold)
        }
        Text(if (q.sobreCurso) "Queja sobre el curso" else q.cursoLabel(), color = scaColors.muted, style = MaterialTheme.typography.bodyMedium)
        Text(q.motivo, style = MaterialTheme.typography.bodyMedium)
        Text("${s.creador(q.creadaPor)} · ${fechaCorta(q.creada())}", color = scaColors.muted, style = MaterialTheme.typography.bodySmall)
        if (estado == QuejaEstado.RECHAZADA) {
            Text("Motivo del rechazo: ${q.motivoRechazo?.ifBlank { null } ?: "sin motivo registrado"}", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
        }
        if (estado == QuejaEstado.PENDIENTE) {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Rechazar", onRechazar)
                PrimaryButton("Aceptar", onAceptar)
            }
        }
    }
}

@Composable
private fun estadoTone(e: QuejaEstado): Tone = when (e) {
    QuejaEstado.RECHAZADA -> scaColors.danger
    QuejaEstado.PENDIENTE -> scaColors.warning
    QuejaEstado.ACEPTADA -> scaColors.info
    else -> scaColors.success
}

// ------------------------------------------------------------------ Anotaciones

@Composable
fun AnotacionesScreen(vm: CoordinacionViewModel, onOpen: (Int) -> Unit) {
    val s by vm.anotaciones.collectAsState()
    val visibles = filtrarAnotaciones(s.items, s.texto, s.chip)
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = s.texto, onValueChange = vm::setTexto, singleLine = true,
            placeholder = { Text("Alumno, curso o profesor") },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().padding(start = Pad, end = Pad, top = 12.dp),
        )
        ChipRow(Modifier.padding(horizontal = Pad, vertical = 12.dp)) {
            AnotChip.entries.forEach { c ->
                val n = if (c == AnotChip.CON_FOTO) "" else " ${c.cuenta(s.items)}"
                ScaChip(c.label + n, s.chip == c, { vm.setChip(c) })
            }
        }
        Box(Modifier.weight(1f)) {
            when {
                s.loading -> LoadingState()
                s.error != null && s.items.isEmpty() -> ErrorState(s.error!!, vm::cargarAnotaciones)
                s.items.isEmpty() -> EmptyState("Sin anotaciones", message = "No hay anotaciones en los últimos 7 días.")
                visibles.isEmpty() -> EmptyState("Sin resultados", message = "Ninguna anotación coincide con la búsqueda.")
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = Pad)) {
                    item {
                        ListCard(visibles) { a ->
                            AnotRow(vm, a) { onOpen(a.id) }
                        }
                        Spacer(Modifier.height(Pad))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnotRow(vm: CoordinacionViewModel, a: AnotacionDto, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FotoBox(vm, a, 56.dp, 160)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.alumno(), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(buildAnnotatedString {
                append(a.curso.orEmpty().ifEmpty { "Sin curso" } + " · ")
                withStyle(SpanStyle(color = tipoColor(a.tipo), fontWeight = FontWeight.SemiBold)) { append(a.tipo) }
            }, style = MaterialTheme.typography.bodySmall, color = scaColors.muted, maxLines = 1)
            Text(
                listOfNotNull(a.observacion?.takeIf { it.isNotBlank() }, cuandoAnotacion(a.fecha()).takeIf { it.isNotEmpty() }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = scaColors.muted, maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun tipoColor(tipo: String): Color = when (tipo.trim().lowercase()) {
    "uniforme" -> scaColors.danger.fg
    "conducta" -> scaColors.warning.fg
    else -> MaterialTheme.colorScheme.onSurface
}

/** Miniatura/foto: "sin foto" si no tiene; carga autenticada al aparecer. */
@Composable
private fun FotoBox(vm: CoordinacionViewModel, a: AnotacionDto, size: Dp, maxPx: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val base = modifier.size(size).clip(shape).background(scaColors.bgSoft)
    if (!a.tieneFoto) {
        Box(base, Alignment.Center) { Text("sin foto", style = MaterialTheme.typography.labelSmall, color = scaColors.muted) }
        return
    }
    val estado by produceState<Any?>(null, a.id) { value = vm.foto(a.id, maxPx) ?: "fallo" }
    val img = estado as? ImageBitmap
    Box(base, Alignment.Center) {
        if (img != null) Image(img, "Foto de la anotación", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(if (estado == null) "[foto]" else "sin foto", style = MaterialTheme.typography.labelSmall, color = scaColors.muted)
    }
}

// ------------------------------------------------------------------ Detalle de anotación

@Composable
fun AnotacionDetalleScreen(vm: CoordinacionViewModel, id: Int) {
    val s by vm.anotaciones.collectAsState()
    val a = s.items.firstOrNull { it.id == id }
    if (a == null) return if (s.loading) LoadingState() else EmptyState("No se encontró la anotación")
    val foto by produceState<Any?>(null, a.id) { value = if (a.tieneFoto) (vm.foto(a.id, 1400) ?: "fallo") else "sin" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Pad), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val img = foto as? ImageBitmap
        Box(Modifier.fillMaxWidth().height(if (img != null) 320.dp else 120.dp).clip(RoundedCornerShape(6.dp)).background(scaColors.bgSoft), Alignment.Center) {
            if (img != null) Image(img, "Foto de la anotación", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Text(
                when (foto) { null -> "Cargando foto…"; "fallo" -> "Foto no disponible"; else -> "sin foto" },
                color = scaColors.muted, style = MaterialTheme.typography.bodyMedium,
            )
        }
        ScaCard {
            Campo("Alumno", a.alumno())
            Campo("Curso", a.curso)
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tipo", style = MaterialTheme.typography.bodySmall, color = scaColors.muted, modifier = Modifier.width(96.dp))
                Text(a.tipo, color = tipoColor(a.tipo), fontWeight = FontWeight.SemiBold)
            }
            Campo("Descripción", a.observacion)
            Campo("Materia", a.materia)
            Campo("Profesor", a.profesor)
            Campo("Fecha", a.fecha()?.let { fechaCorta(it) } ?: a.fechaHora)
        }
    }
}

@Composable
private fun Campo(label: String, valor: String?) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = scaColors.muted, modifier = Modifier.width(96.dp))
        Text(valor?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
