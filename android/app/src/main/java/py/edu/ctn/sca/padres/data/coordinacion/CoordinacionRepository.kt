package py.edu.ctn.sca.padres.data.coordinacion

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.serialization.json.Json
import retrofit2.Response
import py.edu.ctn.sca.padres.data.ApiErrorDto

class CoordinacionRepository(private val api: CoordinacionApi) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun quejas(): List<QuejaDto> = api.quejas()

    /** id -> "Nombre Apellido" de quien cargó la queja; vacío si falla (el web también tolera el fallo). */
    suspend fun nombres(): Map<Long, String> = runCatching {
        api.catalogo().usuarios.associate { it.id to "${it.nombre.orEmpty()} ${it.apellido.orEmpty()}".trim() }
    }.getOrDefault(emptyMap())

    /** null = ok, si no el mensaje de error. */
    suspend fun aceptar(id: Long): String? = respuesta { api.aceptar(id) }

    suspend fun rechazar(id: Long, motivo: String): String? = respuesta { api.rechazar(id, RechazoBody(motivo)) }

    suspend fun anotaciones(): List<AnotacionDto> = api.anotaciones(LIMITE_ANOTACIONES)

    /** Foto decodificada y reducida a [maxPx]; null si no existe o falla. */
    suspend fun foto(id: Int, maxPx: Int): Bitmap? = runCatching {
        val bytes = api.foto(id).use { it.bytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    private suspend fun respuesta(call: suspend () -> Response<Unit>): String? = try {
        val r = call()
        if (r.isSuccessful) null
        else r.errorBody()?.string()?.let { runCatching { json.decodeFromString<ApiErrorDto>(it) }.getOrNull()?.message }
            ?: "No se pudo completar la acción (error ${r.code()})."
    } catch (e: java.io.IOException) {
        "Sin conexión. Verificá tu internet e intentá de nuevo."
    } catch (e: Exception) {
        "No se pudo completar la acción."
    }
}
