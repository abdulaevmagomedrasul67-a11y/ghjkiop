package com.example.domain.pipeline

import android.util.Log
import com.example.domain.model.GenerationProgress
import com.example.domain.model.GenerationStage
import com.example.domain.model.LlmSettings
import com.example.domain.model.SectionType
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkSection
import com.example.domain.quantization.SectionQuantizer
import com.example.domain.repository.LlmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Последовательный контроллер генерации академических работ.
 *
 * Архитектурные принципы:
 * 1. Квантование (Quantization): математическое разбиение целевого объема (например, 45 или 80 стр.)
 *    на строгие квоты символов по разделам исходя из норматива ГОСТ (~1800 знаков на страницу).
 * 2. Итеративный цикл (Iterative Loop): последовательная генерация разделов по одному, предотвращающая
 *    перегрузку памяти и контекстного окна.
 * 3. Сжатие контекста и эстафета резюме (Summary Relay): вместо передачи всего сырого текста
 *    предыдущих глав (вызывающего переполнение окна 8k/16k бюджетных LLM), формируется плотное
 *    3-предложенческое резюме, которое передается в запрос следующего раздела.
 * 4. Анти-ИИ стиль (Burstiness & Perplexity): строгое пресечение шаблонных клише («Важно отметить»,
 *    «Таким образом», «В заключение») и требование чередования длины предложений.
 */
class SequentialGenerationController(
    private val llmRepository: LlmRepository
) {

    suspend fun generateWork(
        meta: WorkMeta,
        settings: LlmSettings,
        onProgress: (GenerationProgress) -> Unit = {}
    ): Result<WorkDocument> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting sequential generation for topic: '${meta.topic}', pages: ${meta.targetPages}")

            // ==============================================================
            // ШАГ 1: КВАНТОВАНИЕ ОБЪЕМА (STEP 1: QUANTIZATION)
            // ==============================================================
            val quantizedPlan = SectionQuantizer.quantize(meta)
            val allSections = quantizedPlan.sections

            onProgress(
                GenerationProgress(
                    stage = GenerationStage.Quantizing(
                        totalPages = quantizedPlan.totalPages,
                        totalTargetChars = quantizedPlan.totalTargetCharacters
                    ),
                    overallPercentage = 5,
                    statusMessage = "Квантование объема: ${quantizedPlan.totalPages} стр. (~${quantizedPlan.totalTargetCharacters} знаков)"
                )
            )

            // Разделы, требующие генерации текста (исключаем чистые контейнеры глав без текста)
            val generatableSections = allSections.filter { it.targetCharacters > 0 }
            val totalGeneratable = generatableSections.size

            // ==============================================================
            // ШАГ 2 И 3: ИТЕРАТИВНАЯ ГЕНЕРАЦИЯ И ЭСТАФЕТА РЕЗЮМЕ
            // (STEP 2 & 3: ITERATIVE GENERATION & SUMMARY RELAY)
            // ==============================================================
            var relaySummary: String? = null
            var generatedCount = 0

            for (section in allSections) {
                // Если это структурный заголовок главы (например "ГЛАВА 1..."), контент формируется автоматически
                if (section.targetCharacters == 0) {
                    section.content = ""
                    section.isGenerated = true
                    continue
                }

                generatedCount++
                val percent = 5 + ((generatedCount.toDouble() / totalGeneratable) * 85).toInt()

                onProgress(
                    GenerationProgress(
                        stage = GenerationStage.GeneratingSection(
                            sectionIndex = generatedCount,
                            totalSections = totalGeneratable,
                            sectionTitle = section.title,
                            targetChars = section.targetCharacters
                        ),
                        currentSectionIndex = generatedCount,
                        totalSections = totalGeneratable,
                        currentSectionTitle = section.title,
                        currentSectionTargetChars = section.targetCharacters,
                        overallPercentage = percent,
                        statusMessage = "Генерация: ${section.title} ($generatedCount из $totalGeneratable)..."
                    )
                )

                // Сборка специализированного промпта с включением эстафетного резюме (Summary Relay)
                val systemPrompt = PromptEngineer.buildSystemPrompt()
                val userPrompt = PromptEngineer.buildSectionPrompt(
                    meta = meta,
                    section = section,
                    relaySummary = relaySummary
                )

                Log.d(TAG, "Generating section [$generatedCount/$totalGeneratable]: '${section.title}' (relaySummary: ${relaySummary != null})")

                // Вызов API (Vibe Code / Gemini Flash / DeepSeek)
                val generationResult = llmRepository.generateCompletion(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    settings = settings
                )

                if (generationResult.isFailure) {
                    val err = generationResult.exceptionOrNull()?.message ?: "Ошибка генерации раздела ${section.title}"
                    Log.e(TAG, "Failed section generation: $err")
                    return@withContext Result.failure(Exception("Сбой на этапе «${section.title}»: $err"))
                }

                val rawContent = generationResult.getOrThrow()

                // Постобработка: очистка от запрещенных клише ИИ и нормализация
                val sanitizedContent = PromptEngineer.sanitizeAndHumanize(rawContent)
                section.content = sanitizedContent
                section.isGenerated = true

                Log.d(TAG, "Section '${section.title}' generated: ${sanitizedContent.length} chars (target was ${section.targetCharacters})")

                // ШАГ 3: КОМПРЕССИЯ КОНТЕКСТА ДЛЯ СЛЕДУЮЩЕГО ШАГА (Только если есть следующие разделы)
                if (generatedCount < totalGeneratable && section.type != SectionType.BIBLIOGRAPHY) {
                    onProgress(
                        GenerationProgress(
                            stage = GenerationStage.CompressingContext(section.title),
                            currentSectionIndex = generatedCount,
                            totalSections = totalGeneratable,
                            currentSectionTitle = section.title,
                            overallPercentage = percent + 1,
                            statusMessage = "Сжатие контекста: формирование 3-предложенческого резюме раздела..."
                        )
                    )

                    relaySummary = computeDenseThreeSentenceSummary(
                        sectionTitle = section.title,
                        sectionContent = sanitizedContent,
                        settings = settings
                    )

                    Log.d(TAG, "Relay summary updated for next iteration: ${relaySummary.take(120)}...")
                }
            }

            // Формируем результирующий документ
            val document = WorkDocument(
                meta = meta,
                sections = allSections
            )

            onProgress(
                GenerationProgress(
                    stage = GenerationStage.Completed(document),
                    overallPercentage = 100,
                    statusMessage = "Генерация текста успешно завершена (${quantizedPlan.totalPages} стр.)"
                )
            )

            Result.success(document)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in sequential generation pipeline", e)
            Result.failure(e)
        }
    }

    /**
     * Формирует сжатое резюме ровно из 3 предложений для эстафеты в следующий раздел.
     * Защищает от переполнения контекстного окна (context window overflow) в бюджетных моделях.
     */
    private suspend fun computeDenseThreeSentenceSummary(
        sectionTitle: String,
        sectionContent: String,
        settings: LlmSettings
    ): String {
        return try {
            val summaryPrompt = PromptEngineer.buildSummaryPrompt(sectionTitle, sectionContent)
            val result = llmRepository.generateCompletion(
                systemPrompt = "Вы — академический редактор. Отвечайте ровно тремя информативными предложениями.",
                userPrompt = summaryPrompt,
                settings = settings.copy(temperature = 0.3)
            )

            val summaryText = result.getOrNull()?.trim()
            if (!summaryText.isNullOrBlank()) {
                PromptEngineer.sanitizeAndHumanize(summaryText)
            } else {
                fallbackSummaryExtraction(sectionContent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Summary API call failed, using fallback heuristic compression", e)
            fallbackSummaryExtraction(sectionContent)
        }
    }

    /**
     * Эвристическое сжатие в 3 плотных предложения, если дополнительный микро-запрос не удался.
     */
    private fun fallbackSummaryExtraction(content: String): String {
        val sentences = content.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 25 && !it.startsWith("#") && !it.startsWith("Глава") }

        return when {
            sentences.size >= 3 -> {
                val s1 = sentences.first()
                val s2 = sentences[sentences.size / 2]
                val s3 = sentences.last()
                "$s1 $s2 $s3"
            }
            sentences.isNotEmpty() -> sentences.take(3).joinToString(" ")
            else -> "В данном разделе представлены теоретические и прикладные аспекты темы исследования."
        }
    }

    companion object {
        private const val TAG = "SequentialGen"
    }
}
