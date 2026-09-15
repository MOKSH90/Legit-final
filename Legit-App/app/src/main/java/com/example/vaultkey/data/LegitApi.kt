package com.example.vaultkey.data

import android.util.Log
import com.example.vaultkey.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.json.Json
import java.io.IOException

class LegitApi(
    private val sessionStore: SessionStore,
    private val baseUrl: String = EnvironmentConfig.baseUrl
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("LegitApi", message)
                }
            }
            level = if (BuildConfig.ENABLE_HTTP_LOGS) {
                LogLevel.BODY
            } else {
                LogLevel.NONE
            }
        }
    }

    suspend fun register(request: RegisterRequestDto): AuthResponseDto =
        post("/api/v1/auth/register", request)

    suspend fun login(request: LoginRequestDto): AuthResponseDto =
        post("/api/v1/auth/login", request)

    suspend fun getProfile(): UserProfileDto =
        get("/api/v1/user/me")

    suspend fun updateProfile(request: UpdateProfileRequestDto): UserProfileDto =
        put("/api/v1/user/me", request)

    suspend fun getDocuments(): DocumentListResponseDto =
        get("/api/v1/documents")

    suspend fun getDocument(documentId: String): DocumentResponseDto =
        get("/api/v1/documents/$documentId")

    suspend fun uploadDocument(request: DocumentUploadRequestDto): DocumentResponseDto {
        val envelope = postEnvelope<DocumentUploadRequestDto, DocumentResponseDto>("/api/v1/documents", request)
        return envelope.data ?: throw LegitApiException(envelope.message)
    }

    suspend fun getPendingContracts(): ContractListResponseDto =
        get("/api/v1/pipeline/user/contracts/pending")

    suspend fun registerFcmToken(fcmToken: String): ApiEnvelope<Unit> =
        postEnvelope("/api/v1/user/fcm-token", RegisterPushTokenRequestDto(fcmToken = fcmToken))

    suspend fun getContract(contractId: String): VerificationResponseDto =
        get("/api/v1/pipeline/user/contracts/$contractId")

    suspend fun approveContract(contractId: String, approved: Boolean): VerificationResponseDto? {
        val envelope = postEnvelope<ContractApprovalRequestDto, VerificationResponseDto>(
            path = "/api/v1/pipeline/user/contracts/approve",
            payload = ContractApprovalRequestDto(contractId = contractId, approved = approved)
        )
        return envelope.data
    }

    fun currentSession(): SessionData? = sessionStore.read()

    fun persistSession(auth: AuthResponseDto) {
        sessionStore.save(
            SessionData(
                token = auth.token,
                refreshToken = auth.refreshToken,
                userId = auth.userId,
                username = auth.username,
                legitId = auth.legitId,
                role = auth.role
            )
        )
    }

    fun clearSession() {
        sessionStore.clear()
    }

    private suspend inline fun <reified T> get(path: String): T {
        val response = client.get(baseUrl + path) {
            accept(ContentType.Application.Json)
            authorized()
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw parseError(body, response.status)
        }
        return unwrapResponse(body)
    }

    private suspend inline fun <reified B, reified T> post(path: String, payload: B): T {
        val envelope = postEnvelope<B, T>(path, payload)
        return envelope.data ?: throw LegitApiException(envelope.message)
    }

    private suspend inline fun <reified B, reified T> put(path: String, payload: B): T {
        val response = client.put(baseUrl + path) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            authorized()
            setBody(payload)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw parseError(body, response.status)
        }
        return unwrapResponse(body)
    }

    private suspend inline fun <reified B, reified T> postEnvelope(path: String, payload: B): ApiEnvelope<T> {
        val response = client.post(baseUrl + path) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            authorized()
            setBody(payload)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw parseError(body, response.status)
        }
        return parseEnvelope(body)
    }

    private fun HttpRequestBuilder.authorized() {
        val token = sessionStore.read()?.token ?: return
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private inline fun <reified T> unwrapResponse(body: String): T {
        val envelope = parseEnvelope<T>(body)
        if (!envelope.success) {
            throw LegitApiException(envelope.message)
        }
        return envelope.data ?: throw LegitApiException("Empty response from server")
    }

    private inline fun <reified T> parseEnvelope(body: String): ApiEnvelope<T> {
        return json.decodeFromString(body)
    }

    private fun parseError(body: String, status: HttpStatusCode): LegitApiException {
        val fallbackMessage = when (status) {
            HttpStatusCode.Unauthorized -> "Unauthorized. Please login again."
            HttpStatusCode.Forbidden -> "Forbidden."
            else -> "Request failed (${status.value})"
        }

        val decoded = runCatching { json.decodeFromString<ApiErrorDto>(body) }.getOrNull()
        val message = decoded?.message?.ifBlank { null } ?: fallbackMessage
        val details = decoded?.details ?: emptyMap()

        return LegitApiException(
            message = message,
            details = details,
            statusCode = status.value
        )
    }

    fun mapThrowable(error: Throwable): LegitApiException {
        return when (error) {
            is LegitApiException -> error
            is UnresolvedAddressException, is IOException -> {
                LegitApiException("Could not reach The Dedox backend at $baseUrl")
            }
            else -> LegitApiException(error.message ?: "Unexpected error")
        }
    }
}

class LegitApiException(
    override val message: String,
    val details: Map<String, String> = emptyMap(),
    val statusCode: Int? = null
) : Exception(message)
