package py.edu.ctn.sca.padres.data.profesor

import kotlinx.serialization.json.Json
import py.edu.ctn.sca.padres.data.ApiErrorDto
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/** Error con mensaje listo para mostrar. */
class ProfesorException(message: String) : Exception(message)

class ProfesorRepository(private val api: ProfesorApi, private val json: Json) {

    private suspend fun <T> call(fallback: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: IOException) {
        Result.failure(ProfesorException("Sin conexión. Verificá tu internet e intentá de nuevo."))
    } catch (e: HttpException) {
        Result.failure(ProfesorException(mensaje(e.response()) ?: fallback))
    } catch (e: Exception) {
        Result.failure(ProfesorException(fallback))
    }

    private fun mensaje(resp: Response<*>?): String? =
        resp?.errorBody()?.string()?.let { runCatching { json.decodeFromString<ApiErrorDto>(it).message }.getOrNull() }
            ?.takeIf { it.isNotBlank() }

    private suspend fun callUnit(fallback: String, block: suspend () -> Response<Unit>): Result<Unit> = try {
        val r = block()
        if (r.isSuccessful) Result.success(Unit) else Result.failure(ProfesorException(mensaje(r) ?: fallback))
    } catch (e: IOException) {
        Result.failure(ProfesorException("Sin conexión. Verificá tu internet e intentá de nuevo."))
    } catch (e: Exception) {
        Result.failure(ProfesorException(fallback))
    }

    suspend fun horarioHoy() = call("No se pudo cargar tu horario de hoy.") { api.horarioHoy() }
    suspend fun misPlanes() = call("No se pudieron cargar tus planes.") { api.misPlanes() }
    suspend fun alumnosRiesgo() = call("No se pudieron cargar los alumnos en riesgo.") { api.alumnosRiesgo().alumnos }
    suspend fun home(cursoId: Int? = null, etapa: Int? = null) = call("No se pudo cargar la información.") { api.home(cursoId, etapa) }
    suspend fun codigosConducta() = call("No se pudieron cargar los códigos de conducta.") { api.codigosConducta() }

    /** null = el profesor no tiene plan para esa asignación/etapa. */
    suspend fun miPlan(asignacionId: Int, etapa: Int, anio: Int = LocalDate.now().year) =
        call("No se pudo cargar tu plan curricular.") { api.miPlan(asignacionId, etapa.toString(), anio).body() }

    suspend fun crearClase(req: CrearClaseRequest) = callUnit("No se pudo registrar la clase.") { api.crearClase(req) }
    suspend fun misClases() = call("No se pudieron cargar tus clases.") { api.misClases() }
    suspend fun miClase(id: Int) = call("No se pudo cargar la clase.") { api.miClase(id) }
    suspend fun tareas(planillaId: Int) = call("No se pudieron cargar las tareas.") { api.tareas(planillaId) }
    suspend fun planilla(planillaId: Int) = call("No se pudo cargar la planilla.") { api.planilla(planillaId) }

    suspend fun guardarNota(planillaId: Int, alumnoId: Int, tareaId: Int, puntos: Int) =
        callUnit("No se pudo guardar la nota.") {
            api.guardarNotas(planillaId, GuardarNotasRequest(listOf(AlumnoNotas(alumnoId, listOf(ItemNota(tareaId, puntos))))))
        }
}
