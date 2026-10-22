package py.edu.ctn.sca.padres.ui.padre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import py.edu.ctn.sca.padres.data.padre.AnotacionDto
import py.edu.ctn.sca.padres.ui.common.ChipRow
import py.edu.ctn.sca.padres.ui.common.EmptyState
import py.edu.ctn.sca.padres.ui.common.ErrorState
import py.edu.ctn.sca.padres.ui.common.ListCard
import py.edu.ctn.sca.padres.ui.common.LoadingState
import py.edu.ctn.sca.padres.ui.common.ScaChip
import py.edu.ctn.sca.padres.ui.common.SectionLabel
import py.edu.ctn.sca.padres.ui.common.Tag
import py.edu.ctn.sca.padres.ui.theme.Tone
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** "Uniforme" danger, "Conducta" warning, otros tipos con el tono info del rol. */
@Composable
fun toneTipo(tipo: String): Tone = when (tipo.trim().lowercase()) {
    "uniforme" -> scaColors.danger
    "conducta" -> scaColors.warning
    else -> scaColors.info
}

/** Como `ListRow` pero con el Tag arriba del título (m-45) y miniatura opcional. */
@Composable
private fun PadreRow(
    title: String,
    subtitle: String,
    tag: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    titleWeight: FontWeight = FontWeight.Medium,
) {
    Row(
        modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            tag()
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = titleWeight)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Fila de la lista de conducta: código N o anotación. [onOpen] solo se usa en anotaciones (los códigos no tienen detalle). */
@Composable
fun ConductaRow(item: ConductaItem, vm: PadreViewModel, alumnoId: Int, onOpen: (Int) -> Unit) {
    when (item) {
        is ConductaItem.Anotacion -> {
            val a = item.dto
            PadreRow(
                title = a.observacion.orEmpty().ifBlank { a.tipo },
                subtitle = cuandoConducta(item) + " · " + origenAnotacion(a),
                onClick = { onOpen(a.id) },
                leading = { FotoThumb(vm, alumnoId, a) },
                titleWeight = FontWeight.SemiBold,
                tag = { Tag(a.tipo, toneTipo(a.tipo)) },
            )
        }
        is ConductaItem.Codigo -> {
            val c = item.dto
            PadreRow(
                title = c.descripcion?.takeIf { it.isNotBlank() } ?: c.observacion.orEmpty().ifBlank { c.codigo },
                subtitle = listOfNotNull(cuandoConducta(item), c.materia, c.profesorNombre).filter { it.isNotBlank() }.joinToString(" · "),
                titleWeight = FontWeight.SemiBold,
                tag = { Tag(c.codigo, scaColors.danger) },
            )
        }
    }
}

/** m-45 */
@Composable
fun ConductaContent(ui: PadreUiState, vm: PadreViewModel, onOpen: (Int) -> Unit) {
    when {
        ui.conductaLoading -> LoadingState(Modifier.padding(vertical = 48.dp))
        ui.conductaError != null -> ErrorState(ui.conductaError, onRetry = vm::retryConducta)
        else -> {
            val items = ui.items
            if (items.isEmpty()) {
                EmptyState("Sin novedades de conducta", message = "Acá vas a ver los códigos y las anotaciones de ${ui.hijo?.nombre.orEmpty()}.")
                return
            }
            val conteo = contarConducta(items)
            ChipRow {
                ScaChip("Todo ${conteo.todo}", ui.conductaFiltro == ConductaFiltro.TODO, { vm.setConductaFiltro(ConductaFiltro.TODO) })
                ScaChip("Códigos N ${conteo.codigos}", ui.conductaFiltro == ConductaFiltro.CODIGOS, { vm.setConductaFiltro(ConductaFiltro.CODIGOS) })
                ScaChip("Anotaciones ${conteo.anotaciones}", ui.conductaFiltro == ConductaFiltro.ANOTACIONES, { vm.setConductaFiltro(ConductaFiltro.ANOTACIONES) })
            }
            val visibles = filtrarConductaItems(items, ui.conductaFiltro)
            if (visibles.isEmpty()) {
                Text("No hay nada en esta categoría.", color = scaColors.muted, style = MaterialTheme.typography.bodyMedium)
            }
            seccionarConducta(visibles).forEach { (seccion, lista) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(seccion.titulo)
                    ListCard(lista) { ConductaRow(it, vm, ui.selectedId ?: 0, onOpen) }
                }
            }
            Text(
                "Los códigos N los pone el profesor en clase. Las anotaciones las carga Preceptoría, a veces con foto.",
                style = MaterialTheme.typography.bodySmall, color = scaColors.muted,
            )
        }
    }
}

/** m-46: detalle de una anotación + "OTRAS DE <nombre>". */
@Composable
fun AnotacionDetalle(ui: PadreUiState, vm: PadreViewModel, id: Int, onOpen: (Int) -> Unit) {
    val a: AnotacionDto? = ui.anotaciones.firstOrNull { it.id == id }
    when {
        ui.conductaLoading || (ui.loading && a == null) -> LoadingState()
        a == null -> EmptyState("No se encontró la anotación")
        else -> {
            val alumnoId = ui.selectedId ?: a.alumnoId
            val otras = ui.items.filter { it !is ConductaItem.Anotacion || it.dto.id != id }.take(5)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(a.tipo, toneTipo(a.tipo))
                    Spacer(Modifier.width(8.dp))
                    Text(fechaCompleta(a), style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                }
                Text(a.observacion.orEmpty().ifBlank { a.tipo }, style = MaterialTheme.typography.titleLarge)
                FotoGrande(vm, alumnoId, a)
                Text("La foto la ve solo la familia del alumno y Coordinación.", style = MaterialTheme.typography.bodySmall, color = scaColors.muted)
                if (otras.isNotEmpty()) {
                    SectionLabel("Otras de ${ui.hijo?.nombre.orEmpty()}")
                    ListCard(otras) { ConductaRow(it, vm, alumnoId, onOpen) }
                }
            }
        }
    }
}
