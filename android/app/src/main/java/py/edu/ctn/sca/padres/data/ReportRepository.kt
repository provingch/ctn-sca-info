package py.edu.ctn.sca.padres.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

sealed interface ReportResult {
    /** El PDF quedó guardado en [file] (bajo cacheDir/reports). */
    data class Ok(val file: File) : ReportResult
    data class Error(val message: String) : ReportResult
}

/**
 * Descarga los PDF de la vista de padres a `cacheDir/reports/` para abrirlos con
 * un visor externo vía FileProvider. Es el primer flujo de descarga de archivos
 * de la app.
 */
class ReportRepository(
    private val appContext: Context,
    private val parentApi: ParentApi,
    private val json: Json,
) {

    private val dir: File
        get() = File(appContext.cacheDir, "reports").apply { mkdirs() }

    suspend fun reporteMensual(alumnoId: Int, mes: Int, anio: Int): ReportResult =
        download("reporte-mensual-$alumnoId-$anio-${mes.toString().padStart(2, '0')}.pdf") {
            parentApi.reporteMensual(alumnoId, mes, anio)
        }

    suspend fun libreta(alumnoId: Int): ReportResult =
        download("libreta-$alumnoId.pdf") { parentApi.libreta(alumnoId) }

    private suspend fun download(
        filename: String,
        call: suspend () -> retrofit2.Response<okhttp3.ResponseBody>,
    ): ReportResult = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (!response.isSuccessful) {
                val serverMessage = response.errorBody()?.string()?.let {
                    runCatching { json.decodeFromString<ApiErrorDto>(it) }.getOrNull()?.message
                }
                return@withContext ReportResult.Error(
                    serverMessage ?: "No se pudo generar el PDF (error ${response.code()}).",
                )
            }
            val body = response.body()
                ?: return@withContext ReportResult.Error("El servidor no devolvió el PDF.")
            val out = File(dir, filename)
            body.byteStream().use { input -> out.outputStream().use { input.copyTo(it) } }
            ReportResult.Ok(out)
        } catch (e: IOException) {
            ReportResult.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
        } catch (e: Exception) {
            ReportResult.Error("No se pudo generar el PDF.")
        }
    }
}
