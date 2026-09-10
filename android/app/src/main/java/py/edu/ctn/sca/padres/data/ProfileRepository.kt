package py.edu.ctn.sca.padres.data

import kotlinx.serialization.json.Json
import java.io.IOException

sealed interface ProfileLoad {
    data class Ok(val data: ProfileResponseDto) : ProfileLoad
    data class Error(val message: String) : ProfileLoad
}

sealed interface ProfileSave {
    data object Ok : ProfileSave
    data class Error(val message: String) : ProfileSave
}

class ProfileRepository(
    private val profileApi: ProfileApi,
    private val json: Json,
) {

    suspend fun load(): ProfileLoad = try {
        ProfileLoad.Ok(profileApi.get())
    } catch (e: IOException) {
        ProfileLoad.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
    } catch (e: Exception) {
        ProfileLoad.Error("No se pudo cargar tu perfil.")
    }

    suspend fun save(request: SaveProfileRequest): ProfileSave = try {
        val resp = profileApi.save(request)
        if (resp.isSuccessful) {
            ProfileSave.Ok
        } else {
            val serverMessage = resp.errorBody()?.string()?.let {
                runCatching { json.decodeFromString<ApiErrorDto>(it) }.getOrNull()?.message
            }
            ProfileSave.Error(serverMessage ?: "No se pudieron guardar los cambios (error ${resp.code()}).")
        }
    } catch (e: IOException) {
        ProfileSave.Error("Sin conexión. Verificá tu internet e intentá de nuevo.")
    } catch (e: Exception) {
        ProfileSave.Error("No se pudieron guardar los cambios.")
    }
}
