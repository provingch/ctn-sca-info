package py.edu.ctn.sca.padres.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/auth/2fa/verify")
    suspend fun verify2fa(@Body body: Verify2faRequest): Response<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(): Response<RefreshResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>
}

interface ParentApi {
    @GET("api/padre")
    suspend fun summary(@Query("alumnoId") alumnoId: Int? = null): ParentResponse

    @Streaming
    @GET("api/padre/alumnos/{alumnoId}/reporte-mensual")
    suspend fun reporteMensual(
        @Path("alumnoId") alumnoId: Int,
        @Query("mes") mes: Int,
        @Query("anio") anio: Int,
    ): Response<ResponseBody>

    @Streaming
    @GET("api/padre/alumnos/{alumnoId}/libreta")
    suspend fun libreta(@Path("alumnoId") alumnoId: Int): Response<ResponseBody>
}

interface ProfileApi {
    @GET("api/profile")
    suspend fun get(): ProfileResponseDto

    @POST("api/profile/save-profile")
    suspend fun save(@Body body: SaveProfileRequest): Response<Unit>

    @POST("api/profile/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>
}

interface PushApi {
    @POST("api/push/fcm")
    suspend fun register(@Body body: FcmTokenRequest): Response<Unit>

    @POST("api/push/fcm/unregister")
    suspend fun unregister(@Body body: FcmTokenRequest): Response<Unit>
}
