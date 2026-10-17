package com.example.data.remote

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
        val cleanEndpoint = endpoint.trim().removeSuffix("/")
        if (cleanEndpoint.isBlank()) {
            return@withContext CheckResult.Error("URL эндпоинта не может быть пустым")
        }

        // Тестируем через лёгкий запрос models или chat/completions с max_tokens=1
        val testUrl = if (cleanEndpoint.endsWith("/chat/completions")) {
            cleanEndpoint
        } else {
            "$cleanEndpoint/chat/completions"
        }

        val jsonPayload = """
            {
               "model": "$model",
               "messages": [{"role": "user", "content": "ping"}],
               "max_tokens": 1
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
                    200 -> {
                        CheckResult.Success("Успешное подключение! Модель ответила 200 OK.")
                    }
                    401 -> {
                        CheckResult.Error("Ошибка 401 Unauthorized: Неверный API-ключ", 401)
                    }
                    403 -> {
                        CheckResult.Error("Ошибка 403 Forbidden: Доступ запрещён или нет прав к модели", 403)
                    }
                    404 -> {
                        // Попробуем альтернативно проверить /models
                        checkModelsEndpoint(cleanEndpoint, apiKey)
                    }
                    429 -> {
                        CheckResult.Error("Ошибка 429 Rate Limit: Превышен лимит запросов или баланс исчерпан", 429)
                    }
                    else -> {
                        val body = response.body?.string()?.take(200) ?: ""
                        CheckResult.Error("Ответ сервера HTTP ${response.code}: $body", response.code)
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
                    CheckResult.Error("Сервер вернул HTTP ${resp.code} на $modelsUrl", resp.code)
                }
            }
        } catch (e: Exception) {
            CheckResult.Error("Не удалось подключиться к $endpoint: ${e.localizedMessage}")
        }
    }
}
