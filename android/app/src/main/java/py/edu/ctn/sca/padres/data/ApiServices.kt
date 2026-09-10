package py.edu.ctn.sca.padres.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
}

interface PushApi {
    @POST("api/push/fcm")
    suspend fun register(@Body body: FcmTokenRequest): Response<Unit>

    @POST("api/push/fcm/unregister")
    suspend fun unregister(@Body body: FcmTokenRequest): Response<Unit>
}
