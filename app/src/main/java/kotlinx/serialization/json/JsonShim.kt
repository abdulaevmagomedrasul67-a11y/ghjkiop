package kotlinx.serialization.json

import com.example.data.remote.dto.ApiErrorDto
import com.example.data.remote.dto.AppendixDto
import com.example.data.remote.dto.ChatMessageDto
import com.example.data.remote.dto.ChoiceDto
import com.example.data.remote.dto.OpenAiChatRequest
import com.example.data.remote.dto.OpenAiChatResponse
import com.example.data.remote.dto.SectionDto
import com.example.data.remote.dto.TitlePageDto
import com.example.data.remote.dto.TocItemDto
import com.example.data.remote.dto.WorkDocumentDto
import com.example.data.remote.dto.WorkMetaDto
import com.example.domain.model.FeedbackTicket
import com.squareup.moshi.Moshi
import kotlinx.serialization.KSerializer

sealed class JsonElement

class JsonPrimitive(val content: String) : JsonElement() {
    override fun toString(): String = content
}

class JsonArray(private val elements: List<JsonElement>) : JsonElement(), List<JsonElement> by elements {
    override fun toString(): String = elements.toString()
}

class JsonObject(private val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {
    override fun toString(): String = content.toString()
}

val JsonElement.jsonObject: JsonObject
    get() = this as? JsonObject ?: throw IllegalArgumentException("Element is not a JsonObject")

val JsonElement.jsonArray: JsonArray
    get() = this as? JsonArray ?: throw IllegalArgumentException("Element is not a JsonArray")

val JsonElement.jsonPrimitive: JsonPrimitive
    get() = when (this) {
        is JsonPrimitive -> this
        else -> JsonPrimitive(this.toString())
    }

class JsonBuilder {
    var ignoreUnknownKeys: Boolean = true
    var isLenient: Boolean = true
    var coerceInputValues: Boolean = true
    var encodeDefaults: Boolean = true
}

fun Json(builderAction: JsonBuilder.() -> Unit = {}): Json {
    val builder = JsonBuilder().apply(builderAction)
    return Json(builder)
}

class Json(val configuration: JsonBuilder) {

    companion object {
        val Default = Json(JsonBuilder())
        private val moshi: Moshi = Moshi.Builder().build()
        private val anyAdapter = moshi.adapter(Any::class.java).lenient()
    }

    fun parseToJsonElement(string: String): JsonElement {
        val trimmed = string.trim()
        if (trimmed.isEmpty()) return JsonPrimitive("")
        val parsed = anyAdapter.fromJson(trimmed)
        return convertAny(parsed)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertMap(map: Map<*, *>): JsonObject {
        val out = mutableMapOf<String, JsonElement>()
        for ((k, v) in map) {
            if (k != null) {
                out[k.toString()] = convertAny(v)
            }
        }
        return JsonObject(out)
    }

    private fun convertList(list: List<*>): JsonArray {
        return JsonArray(list.map { convertAny(it) })
    }

    private fun convertAny(value: Any?): JsonElement {
        return when (value) {
            null -> JsonPrimitive("")
            is Map<*, *> -> convertMap(value)
            is List<*> -> convertList(value)
            is Double -> {
                if (value % 1.0 == 0.0) {
                    JsonPrimitive(value.toLong().toString())
                } else {
                    JsonPrimitive(value.toString())
                }
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    inline fun <reified T> decodeFromString(string: String): T {
        return decodeFromString(T::class.java, string)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> decodeFromString(clazz: Class<T>, string: String): T {
        val trimmed = string.trim()
        val parsed = anyAdapter.fromJson(trimmed)
        if (clazz == WorkDocumentDto::class.java && parsed is Map<*, *>) {
            return parseWorkDocumentDto(parsed as Map<String, Any?>) as T
        }
        if (clazz == OpenAiChatResponse::class.java && parsed is Map<*, *>) {
            return parseOpenAiChatResponse(parsed as Map<String, Any?>) as T
        }
        if (List::class.java.isAssignableFrom(clazz) && parsed is List<*>) {
            val list = mutableListOf<FeedbackTicket>()
            for (item in parsed) {
                val o = item as? Map<*, *> ?: continue
                list.add(
                    FeedbackTicket(
                        id = o["id"]?.toString().orEmpty(),
                        userId = (o["userId"] as? Number)?.toLong() ?: 0L,
                        userEmail = o["userEmail"]?.toString().orEmpty(),
                        category = o["category"]?.toString().orEmpty(),
                        workTopic = o["workTopic"]?.toString().orEmpty(),
                        description = o["description"]?.toString().orEmpty(),
                        createdAt = (o["createdAt"] as? Number)?.toLong() ?: 0L,
                        status = o["status"]?.toString().orEmpty()
                    )
                )
            }
            return list as T
        }
        throw IllegalArgumentException("Unsupported decodeFromString type: ${clazz.name}")
    }

    inline fun <reified T> encodeToString(value: T): String {
        return encodeToString(T::class.java, value)
    }

    fun <T> encodeToString(serializer: KSerializer<T>, value: T): String {
        return encodeToString(value?.javaClass ?: Any::class.java, value)
    }

    fun <T> encodeToString(clazz: Class<*>, value: T): String {
        if (value is OpenAiChatRequest) {
            val map = linkedMapOf<String, Any?>(
                "model" to value.model,
                "temperature" to value.temperature,
                "messages" to value.messages.map { m ->
                    mapOf("role" to m.role, "content" to m.content)
                }
            )
            if (value.maxTokens != null) {
                map["max_tokens"] = value.maxTokens
            }
            if (value.responseFormat != null) {
                map["response_format"] = mapOf("type" to value.responseFormat.type)
            }
            return anyAdapter.toJson(map)
        }
        if (value is List<*>) {
            val list = value.mapNotNull { item ->
                if (item is FeedbackTicket) {
                    mapOf(
                        "id" to item.id,
                        "userId" to item.userId,
                        "userEmail" to item.userEmail,
                        "category" to item.category,
                        "workTopic" to item.workTopic,
                        "description" to item.description,
                        "createdAt" to item.createdAt,
                        "status" to item.status
                    )
                } else null
            }
            return anyAdapter.toJson(list)
        }
        return anyAdapter.toJson(value)
    }

    private fun Map<String, Any?>.optString(key: String, default: String = ""): String {
        return this[key]?.toString() ?: default
    }

    private fun Map<String, Any?>.optInt(key: String, default: Int = 0): Int {
        val v = this[key]
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseOpenAiChatResponse(obj: Map<String, Any?>): OpenAiChatResponse {
        val choicesArr = obj["choices"] as? List<*>
        val choices = mutableListOf<ChoiceDto>()
        if (choicesArr != null) {
            for ((i, item) in choicesArr.withIndex()) {
                val c = item as? Map<String, Any?> ?: continue
                val msgObj = c["message"] as? Map<String, Any?>
                val msg = if (msgObj != null) {
                    ChatMessageDto(
                        role = msgObj.optString("role", "assistant"),
                        content = msgObj.optString("content", "")
                    )
                } else null
                val finishReason = c["finish_reason"]?.toString()
                choices.add(ChoiceDto(index = c.optInt("index", i), message = msg, finishReason = finishReason))
            }
        }
        val errObj = obj["error"] as? Map<String, Any?>
        val error = if (errObj != null) {
            ApiErrorDto(
                message = errObj.optString("message", ""),
                type = errObj["type"]?.toString(),
                code = errObj["code"]?.toString()
            )
        } else null
        return OpenAiChatResponse(
            id = obj["id"]?.toString(),
            choices = choices,
            error = error
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWorkDocumentDto(obj: Map<String, Any?>): WorkDocumentDto {
        val metaObj = obj["meta"] as? Map<String, Any?>
        val meta = if (metaObj != null) {
            WorkMetaDto(
                type = metaObj.optString("type", ""),
                topic = metaObj.optString("topic", ""),
                subject = metaObj.optString("subject", ""),
                language = metaObj.optString("language", "ru"),
                targetPages = metaObj.optInt("target_pages", 0),
                gostProfile = metaObj.optString("gost_profile", "gost"),
                estimatedWords = metaObj.optInt("estimated_words", 0)
            )
        } else WorkMetaDto()

        val tpObj = obj["title_page"] as? Map<String, Any?>
        val titlePage = if (tpObj != null) {
            TitlePageDto(
                university = tpObj.optString("university", ""),
                faculty = tpObj.optString("faculty", ""),
                department = tpObj.optString("department", ""),
                workTypeLabel = tpObj.optString("work_type_label", ""),
                topic = tpObj.optString("topic", ""),
                subject = tpObj.optString("subject", ""),
                student = tpObj.optString("student", ""),
                group = tpObj.optString("group", ""),
                teacher = tpObj.optString("teacher", ""),
                city = tpObj.optString("city", ""),
                year = tpObj.optInt("year", 2026)
            )
        } else TitlePageDto()

        val tocArr = obj["toc"] as? List<*>
        val toc = mutableListOf<TocItemDto>()
        if (tocArr != null) {
            for (item in tocArr) {
                val t = item as? Map<String, Any?> ?: continue
                toc.add(
                    TocItemDto(
                        level = t.optInt("level", 1),
                        title = t.optString("title", ""),
                        anchor = t.optString("anchor", "")
                    )
                )
            }
        }

        val secArr = obj["sections"] as? List<*>
        val sections = mutableListOf<SectionDto>()
        if (secArr != null) {
            for (item in secArr) {
                val s = item as? Map<String, Any?> ?: continue
                val pArr = s["paragraphs"] as? List<*>
                val paragraphs = pArr?.mapNotNull { it?.toString() } ?: emptyList()
                sections.add(
                    SectionDto(
                        anchor = s.optString("anchor", ""),
                        level = s.optInt("level", 1),
                        heading = s.optString("heading", ""),
                        paragraphs = paragraphs
                    )
                )
            }
        }

        val refArr = obj["references"] as? List<*>
        val references = refArr?.mapNotNull { it?.toString() } ?: emptyList()

        val appArr = obj["appendix"] as? List<*>
        val appendix = mutableListOf<AppendixDto>()
        if (appArr != null) {
            for (item in appArr) {
                val a = item as? Map<String, Any?> ?: continue
                appendix.add(
                    AppendixDto(
                        title = a.optString("title", ""),
                        content = a.optString("content", "")
                    )
                )
            }
        }

        return WorkDocumentDto(
            meta = meta,
            titlePage = titlePage,
            toc = toc,
            sections = sections,
            references = references,
            appendix = appendix
        )
    }
}
