package com.example.data.remote.parser

import android.util.Log
import com.example.data.remote.mapper.toDomain
import com.example.domain.model.PartialSubsection
import com.example.domain.model.PartialWork
import com.example.domain.model.Stage
import com.example.domain.model.StageResult
import com.example.domain.model.StageType
import com.example.domain.model.TocItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object StageResponseParser {

    private val json = LlmJsonCleaner.jsonConfig

    fun parse(stage: Stage, rawContent: String): StageResult {
        return try {
            when (stage.type) {
                StageType.INTRO_AND_TOC -> parseIntroStage(rawContent)
                StageType.SUBSECTION -> parseSubsectionStage(rawContent, stage)
                StageType.CHAPTER -> parseChapterStage(rawContent, stage)
                StageType.CONCLUSION -> parseConclusionOnlyStage(rawContent)
                StageType.REFERENCES -> parseReferencesOnlyStage(rawContent)
                StageType.CONCLUSION_AND_REFS -> parseConclusionAndRefsStage(rawContent)
                StageType.ALL_IN_ONE -> parseAllInOneStage(rawContent)
            }
        } catch (e: Exception) {
            Log.e("StageParser", "Failed to parse stage ${stage.id}: ${e.message}", e)
            if (stage.type == StageType.REFERENCES) {
                val rescued = extractReferencesByRegex(rawContent)
                if (rescued.isNotEmpty()) {
                    return StageResult.Content(
                        works = listOf(
                            PartialWork(
                                section = "references",
                                title = "Список использованных источников",
                                level = 1,
                                anchor = "h_refs",
                                references = rescued
                            )
                        ),
                        references = rescued
                    )
                }
            }
            StageResult.Error("Ошибка обработки ответа этапа: ${e.message}")
        }
    }

    private fun extractCleanJson(raw: String): String {
        var text = raw.trim()
        if (text.contains("```json")) {
            text = text.substringAfter("```json")
            if (text.contains("```")) {
                text = text.substringBeforeLast("```")
            }
        } else if (text.contains("```")) {
            text = text.substringAfter("```")
            if (text.contains("```")) {
                text = text.substringBeforeLast("```")
            }
        }
        text = text.trim()

        val firstBrace = text.indexOf('{')
        val firstBracket = text.indexOf('[')

        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            val lastBracket = text.lastIndexOf(']')
            if (lastBracket > firstBracket) {
                return text.substring(firstBracket, lastBracket + 1)
            }
        }

        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1)
        }

        return text
    }

    private fun parseIntroStage(rawContent: String): StageResult {
        val cleaned = extractCleanJson(rawContent)
        val root = try {
            json.parseToJsonElement(cleaned).jsonObject
        } catch (e: Exception) {
            return parseFallbackIntro(rawContent)
        }

        val section = root["section"]?.jsonPrimitive?.content ?: "intro"
        val title = root["title"]?.jsonPrimitive?.content ?: "Введение"
        val level = root["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val anchor = root["anchor"]?.jsonPrimitive?.content ?: "h_intro"
        val paragraphs = extractStringList(root, "paragraphs", "content", "text")

        val introWork = PartialWork(
            section = section,
            title = title,
            level = level,
            anchor = anchor,
            paragraphs = paragraphs
        )

        val tocList = mutableListOf<TocItem>()
        root["toc"]?.jsonArray?.forEach { elem ->
            if (elem is JsonObject) {
                val tLevel = elem["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
                val tTitle = elem["title"]?.jsonPrimitive?.content ?: ""
                val tAnchor = elem["anchor"]?.jsonPrimitive?.content ?: ""
                if (tTitle.isNotBlank()) {
                    tocList.add(TocItem(tLevel, tTitle, tAnchor))
                }
            }
        }

        return StageResult.Content(
            works = listOf(introWork),
            toc = if (tocList.isNotEmpty()) tocList else null
        )
    }

    private fun parseSubsectionStage(rawContent: String, stage: Stage): StageResult {
        val cleaned = extractCleanJson(rawContent)
        val chNum = stage.chapterIndex.coerceAtLeast(1)
        val subNum = stage.subsectionIndex.coerceAtLeast(1)

        val root = try {
            json.parseToJsonElement(cleaned).jsonObject
        } catch (e: Exception) {
            val fallbackParagraphs = rawContent.lines()
                .map { it.trim().removeSurrounding("\"").removeSuffix("\",") }
                .filter { it.length > 35 && !it.startsWith("{") && !it.startsWith("}") && !it.startsWith("```") && !it.startsWith("\"section\"") && !it.startsWith("\"level\"") }
            if (fallbackParagraphs.isNotEmpty()) {
                val works = mutableListOf<PartialWork>()
                if (subNum == 1) {
                    works.add(
                        PartialWork(
                            section = "chapter_$chNum",
                            title = "$chNum. Глава $chNum",
                            level = 1,
                            anchor = "h_ch$chNum",
                            paragraphs = emptyList()
                        )
                    )
                }
                works.add(
                    PartialWork(
                        section = "chapter_${chNum}_$subNum",
                        title = "$chNum.$subNum. Подраздел $chNum.$subNum",
                        level = 2,
                        anchor = "h_ch${chNum}_$subNum",
                        paragraphs = fallbackParagraphs
                    )
                )
                return StageResult.Content(works = works)
            }
            return StageResult.Error("Не удалось разобрать JSON подраздела $chNum.$subNum")
        }

        val rawTitle = root["title"]?.jsonPrimitive?.content?.trim().orEmpty()
        val title = if (rawTitle.isNotBlank()) {
            if (rawTitle.startsWith("$chNum.$subNum")) rawTitle else "$chNum.$subNum $rawTitle"
        } else {
            "$chNum.$subNum Подраздел $chNum.$subNum"
        }
        val section = root["section"]?.jsonPrimitive?.content ?: "chapter_${chNum}_$subNum"
        val level = root["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: 2
        val anchor = root["anchor"]?.jsonPrimitive?.content ?: "h_ch${chNum}_$subNum"
        var paragraphs = extractStringList(root, "paragraphs", "content", "text")

        if (paragraphs.isEmpty()) {
            paragraphs = rawContent.lines()
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.length > 40 && !it.startsWith("{") && !it.startsWith("}") }
        }

        if (paragraphs.isEmpty()) {
            return StageResult.Error("Модель вернула пустой список абзацев для подраздела $chNum.$subNum")
        }

        val works = mutableListOf<PartialWork>()
        if (subNum == 1) {
            works.add(
                PartialWork(
                    section = "chapter_$chNum",
                    title = "$chNum. Глава $chNum",
                    level = 1,
                    anchor = "h_ch$chNum",
                    paragraphs = emptyList()
                )
            )
        }
        works.add(
            PartialWork(
                section = section,
                title = title,
                level = level,
                anchor = anchor,
                paragraphs = paragraphs
            )
        )

        return StageResult.Content(works = works)
    }

    private fun parseChapterStage(rawContent: String, stage: Stage): StageResult {
        val cleaned = extractCleanJson(rawContent)
        val root = try {
            json.parseToJsonElement(cleaned).jsonObject
        } catch (e: Exception) {
            return parseFallbackChapter(rawContent, stage)
        }

        val chapterNum = stage.chapterIndex.coerceAtLeast(1)
        val title = root["title"]?.jsonPrimitive?.content ?: "Глава $chapterNum"
        val section = root["section"]?.jsonPrimitive?.content ?: "chapter_$chapterNum"
        val level = root["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
        val anchor = root["anchor"]?.jsonPrimitive?.content ?: "h_ch$chapterNum"
        val paragraphs = extractStringList(root, "paragraphs", "content", "text")

        val subsections = mutableListOf<PartialSubsection>()
        root["subsections"]?.jsonArray?.forEach { elem ->
            if (elem is JsonObject) {
                val sTitle = elem["title"]?.jsonPrimitive?.content ?: ""
                val sLevel = elem["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: 2
                val sAnchor = elem["anchor"]?.jsonPrimitive?.content ?: ""
                val sParas = extractStringList(elem, "paragraphs", "content", "text")
                if (sTitle.isNotBlank()) {
                    subsections.add(PartialSubsection(sTitle, sLevel, sAnchor, sParas))
                }
            }
        }

        val chapterWork = PartialWork(
            section = section,
            title = title,
            level = level,
            anchor = anchor,
            paragraphs = paragraphs,
            subsections = subsections
        )

        val flatWorks = mutableListOf<PartialWork>()
        flatWorks.add(chapterWork)

        for (sub in subsections) {
            flatWorks.add(
                PartialWork(
                    section = "${section}_sub",
                    title = sub.title,
                    level = sub.level,
                    anchor = sub.anchor,
                    paragraphs = sub.paragraphs
                )
            )
        }

        return StageResult.Content(works = flatWorks)
    }

    private fun parseConclusionOnlyStage(rawContent: String): StageResult {
        val cleaned = extractCleanJson(rawContent)
        var paragraphs = emptyList<String>()
        var title = "Заключение"

        try {
            val root = json.parseToJsonElement(cleaned).jsonObject
            title = root["title"]?.jsonPrimitive?.content ?: "Заключение"
            paragraphs = extractStringList(root, "paragraphs", "content", "text", "conclusion")
        } catch (e: Exception) {
            Log.w("StageParser", "JSON parse error in conclusion, using fallback extraction", e)
        }

        if (paragraphs.isEmpty()) {
            paragraphs = rawContent.lines()
                .map { it.trim() }
                .filter { it.length > 30 && !it.startsWith("{") && !it.startsWith("}") && !it.startsWith("```") }
        }

        val conclusionWork = PartialWork(
            section = "conclusion",
            title = title,
            level = 1,
            anchor = "h_conclusion",
            paragraphs = paragraphs
        )

        return StageResult.Content(works = listOf(conclusionWork))
    }

    private fun parseReferencesOnlyStage(rawContent: String): StageResult {
        val cleaned = extractCleanJson(rawContent)
        val extractedReferences = mutableListOf<String>()

        try {
            val parsedElement = json.parseToJsonElement(cleaned)
            if (parsedElement is JsonObject) {
                val candidateKeys = listOf("references", "sources", "literature", "bibliography", "items", "paragraphs", "list")
                for (key in candidateKeys) {
                    val array = parsedElement[key]
                    if (array is JsonArray && array.isNotEmpty()) {
                        for (elem in array) {
                            val text = extractTextFromElement(elem)
                            if (text.isNotBlank()) {
                                extractedReferences.add(text)
                            }
                        }
                        if (extractedReferences.isNotEmpty()) break
                    }
                }
            } else if (parsedElement is JsonArray) {
                for (elem in parsedElement) {
                    val text = extractTextFromElement(elem)
                    if (text.isNotBlank()) {
                        extractedReferences.add(text)
                    }
                }
            }
        } catch (e: Exception) {
            runCatching { Log.w("StageParser", "JSON parsing failed on references, running regex rescue: ${e.message}") }
        }

        if (extractedReferences.isEmpty()) {
            val regexExtracted = extractReferencesByRegex(rawContent)
            extractedReferences.addAll(regexExtracted)
        }

        if (extractedReferences.isEmpty()) {
            return StageResult.Error(
                message = "Не удалось извлечь пункты списка литературы из ответа модели",
                lastResponseTail = rawContent.takeLast(500)
            )
        }

        val normalizedReferences = extractedReferences.mapIndexed { index, item ->
            val clean = item.replace(Regex("^[\"']|[\"']$"), "").trim()
            if (Regex("^\\d+[\\.\\)]\\s*").containsMatchIn(clean)) {
                clean
            } else {
                "${index + 1}. $clean"
            }
        }

        val refWork = PartialWork(
            section = "references",
            title = "Список использованных источников",
            level = 1,
            anchor = "h_refs",
            references = normalizedReferences
        )

        return StageResult.Content(
            works = listOf(refWork),
            references = normalizedReferences
        )
    }

    private fun extractTextFromElement(elem: JsonElement): String {
        return try {
            when {
                elem is JsonObject -> {
                    elem["title"]?.jsonPrimitive?.content
                        ?: elem["text"]?.jsonPrimitive?.content
                        ?: elem["name"]?.jsonPrimitive?.content
                        ?: elem.values.firstOrNull()?.jsonPrimitive?.content ?: ""
                }
                else -> elem.jsonPrimitive.content.trim()
            }
        } catch (e: Exception) {
            elem.toString().replace(Regex("^\"|\"$"), "")
        }
    }

    private fun extractReferencesByRegex(text: String): List<String> {
        val results = mutableListOf<String>()

        val numberedRegex = Regex("""(?m)^\s*(?:\d+[\.\)]|\[\d+\])\s*([^\n\r]{15,})""")
        numberedRegex.findAll(text).forEach { match ->
            val content = match.groupValues[1].trim()
            if (content.isNotBlank() && !content.startsWith("{") && !content.startsWith("\"references\"")) {
                results.add(content)
            }
        }

        if (results.size >= 5) {
            return results
        }

        val quotedRegex = Regex(""""((?:\d+[\.\)]\s*)?[A-ZА-ЯЁ][^"\n\r]{15,})"""")
        quotedRegex.findAll(text).forEach { match ->
            val content = match.groupValues[1].trim()
            if (content.isNotBlank() && !content.contains("references") && !content.contains("section")) {
                results.add(content)
            }
        }

        return results.distinct()
    }

    private fun parseConclusionAndRefsStage(rawContent: String): StageResult {
        val conclusionResult = parseConclusionOnlyStage(rawContent)
        val referencesResult = parseReferencesOnlyStage(rawContent)

        val works = mutableListOf<PartialWork>()
        if (conclusionResult is StageResult.Content) {
            works.addAll(conclusionResult.works)
        }
        val references = if (referencesResult is StageResult.Content) {
            referencesResult.references ?: emptyList()
        } else {
            emptyList()
        }

        return StageResult.Content(works = works, references = references)
    }

    private fun parseAllInOneStage(rawContent: String): StageResult {
        val cleaned = extractCleanJson(rawContent)
        val parsed = LlmJsonCleaner.cleanAndParse(cleaned)
        return if (parsed.isSuccess) {
            StageResult.FullDocument(parsed.getOrThrow().toDomain())
        } else {
            StageResult.Error(parsed.exceptionOrNull()?.message ?: "Ошибка парсинга полного документа")
        }
    }

    private fun extractStringList(obj: JsonObject, vararg keys: String): List<String> {
        for (key in keys) {
            val element = obj[key]
            if (element is JsonArray) {
                val list = element.mapNotNull {
                    try {
                        it.jsonPrimitive.content
                    } catch (e: Exception) {
                        null
                    }
                }
                if (list.isNotEmpty()) return list
            }
        }
        return emptyList()
    }

    private fun parseFallbackIntro(raw: String): StageResult {
        val paragraphs = raw.lines()
            .map { it.trim() }
            .filter { it.length > 40 && !it.startsWith("{") && !it.startsWith("}") && !it.startsWith("```") }

        return StageResult.Content(
            works = listOf(
                PartialWork(
                    section = "intro",
                    title = "Введение",
                    level = 1,
                    anchor = "h_intro",
                    paragraphs = paragraphs
                )
            )
        )
    }

    private fun parseFallbackChapter(raw: String, stage: Stage): StageResult {
        val paragraphs = raw.lines()
            .map { it.trim() }
            .filter { it.length > 40 && !it.startsWith("{") && !it.startsWith("}") && !it.startsWith("```") }

        val chNum = stage.chapterIndex.coerceAtLeast(1)
        return StageResult.Content(
            works = listOf(
                PartialWork(
                    section = "chapter_$chNum",
                    title = "Глава $chNum",
                    level = 1,
                    anchor = "h_ch$chNum",
                    paragraphs = paragraphs
                )
            )
        )
    }
}
