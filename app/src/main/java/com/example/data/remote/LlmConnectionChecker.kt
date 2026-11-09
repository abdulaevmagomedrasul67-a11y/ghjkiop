package com.example.data.remote

import com.example.data.remote.llm.VibeCodeApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Проверяет доступность и валидность API ключа для указанного OpenAI-совместимого эндпоинта.
 */
class LlmConnectionChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    sealed class CheckResult {
        data class Success(val message: String) : CheckResult()
        data class Error(val message: String, val code: Int? = null) : CheckResult()
    }

    suspend fun testConnection(
        endpoint: String,
        apiKey: String,
        model: String
    ): CheckResult = withContext(Dispatchers.IO) {
        val cleanEndpoint = VibeCodeApiClient.normalizeEndpoint(endpoint)
        if (cleanEndpoint.isBlank()) {
            return@withContext CheckResult.Error("URL эндпоинта не может быть пустым")
        }

        val testUrl = VibeCodeApiClient.buildChatCompletionsUrl(cleanEndpoint)

        val jsonPayload = """
            {
               "model": "$model",
               "messages": [{"role": "user", "content": "ping"}],
               "max_tokens": 5
            }
        """.trimIndent()

        val requestBuilder = Request.Builder()
            .url(testUrl)
            .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                when (response.code) {
                    200 -> CheckResult.Success("Успешное подключение! Модель ответила 200 OK.")
                    404 -> checkModelsEndpoint(cleanEndpoint, apiKey)
                    else -> {
                        val mapped = VibeCodeApiClient.mapHttpError(response.code, response.message)
                        CheckResult.Error(mapped, response.code)
                    }
                }
            }
        } catch (e: Exception) {
            CheckResult.Error("Сетевая ошибка: ${e.localizedMessage ?: "Не удалось подключиться к серверу"}")
        }
    }

    private fun checkModelsEndpoint(endpoint: String, apiKey: String): CheckResult {
        val modelsUrl = "$endpoint/models"
        val builder = Request.Builder().url(modelsUrl).get()
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${apiKey.trim()}")
        }
        return try {
            client.newCall(builder.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    CheckResult.Success("Эндпоинт доступен! Сервер вернул список моделей.")
                } else {
                    CheckResult.Error(VibeCodeApiClient.mapHttpError(resp.code, resp.message), resp.code)
                }
            }
        } catch (e: Exception) {
            CheckResult.Error("Не удалось подключиться к $endpoint: ${e.localizedMessage}")
        }
    }
}
