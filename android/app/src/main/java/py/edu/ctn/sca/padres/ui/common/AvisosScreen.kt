package py.edu.ctn.sca.padres.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** Contenido de la pestaña Avisos (el header lo pone [RoleScaffold]; ver [CommonTabs]). */
@Composable
fun AvisosScreen(vm: AvisosViewModel, modifier: Modifier = Modifier) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    when {
        ui.loading -> LoadingState(modifier)
        ui.error != null && ui.items.isEmpty() -> ErrorState(ui.error!!, vm::load, modifier)
        else -> Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChipRow {
                ScaChip("Sin leer ${ui.items.count { !it.leida }}", ui.soloSinLeer, { vm.setSoloSinLeer(true) })
                ScaChip("Todos", !ui.soloSinLeer, { vm.setSoloSinLeer(false) })
            }
            val visibles = ui.visibles
            if (visibles.isEmpty()) {
                Box(Modifier.padding(vertical = 48.dp)) {
                    Text(
                        if (ui.soloSinLeer) "No tenés avisos sin leer." else "Todavía no tenés avisos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scaColors.muted,
                    )
                }
            }
            agruparAvisos(visibles).forEach { (seccion, lista) ->
                SectionLabel(seccion.titulo)
                ListCard(lista) { n ->
                    ListRow(
                        title = n.titulo,
                        subtitle = listOf(n.cuerpo, cuandoAviso(n.creadaEn())).filter { it.isNotBlank() }.joinToString(" · "),
                        titleWeight = if (n.leida) FontWeight.Normal else FontWeight.SemiBold,
                        onClick = { vm.marcarLeida(n) },
                        leading = {
                            Box(
                                Modifier.size(8.dp).background(
                                    if (n.leida) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                ),
                            )
                        },
                    )
                }
            }
            Text(
                "Los avisos llegan como notificación al celular. Cada rol recibe solo los suyos.",
                style = MaterialTheme.typography.bodySmall,
                color = scaColors.muted,
            )
        }
    }
}
