package py.edu.ctn.sca.padres.data.profesor

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfesorApi {
    @GET("api/home/mi-horario/hoy")
    suspend fun horarioHoy(): List<BloqueHoyDto>

    @GET("api/plan-curricular/mios")
    suspend fun misPlanes(): List<PlanHistorialDto>

    @GET("api/home/alumnos-riesgo")
    suspend fun alumnosRiesgo(): AlumnosRiesgoDto

    @GET("api/home")
    suspend fun home(@Query("cursoId") cursoId: Int? = null, @Query("etapa") etapa: Int? = null): HomeDto

    @GET("api/codigos-conducta")
    suspend fun codigosConducta(): List<CodigoConductaDto>

    /** 204 (sin plan) llega como body null. */
    @GET("api/plan-curricular/mi-plan")
    suspend fun miPlan(
        @Query("asignacionId") asignacionId: Int,
        @Query("etapa") etapa: String,
        @Query("anio") anio: Int,
    ): Response<PlanEstadoDto>

    @POST("api/home/create-rasgo-planilla")
    suspend fun crearClase(@Body body: CrearClaseRequest): Response<Unit>

    @GET("api/home/mis-clases")
    suspend fun misClases(): List<ClaseDadaDto>

    @GET("api/home/mis-clases/{id}")
    suspend fun miClase(@Path("id") id: Int): ClaseDetalleDto

    @GET("api/instrumentos")
    suspend fun instrumentos(): List<InstrumentoDto>

    @POST("api/planillas/{id}/tareas")
    suspend fun crearTarea(@Path("id") planillaId: Int, @Body body: CrearTareaRequest): Response<Unit>

    @GET("api/planillas/{id}/tareas")
    suspend fun tareas(@Path("id") planillaId: Int): List<TareaDto>

    @GET("api/planillas/{id}")
    suspend fun planilla(@Path("id") planillaId: Int): PlanillaDetalleDto

    @POST("api/planillas/{id}/notas")
    suspend fun guardarNotas(@Path("id") planillaId: Int, @Body body: GuardarNotasRequest): Response<Unit>
}
