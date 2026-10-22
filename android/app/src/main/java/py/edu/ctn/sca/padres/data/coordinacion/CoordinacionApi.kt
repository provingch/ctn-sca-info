package py.edu.ctn.sca.padres.data.coordinacion

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface CoordinacionApi {
    @GET("api/admin/quejas")
    suspend fun quejas(): List<QuejaDto>

    @PUT("api/admin/quejas/{id}/aceptacion")
    suspend fun aceptar(@Path("id") id: Long): Response<Unit>

    @PUT("api/admin/quejas/{id}/rechazo")
    suspend fun rechazar(@Path("id") id: Long, @Body body: RechazoBody): Response<Unit>

    /** Catálogo del admin: solo se usa `usuarios` para mostrar quién cargó cada queja. */
    @GET("api/admin")
    suspend fun catalogo(): CatalogoDto

    @GET("api/admin/anotaciones")
    suspend fun anotaciones(@Query("limite") limite: Int): List<AnotacionDto>

    @Streaming
    @GET("api/admin/anotaciones/{id}/foto")
    suspend fun foto(@Path("id") id: Int): ResponseBody
}

@Serializable
data class RechazoBody(val motivoRechazo: String)

@Serializable
data class CatalogoDto(val usuarios: List<UsuarioDto> = emptyList())

@Serializable
data class UsuarioDto(val id: Long = 0, val nombre: String? = null, val apellido: String? = null)

@Serializable
data class QuejaDto(
    val id: Long = 0,
    val profesorId: Long = 0,
    val profesorNombre: String? = null,
    val profesorApellido: String? = null,
    val cursoId: Long = 0,
    val cursoEspecialidad: String? = null,
    val cursoSeccion: String? = null,
    val cursoNivel: Int? = null,
    val tipo: String = "",
    val motivo: String = "",
    val creadaPor: Long = 0,
    /** ISO-8601 o epoch ms según Jackson. */
    val creadaEn: JsonPrimitive? = null,
    val estado: String? = null,
    val aceptadaEn: JsonPrimitive? = null,
    val rechazadaEn: JsonPrimitive? = null,
    val motivoRechazo: String? = null,
    val revisadaEn: JsonPrimitive? = null,
    val resueltaEn: JsonPrimitive? = null,
)

@Serializable
data class AnotacionDto(
    val id: Int = 0,
    val alumnoId: Int = 0,
    val alumnoNombre: String = "",
    val alumnoApellido: String = "",
    val curso: String? = null,
    /** "yyyy-MM-dd HH:mm:ss" hora de Asunción. */
    val fechaHora: String? = null,
    val tipo: String = "",
    val observacion: String? = null,
    val materia: String? = null,
    val profesor: String? = null,
    val tieneFoto: Boolean = false,
)
