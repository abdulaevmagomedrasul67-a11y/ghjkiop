package com.example.data.remote

import com.example.data.remote.llm.VibeCodeApiClient
import com.example.data.remote.parser.LlmJsonCleaner
import com.example.data.remote.parser.StageResponseParser
import com.example.data.remote.prompt.StagePromptBuilder
import com.example.domain.model.ContextCompressor
import com.example.domain.model.GostProfile
import com.example.domain.model.LlmSettings
import com.example.domain.model.PartialWork
import com.example.domain.model.Section
import com.example.domain.model.StagePlanner
import com.example.domain.model.StageResult
import com.example.domain.model.TitlePage
import com.example.domain.model.TocItem
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkRequest
import com.example.domain.repository.LlmChatOutput
import com.example.domain.repository.LlmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class LlmRepositoryImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val json: Json = LlmJsonCleaner.jsonConfig
) : LlmRepository {

    private val apiClient = VibeCodeApiClient(client)

    override suspend fun generateCompletion(
        systemPrompt: String,
        userPrompt: String,
        settings: LlmSettings
    ): Result<String> {
        return apiClient.generateText(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            settings = settings
        )
    }

    override suspend fun executeRawChat(
        systemPrompt: String,
        userPrompt: String,
        endpoint: String,
        apiKey: String,
        model: String,
        maxTokens: Int?,
        temperature: Double
    ): Result<LlmChatOutput> {
        return apiClient.executeChatWithLengthRetry(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            endpoint = endpoint,
            apiKey = apiKey,
            model = model,
            targetMaxTokens = maxTokens ?: 4000,
            temperature = temperature
        )
    }

    override suspend fun generateAcademicDocument(
        request: WorkRequest,
        profile: GostProfile,
        endpoint: String,
        apiKey: String,
        model: String
    ): Result<WorkDocument> = withContext(Dispatchers.IO) {
        val normalizedEndpoint = VibeCodeApiClient.normalizeEndpoint(endpoint)
        if (normalizedEndpoint.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Не указан URL эндпоинта LLM. Перейдите в Настройки и укажите адрес сервера.")
            )
        }

        val stages = StagePlanner.plan(request.type, request.targetPages)
        val accumulatedWorks = mutableListOf<PartialWork>()
        val accumulatedToc = mutableListOf<TocItem>()
        val accumulatedReferences = mutableListOf<String>()
        var runningSummary = ""

        val pauseMs = if (VibeCodeApiClient.isWeakModel(model)) 2000L else 1000L

        for ((idx, stage) in stages.withIndex()) {
            val contextSummary = if (runningSummary.isNotBlank()) {
                runningSummary
            } else {
                ContextCompressor.createThreeSentenceSummary(
                    previousWorks = accumulatedWorks,
                    topic = request.topic,
                    subject = request.subject,
                    nextStageLabel = stage.label
                )
            }

            val prompt = StagePromptBuilder.buildPrompt(
                stage = stage,
                request = request,
                previousWorks = accumulatedWorks,
                toc = accumulatedToc,
                profile = profile,
                contextSummary = contextSummary
            )

            val chatResult = executeRawChat(
                systemPrompt = StagePromptBuilder.COMMON_STAGE_SYSTEM_PROMPT,
                userPrompt = prompt,
                endpoint = normalizedEndpoint,
                apiKey = apiKey,
                model = model,
                maxTokens = stage.maxTokens,
                temperature = VibeCodeApiClient.effectiveTemperature(0.6, model)
            )

            if (chatResult.isFailure) {
                return@withContext Result.failure(
                    chatResult.exceptionOrNull() ?: Exception("Ошибка генерации этапа: ${stage.label}")
                )
            }

            val output = chatResult.getOrThrow()
            val parseResult = StageResponseParser.parse(stage, output.content)
            when (parseResult) {
                is StageResult.Content -> {
                    accumulatedWorks.addAll(parseResult.works)
                    val newText = parseResult.works.joinToString("\n") { w ->
                        "${w.title}\n${w.paragraphs.joinToString("\n")}"
                    }
                    runningSummary = ContextCompressor.updateSummary(runningSummary, newText)

                    if (!parseResult.toc.isNullOrEmpty()) {
                        accumulatedToc.clear()
                        accumulatedToc.addAll(parseResult.toc)
                    }
                    if (!parseResult.references.isNullOrEmpty()) {
                        accumulatedReferences.addAll(parseResult.references)
                    }
                }
                is StageResult.References -> {
                    accumulatedReferences.addAll(parseResult.references)
                }
                is StageResult.FullDocument -> {
                    return@withContext Result.success(parseResult.document)
                }
                is StageResult.Error -> {
                    return@withContext Result.failure(
                        Exception("Ошибка разбора ответа этапа «${stage.label}»: ${parseResult.message}")
                    )
                }
            }

            if (idx < stages.lastIndex) {
                delay(pauseMs)
            }
        }

        val allReferences = (accumulatedReferences + accumulatedWorks.filter { it.section == "references" }
            .flatMap { it.references ?: emptyList() })
            .distinct()
            .filter { it.isNotBlank() }

        val sections = accumulatedWorks
            .filter { it.section != "references" }
            .map { pw ->
                Section(
                    anchor = pw.anchor,
                    level = pw.level,
                    heading = pw.title,
                    paragraphs = pw.paragraphs
                )
            }

        val finalToc = if (accumulatedToc.isNotEmpty()) {
            val baseToc = accumulatedToc.toMutableList()
            if (allReferences.isNotEmpty() && baseToc.none { it.anchor == "h_refs" || it.anchor == "h_references" }) {
                baseToc.add(TocItem(level = 1, title = "Список использованных источников", anchor = "h_refs"))
            }
            baseToc
        } else {
            val autoToc = sections.map {
                TocItem(level = it.level, title = it.heading, anchor = it.anchor)
            }.toMutableList()
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

        val doc = WorkDocument(
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

        Result.success(doc)
    }
}
