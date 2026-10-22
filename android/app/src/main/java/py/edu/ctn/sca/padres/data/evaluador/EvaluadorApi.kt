package py.edu.ctn.sca.padres.data.evaluador

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface EvaluadorApi {
    @GET("api/plan-curricular/pendientes")
    suspend fun pendientes(): List<PlanResumenDto>

    @GET("api/plan-curricular/{id}")
    suspend fun plan(@Path("id") id: Int): PlanDetalleDto

    @Streaming
    @GET("api/plan-curricular/{id}/documento")
    suspend fun documento(@Path("id") id: Int): Response<ResponseBody>

    @POST("api/plan-curricular/{id}/aprobar")
    suspend fun aprobar(@Path("id") id: Int): Response<Unit>

    @POST("api/plan-curricular/{id}/rechazar")
    suspend fun rechazar(@Path("id") id: Int, @Query("observaciones") observaciones: String): Response<Unit>

    @GET("api/evaluacion/incumplimientos")
    suspend fun incumplimientos(): List<IncumplimientoDto>

    @POST("api/evaluacion/incumplimientos/{id}/resolver")
    suspend fun resolver(@Path("id") id: Long, @Body body: ResolucionDto): ResultadoResolucionDto
}

@Serializable
data class PlanResumenDto(
    val id: Int = 0,
    val estado: String = "",
    val fechaSubida: String? = null,
    val materiaNombre: String = "",
    val profesorNombre: String = "",
    val profesorNombreCorto: String? = null,
    val cursoDescripcion: String = "",
)

@Serializable
data class TemaDto(
    val mes: String = "",
    val ordenMes: Int = 0,
    val bloque: Int = 0,
    val temasContenidos: String = "",
)

@Serializable
data class PlanDetalleDto(
    val id: Int? = null,
    val estado: String = "",
    val fechaSubida: String? = null,
    val materiaNombre: String = "",
    val profesorNombre: String = "",
    val profesorNombreCorto: String? = null,
    val cursoDescripcion: String = "",
    val temas: List<TemaDto> = emptyList(),
)

@Serializable
data class IncumplimientoDto(
    val id: Long = 0,
    val tipo: String = "",
    val descripcion: String = "",
    val fechaCreacion: String? = null,
    val usuarioId: Int = 0,
    val usuarioNombre: String? = null,
    val usuarioApellido: String? = null,
    val materiaNombre: String? = null,
    val fechaClase: String? = null,
    val temaIngresado: String? = null,
    val temaEsperado: String? = null,
    val justificacionProfesor: String? = null,
)

@Serializable
data class ResolucionDto(val estado: String, val nota: String? = null)

@Serializable
data class ResultadoResolucionDto(val ok: Boolean = false, val bloqueoGenerado: Boolean = false)
