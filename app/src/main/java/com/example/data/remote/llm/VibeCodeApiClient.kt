package com.example.data.remote.llm

import android.util.Log
import com.example.domain.model.LlmSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class VibeCodeApiClient(
    private val defaultClient: OkHttpClient = OkHttpClient()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateText(
        systemPrompt: String,
        userPrompt: String,
        settings: LlmSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        val client = defaultClient.newBuilder()
            .connectTimeout(settings.timeout, TimeUnit.SECONDS)
            .readTimeout(settings.timeout, TimeUnit.SECONDS)
            .writeTimeout(settings.timeout, TimeUnit.SECONDS)
            .build()

        try {
            // Формируем стандартный OpenAI/VibeCode-совместимый JSON для чат-комплишена
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

            val requestBodyJson = JSONObject().apply {
                put("model", settings.model)
                put("messages", messagesArray)
                put("temperature", settings.temperature)
                put("top_p", settings.topP)
                put("max_tokens", 4096)
            }

            val requestBuilder = Request.Builder()
                .url(settings.endpoint)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            if (settings.apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${settings.apiKey.trim()}")
            }

            val request = requestBuilder.build()
            Log.d("VIBE_CODE_API", "Sending request to ${settings.endpoint} (model: ${settings.model})")

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                val errorMsg = "HTTP ${response.code}: ${response.message} — ${responseBody?.take(300)}"
                Log.e("VIBE_CODE_API", errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Парсим ответ OpenAI-совместимого формата
            val jsonResponse = JSONObject(responseBody)
            if (jsonResponse.has("error")) {
                val errObj = jsonResponse.optJSONObject("error")
                val msg = errObj?.optString("message") ?: jsonResponse.optString("error")
                return@withContext Result.failure(Exception("LLM API Error: $msg"))
            }

            val choices = jsonResponse.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val messageObj = firstChoice.optJSONObject("message")
                val content = messageObj?.optString("content") ?: firstChoice.optString("text", "")
                if (content.isNotBlank()) {
                    return@withContext Result.success(content)
                }
            }

            // Fallback для Gemini REST формата: candidates[0].content.parts[0].text
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }
            }

            Result.failure(Exception("Empty or unrecognized response structure from LLM endpoint"))
        } catch (e: Exception) {
            Log.e("VIBE_CODE_API", "Exception while calling LLM API", e)
            Result.failure(e)
        }
    }
}
