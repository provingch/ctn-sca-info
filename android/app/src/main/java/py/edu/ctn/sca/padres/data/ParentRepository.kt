package py.edu.ctn.sca.padres.data

import java.io.IOException

sealed interface ParentResult {
    data class Ok(val data: ParentResponse) : ParentResult
    data class Error(val message: String) : ParentResult
}

class ParentRepository(private val parentApi: ParentApi) {

    suspend fun summary(alumnoId: Int? = null): ParentResult = try {
        ParentResult.Ok(parentApi.summary(alumnoId))
    } catch (e: IOException) {
        ParentResult.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
    } catch (e: Exception) {
        ParentResult.Error("No se pudo cargar el resumen académico.")
    }
}
