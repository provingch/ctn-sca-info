package py.edu.ctn.sca.padres.data.evaluador

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import py.edu.ctn.sca.padres.data.ApiErrorDto
import java.io.File
import java.io.IOException

class EvaluadorRepository(
    private val appContext: Context,
    private val api: EvaluadorApi,
    private val json: Json,
) {
    suspend fun pendientes() = call("No se pudieron cargar los planes.") { api.pendientes() }
    suspend fun plan(id: Int) = call("No se pudo cargar el plan.") { api.plan(id) }
    suspend fun incumplimientos() = call("No se pudieron cargar los incumplimientos.") { api.incumplimientos() }

    suspend fun aprobar(id: Int) = call("No se pudo aprobar el plan.") {
        val r = api.aprobar(id)
        if (!r.isSuccessful) throw HttpException(r)
    }

    suspend fun rechazar(id: Int, observaciones: String) = call("No se pudo rechazar el plan.") {
        val r = api.rechazar(id, observaciones)
        if (!r.isSuccessful) throw HttpException(r)
    }

    suspend fun resolver(id: Long, resolucion: ResolucionDto) =
        call("No se pudo resolver el caso.") { api.resolver(id, resolucion) }

    /** Descarga el documento original del plan a cacheDir/reports (cubierto por file_paths.xml). */
    suspend fun documento(id: Int): Result<File> = withContext(Dispatchers.IO) {
        call("No se pudo descargar el documento.") {
            val r = api.documento(id)
            if (!r.isSuccessful) throw HttpException(r)
            val body = r.body() ?: throw IOException("vacío")
            val dir = File(appContext.cacheDir, "reports").apply { mkdirs() }
            File(dir, "plan-curricular-$id.xlsx").also { f ->
                body.byteStream().use { i -> f.outputStream().use { i.copyTo(it) } }
            }
        }
    }

    private suspend fun <T> call(fallback: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: HttpException) {
        val msg = e.response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString<ApiErrorDto>(it) }.getOrNull()?.message }
        Result.failure(Exception(msg?.takeIf { it.isNotBlank() } ?: fallback))
    } catch (e: IOException) {
        Result.failure(Exception("Sin conexión. Verificá tu internet e intentá de nuevo."))
    } catch (e: Exception) {
        Result.failure(Exception(fallback))
    }
}
