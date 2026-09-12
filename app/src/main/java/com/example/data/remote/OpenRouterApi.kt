package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val temperature: Double? = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int? = 2048,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ChatMessagePayload(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChoicePayload>? = null,
    val error: ApiErrorPayload? = null
)

@JsonClass(generateAdapter = true)
data class ChoicePayload(
    val message: ChatMessagePayload? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorPayload(
    val message: String? = null,
    val code: Any? = null
)

@JsonClass(generateAdapter = true)
data class ModelsListResponse(
    val data: List<RemoteModelItem>? = null
)

@JsonClass(generateAdapter = true)
data class RemoteModelItem(
    val id: String,
    val name: String? = null,
    val description: String? = null
)

interface OpenRouterService {
    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://aistudio.google.com/build",
        @Header("X-Title") title: String = "Nova Chatbot AI",
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>

    @Streaming
    @POST("api/v1/chat/completions")
    suspend fun createChatCompletionStream(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://aistudio.google.com/build",
        @Header("X-Title") title: String = "Nova Chatbot AI",
        @Body request: OpenRouterRequest
    ): Response<ResponseBody>

    @GET("api/v1/models")
    suspend fun listModels(
        @Header("Authorization") authHeader: String
    ): Response<ModelsListResponse>
}

object RetrofitClient {
    private const val BASE_URL = "https://openrouter.ai/"

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val openRouterService: OpenRouterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterService::class.java)
    }
}
