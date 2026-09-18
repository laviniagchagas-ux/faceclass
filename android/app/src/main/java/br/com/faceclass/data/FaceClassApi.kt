package br.com.faceclass.data

import br.com.faceclass.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FaceClassApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun currentUser(): CurrentUser

    @GET("lessons/available")
    suspend fun availableLessons(): List<Lesson>

    @POST("attendance/check-in")
    suspend fun checkIn(@Body body: CheckInRequest): CheckInResponse

    @GET("biometrics/status")
    suspend fun faceStatus(): FaceStatus

    @POST("biometrics/enroll")
    suspend fun enrollFace(@Body body: FaceSignatureRequest): FaceStatus

    @POST("attendance/face-check-in")
    suspend fun faceCheckIn(@Body body: FaceCheckInRequest): CheckInResponse

    @GET("dashboard/me/attendance")
    suspend fun attendanceHistory(): List<Attendance>

    @GET("dashboard/me/lateness")
    suspend fun latenessSummary(): LatenessSummary

    @POST("lessons")
    suspend fun createLesson(@Body body: CreateLessonRequest): Lesson

    @GET("dashboard/classes/{classId}")
    suspend fun classDashboard(@Path("classId") classId: Int): ClassDashboard
}

class SessionStore(context: android.content.Context) {
    private val preferences = context.getSharedPreferences("faceclass_session", android.content.Context.MODE_PRIVATE)

    var token: String?
        get() = preferences.getString("token", null)
        private set(value) = preferences.edit().putString("token", value).apply()

    fun save(token: String) {
        this.token = token
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}

class FaceClassRepository(private val session: SessionStore) {
    private val api: FaceClassApi by lazy {
        val authorizationInterceptor = Interceptor { chain ->
            val token = session.token
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(authorizationInterceptor)
            .addInterceptor(logger)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FaceClassApi::class.java)
    }

    suspend fun login(email: String, password: String): CurrentUser {
        val response = api.login(LoginRequest(email, password))
        session.save(response.access_token)
        return api.currentUser()
    }

    suspend fun currentUser() = api.currentUser()
    suspend fun availableLessons() = api.availableLessons()
    suspend fun attendanceHistory() = api.attendanceHistory()
    suspend fun latenessSummary() = api.latenessSummary()
    suspend fun faceStatus() = api.faceStatus()
    suspend fun enrollFace(signature: List<Float>) = api.enrollFace(FaceSignatureRequest(signature))
    suspend fun faceCheckIn(lessonId: Int, signature: List<Float>) = api.faceCheckIn(
        FaceCheckInRequest(
            id_aula = lessonId,
            assinatura = signature,
            wifi_ssid = "FaceClass-Demo",
        )
    )
    suspend fun checkIn(lessonId: Int) = api.checkIn(
        CheckInRequest(
            id_aula = lessonId,
            face_verification_token = "faceclass-demo-ok",
            wifi_ssid = "FaceClass-Demo",
        )
    )
    suspend fun createLesson(request: CreateLessonRequest) = api.createLesson(request)
    suspend fun classDashboard(classId: Int) = api.classDashboard(classId)
    fun logout() = session.clear()
    fun hasSession() = !session.token.isNullOrBlank()
}
