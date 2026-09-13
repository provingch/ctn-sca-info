package py.edu.ctn.sca.padres.data

import java.io.IOException

sealed interface ParentResult {
    data class Ok(val data: ParentResponse) : ParentResult
    data class Error(val message: String) : ParentResult
}

sealed interface ConductaResult {
    data class Ok(val data: List<RasgoConductaDto>) : ConductaResult
    data class Error(val message: String) : ConductaResult
}

class ParentRepository(private val parentApi: ParentApi) {

    suspend fun summary(alumnoId: Int? = null): ParentResult = try {
        ParentResult.Ok(parentApi.summary(alumnoId))
    } catch (e: IOException) {
        ParentResult.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
    } catch (e: Exception) {
        ParentResult.Error("No se pudo cargar el resumen académico.")
    }

    suspend fun conducta(alumnoId: Int): ConductaResult = try {
        ConductaResult.Ok(parentApi.conducta(alumnoId))
    } catch (e: IOException) {
        ConductaResult.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
    } catch (e: Exception) {
        ConductaResult.Error("No se pudieron cargar las notas de conducta.")
    }
}
