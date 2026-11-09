package com.example.data.remote.llm

import android.util.Log
import com.example.domain.model.LlmSettings
import com.example.domain.repository.LlmChatOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class VibeCodeApiClient(
    private val defaultClient: OkHttpClient = OkHttpClient()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private val WEAK_MODEL_KEYWORDS = listOf("flash", "mini", "fast", "haiku", "nano", "lite", "vibe")

        /**
         * 3.1. Нормализация endpoint
         */
        fun normalizeEndpoint(url: String): String {
            var result = url.trim()
            if (result.isEmpty()) return ""
            if (!result.startsWith("http")) result = "https://$result"
            result = result.trimEnd('/')

            // Добавить /v1 если нет пути
            val afterDomain = result.substringAfter("://").substringAfter("/", "")
            if (afterDomain.isEmpty()) {
                result = "$result/v1"
            }

            return result
        }

        /**
         * Построение полного URL для chat/completions
         */
        fun buildChatCompletionsUrl(rawEndpoint: String): String {
            val normalized = normalizeEndpoint(rawEndpoint)
            if (normalized.isEmpty()) return ""
            return if (normalized.endsWith("/chat/completions")) {
                normalized
            } else {
                "$normalized/chat/completions"
            }
        }

        /**
         * 3.3. Определение «простых» / бюджетных моделей (Gemini Flash, DeepSeek Flash, Vibe, Mini, Haiku, Nano)
         */
        fun isWeakModel(model: String): Boolean {
            val lower = model.lowercase().trim()
            return WEAK_MODEL_KEYWORDS.any { lower.contains(it) }
        }

        /**
         * 3.3. Скорректированный лимит max_tokens (для простых моделей уменьшаем на 20%)
         */
        fun effectiveMaxTokens(targetTokens: Int, model: String): Int {
            return if (isWeakModel(model)) {
                (targetTokens * 0.8).toInt().coerceAtLeast(512)
            } else {
                targetTokens.coerceAtLeast(512)
            }
        }

        /**
         * 3.3. Скорректированная температура (для простых моделей 0.85)
         */
        fun effectiveTemperature(defaultTemp: Double, model: String): Double {
            return if (isWeakModel(model)) 0.85 else defaultTemp
        }

        /**
         * 3.4. Таймаут в секундах:
         * - слабые модели: 90 сек
         * - сильные модели: 180 сек
         * - потолок: 300 сек
         */
        fun timeoutForModel(model: String, requestedTimeoutSec: Long? = null): Long {
            val base = if (isWeakModel(model)) 90L else 180L
            val candidate = requestedTimeoutSec?.takeIf { it > 0 } ?: base
            return candidate.coerceIn(30L, 300L)
        }

        /**
         * 3.5. Человекочитаемое сопоставление HTTP-кодов ошибок
         */
        fun mapHttpError(code: Int, message: String = ""): String {
            return when (code) {
                401 -> "Неверный API-ключ"
                403 -> "Доступ запрещён"
                404 -> "Модель или endpoint не найдены"
                429 -> "Превышен лимит, повторите через минуту"
                500, 502, 503 -> "Ошибка на стороне LLM"
                else -> if (message.isNotBlank()) "HTTP $code: $message" else "HTTP $code"
            }
        }

        /**
         * 3.2. Коэффициенты retry при finish_reason == "length":
         * Попытка 1: 1.0, Попытка 2: 0.7, Попытка 3: 0.5, Попытка 4: 0.3
         */
        val LENGTH_RETRY_FACTORS = listOf(1.0, 0.7, 0.5, 0.3)
    }

    suspend fun generateText(
        systemPrompt: String,
        userPrompt: String,
        settings: LlmSettings
    ): Result<String> {
        val chatRes = executeChatWithLengthRetry(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            endpoint = settings.endpoint,
            apiKey = settings.apiKey,
            model = settings.model,
            targetMaxTokens = 4000,
            temperature = settings.temperature,
            timeoutSec = settings.timeout
        )
        return chatRes.map { it.content }
    }

    /**
     * 3.2. Выполнение запроса к LLM с 4-шаговым retry при finish_reason="length"
     * (1.0 -> 0.7 -> 0.5 -> 0.3 от целевого max_tokens).
     */
    suspend fun executeChatWithLengthRetry(
        systemPrompt: String,
        userPrompt: String,
        endpoint: String,
        apiKey: String,
        model: String,
        targetMaxTokens: Int = 4000,
        temperature: Double = 0.7,
        timeoutSec: Long? = null
    ): Result<LlmChatOutput> = withContext(Dispatchers.IO) {
        val baseTokens = effectiveMaxTokens(targetMaxTokens, model)
        val effTemp = effectiveTemperature(temperature, model)

        for ((index, factor) in LENGTH_RETRY_FACTORS.withIndex()) {
            val attemptTokens = (baseTokens * factor).toInt().coerceAtLeast(400)
            val attemptPrompt = if (index == 0) {
                userPrompt
            } else {
                "$userPrompt\n\n[ВАЖНО: Сократи объём ответа примерно до ${(factor * 100).toInt()}%, сохранив строгую структуру и закрыв все скобки JSON так, чтобы ответ уложился в $attemptTokens токенов без обрыва.]"
            }

            val singleResult = executeSingleChatRequest(
                systemPrompt = systemPrompt,
                userPrompt = attemptPrompt,
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                maxTokens = attemptTokens,
                temperature = effTemp,
                timeoutSec = timeoutSec
            )

            if (singleResult.isFailure) {
                return@withContext singleResult
            }

            val output = singleResult.getOrThrow()
            if (output.finishReason?.lowercase() == "length") {
                Log.w(
                    "VIBE_CODE_API",
                    "finish_reason=length on attempt ${index + 1}/${LENGTH_RETRY_FACTORS.size} (tokens=$attemptTokens)"
                )
                if (index < LENGTH_RETRY_FACTORS.lastIndex) {
                    delay(800L * (index + 1))
                    continue
                } else {
                    return@withContext Result.failure(IOException("Модель не укладывается"))
                }
            }

            return@withContext Result.success(output)
        }

        Result.failure(IOException("Модель не укладывается"))
    }

    suspend fun executeSingleChatRequest(
        systemPrompt: String,
        userPrompt: String,
        endpoint: String,
        apiKey: String,
        model: String,
        maxTokens: Int?,
        temperature: Double,
        timeoutSec: Long? = null
    ): Result<LlmChatOutput> = withContext(Dispatchers.IO) {
        val requestUrl = buildChatCompletionsUrl(endpoint)
        if (requestUrl.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("Не указан URL эндпоинта LLM. Перейдите в Настройки и укажите адрес сервера.")
            )
        }

        val resolvedTimeout = timeoutForModel(model, timeoutSec)
        val client = defaultClient.newBuilder()
            .connectTimeout(resolvedTimeout, TimeUnit.SECONDS)
            .readTimeout(resolvedTimeout, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .build()

        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }

            val cleanModel = model.trim().ifEmpty { "gpt-4o-mini" }
            val requestBodyJson = JSONObject().apply {
                put("model", cleanModel)
                put("messages", messagesArray)
                put("temperature", effectiveTemperature(temperature, cleanModel))
                if (maxTokens != null && maxTokens > 0) {
                    put("max_tokens", maxTokens)
                }
                if (!isWeakModel(cleanModel)) {
                    put("response_format", JSONObject().apply {
                        put("type", "json_object")
                    })
                }
            }

            val requestBuilder = Request.Builder()
                .url(requestUrl)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val mappedError = mapHttpError(response.code, response.message)
                    Log.e("VIBE_CODE_API", "HTTP ${response.code}: $mappedError | ${responseBody.take(250)}")
                    return@withContext Result.failure(IOException(mappedError))
                }

                if (responseBody.isBlank()) {
                    return@withContext Result.failure(IOException("Сервер вернул пустой ответ (HTTP 200)"))
                }

                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return@withContext Result.success(LlmChatOutput(content = responseBody, finishReason = "stop"))
                }

                if (jsonResponse.has("error")) {
                    val errObj = jsonResponse.optJSONObject("error")
                    val msg = errObj?.optString("message") ?: jsonResponse.optString("error")
                    return@withContext Result.failure(IOException("LLM API Error: $msg"))
                }

                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val messageObj = firstChoice.optJSONObject("message")
                    val content = messageObj?.optString("content") ?: firstChoice.optString("text", "")
                    val finishReason = firstChoice.optString("finish_reason", "").ifBlank { null }

                    if (content.isNotBlank() || finishReason == "length") {
                        return@withContext Result.success(
                            LlmChatOutput(
                                content = content,
                                finishReason = finishReason
                            )
                        )
                    }
                }

                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val rawFinish = firstCandidate.optString("finishReason", "")
                    val finishReason = if (rawFinish.equals("MAX_TOKENS", ignoreCase = true)) "length" else rawFinish.ifBlank { null }
                    val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank() || finishReason == "length") {
                            return@withContext Result.success(
                                LlmChatOutput(
                                    content = text,
                                    finishReason = finishReason
                                )
                            )
                        }
                    }
                }

                Result.failure(IOException("Модель вернула пустой или нераспознанный ответ"))
            }
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Нет подключения к интернету или неверный адрес сервера: ${e.message}"))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("Превышено время ожидания ответа от сервера (таймаут ${resolvedTimeout} сек)."))
        } catch (e: Exception) {
            Log.e("VIBE_CODE_API", "Exception while calling LLM API", e)
            Result.failure(e)
        }
    }
}
