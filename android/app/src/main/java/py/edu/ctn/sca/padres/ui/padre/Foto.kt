package py.edu.ctn.sca.padres.ui.padre

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import py.edu.ctn.sca.padres.data.padre.AnotacionDto
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** null = cargando; [Cargada.bitmap] null = no hay / falló. */
private class Cargada(val bitmap: ImageBitmap?)

@Composable
private fun rememberFoto(vm: PadreViewModel, alumnoId: Int, a: AnotacionDto): Cargada? {
    val s by produceState<Cargada?>(if (a.tieneFoto) null else Cargada(null), alumnoId, a.id) {
        if (a.tieneFoto) value = Cargada(vm.foto(alumnoId, a.id))
    }
    return s
}

/** Miniatura 64x56 de la lista; "sin foto" si la anotación no tiene. */
@Composable
fun FotoThumb(vm: PadreViewModel, alumnoId: Int, a: AnotacionDto) {
    val foto = rememberFoto(vm, alumnoId, a)
    Box(
        Modifier.size(width = 64.dp, height = 56.dp).clip(RoundedCornerShape(4.dp)).background(scaColors.bgSoft),
        Alignment.Center,
    ) {
        when {
            foto == null -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            foto.bitmap != null -> Image(foto.bitmap, null, Modifier.size(width = 64.dp, height = 56.dp), contentScale = ContentScale.Crop)
            else -> Text(if (a.tieneFoto) "foto" else "sin foto", style = MaterialTheme.typography.labelSmall, color = scaColors.muted)
        }
    }
}

/** Foto grande del detalle, con placeholder si no hay o no se pudo cargar. */
@Composable
fun FotoGrande(vm: PadreViewModel, alumnoId: Int, a: AnotacionDto) {
    val foto = rememberFoto(vm, alumnoId, a)
    val bitmap = foto?.bitmap
    if (bitmap != null) {
        Image(
            bitmap, "Foto de evidencia",
            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.FillWidth,
        )
    } else {
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(6.dp)).background(scaColors.bgSoft),
            Alignment.Center,
        ) {
            if (foto == null) CircularProgressIndicator()
            else Text(
                if (a.tieneFoto) "No se pudo cargar la foto" else "Esta anotación no tiene foto",
                style = MaterialTheme.typography.bodyMedium, color = scaColors.muted,
            )
        }
    }
}
