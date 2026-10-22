package py.edu.ctn.sca.padres.data.padre

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.io.IOException

/** Endpoints de padres que no están en `ParentApi` (AnotacionWebController + `visto`). */
interface PadreApi {
    @GET("api/padre/alumnos/{alumnoId}/anotaciones")
    suspend fun anotaciones(@Path("alumnoId") alumnoId: Int): List<AnotacionDto>

    @Streaming
    @GET("api/padre/alumnos/{alumnoId}/anotaciones/{anotacionId}/foto")
    suspend fun foto(@Path("alumnoId") alumnoId: Int, @Path("anotacionId") anotacionId: Int): Response<ResponseBody>

    @POST("api/padre/alumnos/{alumnoId}/visto")
    suspend fun visto(@Path("alumnoId") alumnoId: Int): Response<Unit>
}

/** Espejo de `AnotacionWebController.AnotacionDto`; `fechaHora` = `yyyy-MM-dd HH:mm:ss`. */
@Serializable
data class AnotacionDto(
    val id: Int = 0,
    val alumnoId: Int = 0,
    val alumnoNombre: String? = null,
    val alumnoApellido: String? = null,
    /** "Informática · 2° B" */
    val curso: String? = null,
    val fechaHora: String? = null,
    val tipo: String = "",
    val observacion: String? = null,
    val materia: String? = null,
    val profesor: String? = null,
    val tieneFoto: Boolean = false,
)

sealed interface PadreResult<out T> {
    data class Ok<T>(val data: T) : PadreResult<T>
    data class Error(val message: String) : PadreResult<Nothing>
}

class PadreRepository(private val api: PadreApi) {
    private val fotos = LruCache<Int, ImageBitmap>(24)

    suspend fun anotaciones(alumnoId: Int): PadreResult<List<AnotacionDto>> = try {
        PadreResult.Ok(api.anotaciones(alumnoId))
    } catch (e: IOException) {
        PadreResult.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
    } catch (e: Exception) {
        PadreResult.Error("No se pudieron cargar las anotaciones.")
    }

    /** Fire-and-forget como la web: cierra las novedades pendientes del hijo. Los errores se ignoran. */
    suspend fun visto(alumnoId: Int) {
        runCatching { api.visto(alumnoId) }
    }

    /** Foto de evidencia decodificada (máx. ~1600px de lado), cacheada; null si no hay o falla. */
    suspend fun foto(alumnoId: Int, anotacionId: Int): ImageBitmap? {
        fotos.get(anotacionId)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val bytes = api.foto(alumnoId, anotacionId).takeIf { it.isSuccessful }?.body()?.bytes()
                    ?: return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
                    ?.also { fotos.put(anotacionId, it) }
            }.getOrNull()
        }
    }
}
