package py.edu.ctn.sca.padres.data.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

interface NotificacionesApi {
    @GET("api/notificaciones")
    suspend fun listar(@Query("soloNoLeidas") soloNoLeidas: Boolean = false): List<NotificacionDto>

    @GET("api/notificaciones/contador")
    suspend fun contador(): ContadorDto

    @POST("api/notificaciones/{id}/leer")
    suspend fun leer(@Path("id") id: Long): Response<Unit>

    @POST("api/notificaciones/leer-todas")
    suspend fun leerTodas(): Response<Unit>
}

@Serializable
data class ContadorDto(val count: Int = 0)

@Serializable
data class NotificacionDto(
    val id: Long = 0,
    val tipo: String? = null,
    val titulo: String = "",
    val cuerpo: String = "",
    val entidadTipo: String? = null,
    val entidadId: Long? = null,
    val leida: Boolean = false,
    /** Puede llegar como ISO-8601 o epoch ms según la configuración de Jackson. */
    val createdAt: JsonPrimitive? = null,
) {
    fun creadaEn(): LocalDateTime? {
        val raw = createdAt?.content?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        val zone = ZoneId.systemDefault()
        return runCatching {
            raw.toLongOrNull()?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
                ?: OffsetDateTime.parse(raw).atZoneSameInstant(zone).toLocalDateTime()
        }.recoverCatching { LocalDateTime.parse(raw) }.getOrNull()
    }
}

class NotificacionesRepository(private val api: NotificacionesApi) {
    suspend fun listar(): List<NotificacionDto> = api.listar()
    suspend fun contador(): Int = api.contador().count
    suspend fun leer(id: Long): Boolean = runCatching { api.leer(id).isSuccessful }.getOrDefault(false)
    suspend fun leerTodas(): Boolean = runCatching { api.leerTodas().isSuccessful }.getOrDefault(false)
}
