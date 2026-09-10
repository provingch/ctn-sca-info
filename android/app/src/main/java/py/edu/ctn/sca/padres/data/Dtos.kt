package py.edu.ctn.sca.padres.data

import kotlinx.serialization.Serializable

// ---- Auth (mirror of ctn.informatica.sca.dto.*) ----

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val rememberMe: Boolean = true,
)

@Serializable
data class Verify2faRequest(
    val tempToken: String,
    val code: String,
    val rememberMe: Boolean = true,
)

@Serializable
data class LoginResponse(
    val requiere2fa: Boolean = false,
    val tempToken: String? = null,
    val accessToken: String? = null,
    val level: Int? = null,
)

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val level: Int? = null,
)

@Serializable
data class AuthErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val retryAfterSeconds: Long? = null,
)

// ---- Parent summary (mirror of ParentController.ParentResponse) ----

@Serializable
data class ParentResponse(
    val hijos: List<ChildDto> = emptyList(),
    val selectedAlumnoId: Int? = null,
    val materias: List<SubjectDto> = emptyList(),
)

@Serializable
data class ChildDto(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val especialidad: String? = null,
    val promedio: Int = 0,
)

@Serializable
data class SubjectDto(
    val planillaId: Int,
    val materiaId: Int,
    val materia: String,
    val etapa: String,
    val puntos: Int = 0,
    val total: Int = 0,
    val porcentaje: Int = 0,
    val nota: Int = 0,
    val tareas: List<TaskDto> = emptyList(),
)

@Serializable
data class TaskDto(
    val id: Int,
    val titulo: String,
    val fecha: String? = null,
    val puntos: Int? = null,
    val total: Int = 0,
    val estado: String,
)

// ---- Push registration (backend endpoint added on this branch) ----

@Serializable
data class FcmTokenRequest(
    val token: String,
    val platform: String = "android",
)
