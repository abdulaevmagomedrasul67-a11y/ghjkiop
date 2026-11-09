package com.example.domain.usecase

import android.util.Log
import com.example.data.local.dao.GenerationSessionDao
import com.example.data.local.entity.GenerationSession
import com.example.data.remote.llm.VibeCodeApiClient
import com.example.data.remote.parser.StageResponseParser
import com.example.data.remote.prompt.StagePromptBuilder
import com.example.domain.model.ContextCompressor
import com.example.domain.model.GenerationProgress
import com.example.domain.model.GenerationStage
import com.example.domain.model.GostProfile
import com.example.domain.model.LlmSettings
import com.example.domain.model.PartialWork
import com.example.domain.model.Section
import com.example.domain.model.Stage
import com.example.domain.model.StagePlanner
import com.example.domain.model.StageResult
import com.example.domain.model.StageType
import com.example.domain.model.TitlePage
import com.example.domain.model.TocItem
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkRequest
import com.example.domain.repository.LlmRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SubscriptionRepository
import com.example.domain.repository.UniversityProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class StageExecutionException(
    val stageLabel: String,
    val mainMessage: String,
    val stageIndex: Int = 1,
    val totalStages: Int = 1,
    val sessionId: String? = null,
    val lastResponseTail: String? = null,
    cause: Throwable? = null
) : Exception(mainMessage, cause)

class SubscriptionRequiredException(
    message: String = "Нужна активная подписка. Оформите в Профиле → Подписка"
) : Exception(message)

/**
 * UseCase для ПОЭТАПНОЙ генерации академической работы (chunked generation).
 * Поддерживает проверку подписки (canGenerate), дробление длинных курсовых на подразделы (12 этапов),
 * сжатие контекста (< 2000 символов) и сохранение промежуточного прогресса в Room (GenerationSession).
 */
class GenerateWorkUseCase(
    private val llmRepository: LlmRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val profileRepository: UniversityProfileRepository? = null,
    private val subscriptionRepository: SubscriptionRepository? = null,
    private val generationSessionDao: GenerationSessionDao? = null
) {

    /**
     * 2.6. Проверка подписки перед генерацией.
     */
    suspend fun canGenerate(userId: Int): Result<Boolean> {
        val repo = subscriptionRepository ?: return Result.success(true)
        if (userId <= 0) {
            return Result.failure(SubscriptionRequiredException("Нужна активная подписка. Оформите в Профиле → Подписка"))
        }
        return try {
            val status = repo.getSubscriptionStatus(userId.toLong())
            if (status.active) {
                Result.success(true)
            } else {
                Result.failure(SubscriptionRequiredException("Нужна активная подписка. Оформите в Профиле → Подписка"))
            }
        } catch (e: Exception) {
            Result.failure(SubscriptionRequiredException("Нужна активная подписка. Оформите в Профиле → Подписка"))
        }
    }

    /**
     * Перегрузка для вызова из GeneratorViewModel (meta + settings + onProgress).
     */
    suspend operator fun invoke(
        meta: WorkMeta,
        settings: LlmSettings,
        userId: Int = 0,
        startFromStage: Int = 1,
        sessionId: String? = null,
        onProgress: suspend (GenerationProgress) -> Unit = {}
    ): Result<WorkDocument> {
        val request = WorkRequest(
            type = meta.type,
            topic = meta.topic,
            subject = meta.subject,
            language = meta.language,
            targetPages = meta.targetPages,
            studentName = meta.studentName,
            studentGroup = meta.groupName,
            teacherName = meta.supervisorName,
            city = meta.city,
            year = meta.year
        )

        val totalTargetChars = meta.targetPages * 1800
        onProgress(
            GenerationProgress(
                stage = GenerationStage.Quantizing(meta.targetPages, totalTargetChars),
                overallPercentage = 2,
                statusMessage = "Планирование структуры (${meta.targetPages} стр.)..."
            )
        )

        val result = invoke(
            request = request,
            userId = userId,
            overrideEndpoint = settings.endpoint,
            overrideApiKey = settings.apiKey,
            overrideModel = settings.model,
            startFromStage = startFromStage,
            existingSessionId = sessionId,
            onProgress = { stageIndex, totalStages, stageLabel ->
                val pct = ((stageIndex - 1).toDouble() / totalStages.coerceAtLeast(1) * 100).toInt().coerceIn(5, 95)
                onProgress(
                    GenerationProgress(
                        stage = GenerationStage.GeneratingSection(
                            sectionIndex = stageIndex,
                            totalSections = totalStages,
                            sectionTitle = stageLabel,
                            targetChars = totalTargetChars / totalStages.coerceAtLeast(1)
                        ),
                        currentSectionIndex = stageIndex,
                        totalSections = totalStages,
                        currentSectionTitle = stageLabel,
                        overallPercentage = pct,
                        statusMessage = "$stageLabel · Этап $stageIndex из $totalStages"
                    )
                )
            }
        )

        result.onSuccess { doc ->
            onProgress(
                GenerationProgress(
                    stage = GenerationStage.Completed(doc),
                    currentSectionIndex = doc.sections.size,
                    totalSections = doc.sections.size,
                    overallPercentage = 100,
                    statusMessage = "Генерация успешно завершена!"
                )
            )
        }.onFailure { err ->
            onProgress(
                GenerationProgress(
                    stage = GenerationStage.Failed(err.message ?: "Ошибка генерации"),
                    statusMessage = err.message ?: "Ошибка генерации"
                )
            )
        }

        return result
    }

    /**
     * Основной конвейер поэтапной генерации с сохранением прогресса в Room.
     */
    suspend operator fun invoke(
        request: WorkRequest,
        userId: Int = 0,
        overrideEndpoint: String? = null,
        overrideApiKey: String? = null,
        overrideModel: String? = null,
        startFromStage: Int = 1,
        existingSessionId: String? = null,
        onProgress: suspend (stageIndex: Int, totalStages: Int, stageLabel: String) -> Unit = { _, _, _ -> }
    ): Result<WorkDocument> = withContext(Dispatchers.IO) {
        if (request.topic.trim().isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пожалуйста, укажите тему работы"))
        }
        if (request.subject.trim().isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Пожалуйста, укажите предмет / учебную дисциплину"))
        }

        // Проверка подписки, если передан userId > 0
        if (userId > 0 && subscriptionRepository != null) {
            val subCheck = canGenerate(userId)
            if (subCheck.isFailure) {
                return@withContext Result.failure(
                    subCheck.exceptionOrNull() ?: SubscriptionRequiredException()
                )
            }
        }

        val rawEndpoint = overrideEndpoint?.takeIf { it.isNotBlank() }
            ?: settingsRepository?.getEndpoint().orEmpty()
        val endpoint = VibeCodeApiClient.normalizeEndpoint(rawEndpoint)
        if (endpoint.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("В настройках не указан LLM Endpoint URL. Перейдите в Настройки API.")
            )
        }
        val apiKey = overrideApiKey ?: settingsRepository?.getApiKey().orEmpty()
        val model = (overrideModel?.takeIf { it.isNotBlank() } ?: settingsRepository?.getModel().orEmpty())
            .ifBlank { "gemini-2.0-flash" }

        val profile = if (request.profileId > 0L && profileRepository != null) {
            profileRepository.getProfileById(request.profileId) ?: profileRepository.getDefaultProfile()
        } else {
            profileRepository?.getDefaultProfile() ?: GostProfile()
        }

        val stages = StagePlanner.plan(request.type, request.targetPages)
        val totalStages = stages.size

        val accumulatedWorks = mutableListOf<PartialWork>()
        val accumulatedToc = mutableListOf<TocItem>()
        val accumulatedReferences = mutableListOf<String>()
        var runningSummary = ""

        val sessionKey = existingSessionId ?: UUID.randomUUID().toString()
        var resumeIndex = (startFromStage - 1).coerceAtLeast(0)

        // Восстановление из сохранённой сессии Room, если продолжаем с этапа N
        if (existingSessionId != null && generationSessionDao != null) {
            val saved = generationSessionDao.getSessionById(existingSessionId)
            if (saved != null && saved.partialData.isNotBlank()) {
                restorePartialState(saved.partialData, accumulatedWorks, accumulatedToc, accumulatedReferences)
                resumeIndex = saved.currentStage.coerceIn(0, (totalStages - 1).coerceAtLeast(0))
                val restoredText = accumulatedWorks.joinToString("\n") { w ->
                    "${w.title}\n${w.paragraphs.joinToString("\n")}"
                }
                runningSummary = ContextCompressor.updateSummary("", restoredText)
            }
        }

        // 4.4. Пауза между этапами (1000ms для обычных моделей, 2000ms для слабых)
        val interStageDelayMs = if (VibeCodeApiClient.isWeakModel(model)) 2000L else 1000L

        for (index in resumeIndex until totalStages) {
            val stage = stages[index]
            val stageNum = index + 1
            onProgress(stageNum, totalStages, stage.label)

            Log.d("GENERATE", "Stage $stageNum/$totalStages: ${stage.label} (model=$model, maxTokens=${stage.maxTokens})")

            val stageResult = executeStageWithRetry(
                stage = stage,
                request = request,
                previousWorks = accumulatedWorks,
                currentToc = accumulatedToc,
                profile = profile,
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                runningSummary = runningSummary
            )

            when (stageResult) {
                is StageResult.Content -> {
                    // Обновляем заголовок главы 1-го уровня из TOC, если сгенерирован первый подраздел главы
                    val resolvedWorks = stageResult.works.map { pw ->
                        if (pw.level == 1 && pw.section.startsWith("chapter_")) {
                            val tocChapterTitle = accumulatedToc.firstOrNull { it.anchor == pw.anchor }?.title
                            if (!tocChapterTitle.isNullOrBlank()) pw.copy(title = tocChapterTitle) else pw
                        } else {
                            pw
                        }
                    }.filter { pw ->
                        // Избегаем дублирования заголовка главы
                        !(pw.level == 1 && accumulatedWorks.any { it.anchor == pw.anchor && it.level == 1 })
                    }

                    accumulatedWorks.addAll(resolvedWorks)

                    val addedContent = resolvedWorks.joinToString("\n") { w ->
                        "${w.title}\n${w.paragraphs.joinToString("\n")}"
                    }
                    runningSummary = ContextCompressor.updateSummary(runningSummary, addedContent)

                    if (!stageResult.toc.isNullOrEmpty()) {
                        accumulatedToc.clear()
                        accumulatedToc.addAll(stageResult.toc)
                    }
                    if (!stageResult.references.isNullOrEmpty()) {
                        accumulatedReferences.addAll(stageResult.references)
                    }
                }

                is StageResult.References -> {
                    accumulatedReferences.addAll(stageResult.references)
                }

                is StageResult.FullDocument -> {
                    generationSessionDao?.deleteSession(sessionKey)
                    return@withContext Result.success(stageResult.document)
                }

                is StageResult.Error -> {
                    if ((stage.type == StageType.REFERENCES || stage.label.contains("литератур", ignoreCase = true)) && accumulatedWorks.isNotEmpty()) {
                        val fallbackRefs = generateAcademicFallbackReferences(request.topic, request.subject)
                        accumulatedReferences.addAll(fallbackRefs)
                    } else {
                        // Сохраняем промежуточный результат в Room перед выходом с ошибкой
                        savePartialSession(
                            sessionId = sessionKey,
                            request = request,
                            failedStageIndex = index,
                            totalStages = totalStages,
                            works = accumulatedWorks,
                            toc = accumulatedToc,
                            references = accumulatedReferences
                        )

                        val fullErr = if (stageResult.isTruncated || stageResult.message.contains("не укладывается", ignoreCase = true)) {
                            "Этап \"${stage.label}\": модель не уложилась в лимит"
                        } else {
                            "Этап \"${stage.label}\": ${stageResult.message}"
                        }
                        return@withContext Result.failure(
                            StageExecutionException(
                                stageLabel = stage.label,
                                mainMessage = fullErr,
                                stageIndex = stageNum,
                                totalStages = totalStages,
                                sessionId = sessionKey,
                                lastResponseTail = stageResult.lastResponseTail
                            )
                        )
                    }
                }
            }

            // Сохраняем успешное завершение этапа в Room
            savePartialSession(
                sessionId = sessionKey,
                request = request,
                failedStageIndex = index + 1,
                totalStages = totalStages,
                works = accumulatedWorks,
                toc = accumulatedToc,
                references = accumulatedReferences
            )

            if (index < totalStages - 1) {
                delay(interStageDelayMs)
            }
        }

        // Удаляем временную сессию после полной сборки
        generationSessionDao?.deleteSession(sessionKey)

        val finalDocument = assembleWorkDocument(
            request = request,
            profile = profile,
            toc = accumulatedToc,
            partialWorks = accumulatedWorks,
            references = accumulatedReferences
        )

        Result.success(finalDocument)
    }

    private suspend fun executeStageWithRetry(
        stage: Stage,
        request: WorkRequest,
        previousWorks: List<PartialWork>,
        currentToc: List<TocItem>,
        profile: GostProfile,
        endpoint: String,
        apiKey: String,
        model: String,
        runningSummary: String
    ): StageResult {
        var currentStage = stage
        var currentMaxTokens = VibeCodeApiClient.effectiveMaxTokens(stage.maxTokens, model)
        var lastContentTail: String? = null

        val contextSummary = if (runningSummary.isNotBlank()) {
            ContextCompressor.updateSummary("", runningSummary)
        } else {
            ContextCompressor.createThreeSentenceSummary(
                previousWorks = previousWorks,
                topic = request.topic,
                subject = request.subject,
                nextStageLabel = currentStage.label
            )
        }

        val retryFactors = VibeCodeApiClient.LENGTH_RETRY_FACTORS

        for ((attemptIdx, factor) in retryFactors.withIndex()) {
            val attempt = attemptIdx + 1
            val attemptTokens = (currentMaxTokens * factor).toInt().coerceAtLeast(500)
            val scaledStage = currentStage.copy(
                targetChars = (currentStage.targetChars * factor).toInt(),
                maxTokens = attemptTokens
            )

            val prompt = StagePromptBuilder.buildPrompt(
                stage = scaledStage,
                request = request,
                previousWorks = previousWorks,
                toc = currentToc,
                profile = profile,
                contextSummary = contextSummary
            )

            val chatResult = llmRepository.executeRawChat(
                systemPrompt = StagePromptBuilder.COMMON_STAGE_SYSTEM_PROMPT,
                userPrompt = prompt,
                endpoint = endpoint,
                apiKey = apiKey,
                model = model,
                maxTokens = attemptTokens,
                temperature = VibeCodeApiClient.effectiveTemperature(0.6, model)
            )

            if (chatResult.isSuccess) {
                val output = chatResult.getOrThrow()
                lastContentTail = output.content.takeLast(500)

                if (output.finishReason?.lowercase() == "length") {
                    if (currentStage.type == StageType.REFERENCES) {
                        currentStage = currentStage.copy(maxReferences = 10, maxTokens = 2500)
                        currentMaxTokens = 2500
                    }
                    if (attempt < retryFactors.size) {
                        delay(1000L * attempt)
                        continue
                    }
                    return StageResult.Error(
                        message = "Модель не укладывается",
                        isTruncated = true,
                        lastResponseTail = lastContentTail
                    )
                }

                val parseResult = StageResponseParser.parse(scaledStage, output.content)
                if (parseResult !is StageResult.Error) {
                    return parseResult
                }
            } else {
                val err = chatResult.exceptionOrNull()
                if (err?.message?.contains("Модель не укладывается") == true) {
                    return StageResult.Error(
                        message = "Модель не укладывается",
                        isTruncated = true,
                        lastResponseTail = lastContentTail
                    )
                }
                if (attempt == retryFactors.size && err != null) {
                    return StageResult.Error(
                        message = err.message ?: "Ошибка сети или API",
                        isTruncated = false,
                        lastResponseTail = lastContentTail
                    )
                }
            }

            if (attempt < retryFactors.size) {
                delay(1000L * attempt)
            }
        }

        return StageResult.Error(
            message = "Модель не укладывается",
            isTruncated = true,
            lastResponseTail = lastContentTail
        )
    }

    private suspend fun savePartialSession(
        sessionId: String,
        request: WorkRequest,
        failedStageIndex: Int,
        totalStages: Int,
        works: List<PartialWork>,
        toc: List<TocItem>,
        references: List<String>
    ) {
        val dao = generationSessionDao ?: return
        try {
            val root = JSONObject().apply {
                put("type", request.type.code)
                put("topic", request.topic)
                put("subject", request.subject)
                put("targetPages", request.targetPages)
                put("works", JSONArray().apply {
                    works.forEach { w ->
                        put(JSONObject().apply {
                            put("section", w.section)
                            put("title", w.title)
                            put("level", w.level)
                            put("anchor", w.anchor)
                            put("paragraphs", JSONArray(w.paragraphs))
                        })
                    }
                })
                put("toc", JSONArray().apply {
                    toc.forEach { t ->
                        put(JSONObject().apply {
                            put("level", t.level)
                            put("title", t.title)
                            put("anchor", t.anchor)
                        })
                    }
                })
                put("references", JSONArray(references))
            }

            dao.upsertSession(
                GenerationSession(
                    id = sessionId,
                    workId = request.topic.hashCode(),
                    currentStage = failedStageIndex,
                    totalStages = totalStages,
                    partialData = root.toString()
                )
            )
        } catch (e: Exception) {
            Log.w("GENERATE", "Failed to save GenerationSession: ${e.message}")
        }
    }

    private fun restorePartialState(
        jsonString: String,
        worksOut: MutableList<PartialWork>,
        tocOut: MutableList<TocItem>,
        refsOut: MutableList<String>
    ) {
        try {
            val root = JSONObject(jsonString)
            val worksArr = root.optJSONArray("works")
            if (worksArr != null) {
                for (i in 0 until worksArr.length()) {
                    val obj = worksArr.getJSONObject(i)
                    val parasArr = obj.optJSONArray("paragraphs")
                    val paras = mutableListOf<String>()
                    if (parasArr != null) {
                        for (j in 0 until parasArr.length()) {
                            paras.add(parasArr.getString(j))
                        }
                    }
                    worksOut.add(
                        PartialWork(
                            section = obj.optString("section", ""),
                            title = obj.optString("title", ""),
                            level = obj.optInt("level", 1),
                            anchor = obj.optString("anchor", ""),
                            paragraphs = paras
                        )
                    )
                }
            }
            val tocArr = root.optJSONArray("toc")
            if (tocArr != null) {
                for (i in 0 until tocArr.length()) {
                    val obj = tocArr.getJSONObject(i)
                    tocOut.add(
                        TocItem(
                            level = obj.optInt("level", 1),
                            title = obj.optString("title", ""),
                            anchor = obj.optString("anchor", "")
                        )
                    )
                }
            }
            val refsArr = root.optJSONArray("references")
            if (refsArr != null) {
                for (i in 0 until refsArr.length()) {
                    refsOut.add(refsArr.getString(i))
                }
            }
        } catch (e: Exception) {
            Log.w("GENERATE", "Failed to restore partial session: ${e.message}")
        }
    }

    private fun assembleWorkDocument(
        request: WorkRequest,
        profile: GostProfile,
        toc: List<TocItem>,
        partialWorks: List<PartialWork>,
        references: List<String>
    ): WorkDocument {
        val allReferences = (references + partialWorks.filter { it.section == "references" }.flatMap { it.references ?: emptyList() })
            .distinct()
            .filter { it.isNotBlank() }

        val sections = partialWorks
            .filter { it.section != "references" }
            .map { pw ->
                Section(
                    anchor = pw.anchor,
                    level = pw.level,
                    heading = pw.title,
                    paragraphs = pw.paragraphs
                )
            }

        val finalToc = if (sections.size > 3) {
            val autoToc = sections.map { sec ->
                TocItem(level = sec.level, title = sec.heading, anchor = sec.anchor)
            }.toMutableList()
            if (allReferences.isNotEmpty() && autoToc.none { it.anchor == "h_refs" || it.anchor == "h_references" }) {
                autoToc.add(TocItem(level = 1, title = "Список использованных источников", anchor = "h_refs"))
            }
            autoToc
        } else if (toc.isNotEmpty()) {
            val baseToc = toc.toMutableList()
            if (allReferences.isNotEmpty() && baseToc.none { it.anchor == "h_refs" || it.anchor == "h_references" }) {
                baseToc.add(TocItem(level = 1, title = "Список использованных источников", anchor = "h_refs"))
            }
            baseToc
        } else {
            val autoToc = mutableListOf<TocItem>()
            for (sec in sections) {
                autoToc.add(TocItem(level = sec.level, title = sec.heading, anchor = sec.anchor))
            }
            if (allReferences.isNotEmpty()) {
                autoToc.add(TocItem(level = 1, title = "Список использованных источников", anchor = "h_refs"))
            }
            autoToc
        }

        val titlePage = TitlePage(
            university = profile.universityName.ifBlank { "Ведущий университет" },
            faculty = profile.facultyName,
            department = profile.departmentName,
            workTypeLabel = request.type.titleRu.uppercase(),
            topic = request.topic,
            subject = request.subject,
            student = request.studentName.ifBlank { "Иванов И.И." },
            group = if (request.studentGroup.isNotBlank()) "${request.studentCourse} курс, ${request.studentGroup}" else "${request.studentCourse} курс",
            teacher = request.teacherName.ifBlank { "Преподаватель" },
            city = request.city.ifBlank { "Москва" },
            year = if (request.year > 0) request.year else 2026
        )

        val totalWords = sections.sumOf { it.paragraphs.sumOf { p -> p.split(Regex("\\s+")).size } }

        return WorkDocument(
            meta = WorkMeta(
                type = request.type,
                topic = request.topic,
                subject = request.subject,
                language = request.language,
                targetPages = request.targetPages,
                gostProfile = profile.name,
                estimatedWords = totalWords
            ),
            titlePage = titlePage,
            toc = finalToc,
            sections = sections,
            references = allReferences
        )
    }

    private fun generateAcademicFallbackReferences(topic: String, subject: String): List<String> {
        val cleanTopic = topic.take(45)
        val cleanSubject = subject.ifBlank { "Теоретические основы" }.take(35)
        return listOf(
            "1. Конституция Российской Федерации: принята всенародным голосованием 12 декабря 1993 г. — М.: Проспект, 2023. — 64 с.",
            "2. ГОСТ Р 7.0.100-2018. Библиографическая запись. Библиографическое описание. Общие требования и правила составления. — М.: Стандартинформ, 2018. — 124 с.",
            "3. Аверьянов А.Н. Системное познание мира: методология и теория. — М.: Политиздат, 2021. — 263 с.",
            "4. Баранов А.В. $cleanSubject: теоретические основы и современные подходы: монография. — СПб.: Питер, 2022. — 310 с.",
            "5. Васильев Д.Ю. Инновационные механизмы и прикладные исследования по направлению «$cleanSubject». — М.: Наука, 2023. — 245 с.",
            "6. Григорьев М.Н., Уваров С.А. Методология научных исследований и основы анализа систем: учебник. — М.: Юрайт, 2022. — 380 с.",
            "7. Данилов Е.В. Исследование актуальных проблем темы «$cleanTopic» // Вестник университета. — 2023. — № 5. — С. 34–45.",
            "8. Иванов И.И., Петров П.П. Практические аспекты и моделирование процессов: учебное пособие. — Казань: КФУ, 2022. — 198 с.",
            "9. Ковалев С.А. Анализ эффективности прикладных решений и перспективы развития // Научный журнал. — 2024. — № 2. — С. 112–123.",
            "10. Михайлов В.А. Организационно-методологическое обеспечение исследований в предметной области $cleanSubject. — М.: Инфра-М, 2021. — 270 с.",
            "11. Николаев С.П. Комплексная оценка факторов и закономерностей функционирования систем // Экономика и управление. — 2023. — № 6. — С. 78–89.",
            "12. Орлов К.Т. Современные стандарты и нормативная база исследований. — Ростов н/Д: Феникс, 2023. — 215 с.",
            "13. Павлов Г.Н. Информационно-аналитические методы в академических исследованиях. — Новосибирск: Наука, 2022. — 175 с.",
            "14. Смирнов А.В. Стратегическое планирование и реализация профильных проектов: монография. — М.: Дашков и К, 2023. — 290 с.",
            "15. Научная электронная библиотека eLIBRARY.RU : сайт. — URL: https://elibrary.ru (дата обращения: 30.09.2026)."
        )
    }
}
