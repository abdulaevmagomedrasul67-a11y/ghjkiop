package com.example.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запрос к OpenAI-совместимому эндпоинту (/v1/chat/completions).
 */
@Serializable
data class OpenAiChatRequest(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<ChatMessageDto>,
    @SerialName("temperature")
    val temperature: Double = 0.5,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("response_format")
    val responseFormat: ResponseFormatDto? = ResponseFormatDto(type = "json_object")
) {
    companion object {
        fun serializer(): KSerializer<OpenAiChatRequest> = object : KSerializer<OpenAiChatRequest> {}
    }
}

@Serializable
data class ChatMessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class ResponseFormatDto(
    @SerialName("type")
    val type: String = "json_object"
)

/**
 * Ответ от OpenAI-совместимого эндпоинта.
 */
@Serializable
data class OpenAiChatResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("choices")
    val choices: List<ChoiceDto> = emptyList(),
    @SerialName("error")
    val error: ApiErrorDto? = null
)

@Serializable
data class ChoiceDto(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("message")
    val message: ChatMessageDto? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class ApiErrorDto(
    @SerialName("message")
    val message: String = "",
    @SerialName("type")
    val type: String? = null,
    @SerialName("code")
    val code: String? = null
)
