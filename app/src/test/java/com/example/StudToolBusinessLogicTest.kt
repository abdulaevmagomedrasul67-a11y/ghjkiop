package com.example

import com.example.data.remote.llm.VibeCodeApiClient
import com.example.data.remote.parser.StageResponseParser
import com.example.domain.model.ContextCompressor
import com.example.domain.model.Stage
import com.example.domain.model.StagePlanner
import com.example.domain.model.StageResult
import com.example.domain.model.StageType
import com.example.domain.model.SubscriptionOrder
import com.example.domain.model.SubscriptionPlanType
import com.example.domain.model.SubscriptionStatus
import com.example.domain.model.SupportCategory
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import com.example.domain.repository.LlmRepository
import com.example.domain.repository.SubscriptionRepository
import com.example.domain.usecase.GenerateWorkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudToolBusinessLogicTest {

    @Test
    fun testSupportCategoriesCount() {
        val categories = SupportCategory.entries
        assertEquals(5, categories.size)
        assertNotNull(SupportCategory.fromCode("refund"))
        assertNotNull(SupportCategory.fromCode("password"))
        assertNotNull(SupportCategory.fromCode("bug"))
        assertNotNull(SupportCategory.fromCode("question"))
        assertNotNull(SupportCategory.fromCode("other"))
    }

    @Test
    fun testSubscriptionPricingAndFirstTimeLogic() {
        assertEquals(1800, SubscriptionPlanType.MONTHLY_PRICE_RUB)
        assertEquals(14000, SubscriptionPlanType.YEARLY_FIRST_TIME_PRICE_RUB)
        assertEquals(21600, SubscriptionPlanType.YEARLY_RENEWAL_PRICE_RUB)
        assertEquals(7600, SubscriptionPlanType.YEARLY_SAVINGS_RUB)

        val newUserStatus = SubscriptionStatus(active = false, hasHadSubscription = false)
        assertEquals(14000, newUserStatus.yearlyPriceRub)
        assertEquals("Оформить за 14 000 ₽", newUserStatus.yearlyButtonText)

        val returningUserStatus = SubscriptionStatus(active = true, hasHadSubscription = true, daysLeft = 45)
        assertEquals(21600, returningUserStatus.yearlyPriceRub)
        assertEquals("Продлить за 21 600 ₽", returningUserStatus.yearlyButtonText)
        assertEquals(45, returningUserStatus.daysRemaining)
    }

    @Test
    fun testCanGenerateChecksSubscriptionStatus() = runBlocking {
        val fakeLlmRepo = object : LlmRepository {
            override suspend fun generateCompletion(
                systemPrompt: String,
                userPrompt: String,
                settings: com.example.domain.model.LlmSettings
            ): Result<String> = Result.success("ok")
        }

        var isSubActive = false
        val fakeSubRepo = object : SubscriptionRepository {
            override val statusFlow: StateFlow<SubscriptionStatus> =
                MutableStateFlow(SubscriptionStatus(active = isSubActive))
            override val ordersFlow: StateFlow<List<SubscriptionOrder>> =
                MutableStateFlow(emptyList())

            override suspend fun getSubscriptionStatus(userId: Long): SubscriptionStatus {
                return SubscriptionStatus(active = isSubActive, hasHadSubscription = isSubActive)
            }

            override suspend fun createSubscriptionOrder(
                userId: Long,
                planType: SubscriptionPlanType
            ): Result<SubscriptionOrder> = Result.failure(UnsupportedOperationException())

            override suspend fun confirmOrderPaid(orderId: Long): Result<SubscriptionOrder> =
                Result.failure(UnsupportedOperationException())

            override suspend fun getMySubscriptions(userId: Long): Result<List<SubscriptionOrder>> =
                Result.success(emptyList())

            override suspend fun cancelAutoRenewal(userId: Long): Result<SubscriptionStatus> =
                Result.success(SubscriptionStatus(active = isSubActive, autoRenew = false))
        }

        val useCase = GenerateWorkUseCase(
            llmRepository = fakeLlmRepo,
            subscriptionRepository = fakeSubRepo
        )

        val blockedResult = useCase.canGenerate(userId = 10)
        assertTrue(blockedResult.isFailure)
        assertTrue(blockedResult.exceptionOrNull()?.message?.contains("Нужна активная подписка") == true)

        isSubActive = true
        val allowedResult = useCase.canGenerate(userId = 10)
        assertTrue(allowedResult.isSuccess)
        assertEquals(true, allowedResult.getOrNull())
    }

    @Test
    fun testNormalizeEndpointAndWeakModelDetection() {
        assertEquals("https://api.openai.com/v1", VibeCodeApiClient.normalizeEndpoint("api.openai.com"))
        assertEquals("https://api.deepseek.com/v1", VibeCodeApiClient.normalizeEndpoint("https://api.deepseek.com/"))
        assertEquals(
            "https://openrouter.ai/api/v1",
            VibeCodeApiClient.normalizeEndpoint("https://openrouter.ai/api/v1/")
        )
        assertEquals("", VibeCodeApiClient.normalizeEndpoint("   "))

        assertTrue(VibeCodeApiClient.isWeakModel("gemini-2.0-flash"))
        assertTrue(VibeCodeApiClient.isWeakModel("deepseek-fast"))
        assertTrue(VibeCodeApiClient.isWeakModel("gpt-4o-mini"))
        assertTrue(VibeCodeApiClient.isWeakModel("claude-3-5-haiku"))
        assertTrue(VibeCodeApiClient.isWeakModel("vibe-nano"))
        assertFalse(VibeCodeApiClient.isWeakModel("gpt-4o"))

        // Уменьшение max_tokens на 20% для слабых моделей
        assertEquals(3200, VibeCodeApiClient.effectiveMaxTokens(4000, "gemini-2.0-flash"))
        assertEquals(4000, VibeCodeApiClient.effectiveMaxTokens(4000, "gpt-4o"))

        // Температура 0.85 для слабых моделей
        assertEquals(0.85, VibeCodeApiClient.effectiveTemperature(0.6, "gemini-2.0-flash"), 0.001)
        assertEquals(0.6, VibeCodeApiClient.effectiveTemperature(0.6, "gpt-4o"), 0.001)

        // Коэффициенты retry при finish_reason=length
        assertEquals(listOf(1.0, 0.7, 0.5, 0.3), VibeCodeApiClient.LENGTH_RETRY_FACTORS)

        // HTTP ошибки
        assertEquals("Неверный API-ключ", VibeCodeApiClient.mapHttpError(401, "Unauthorized"))
        assertEquals("Доступ запрещён", VibeCodeApiClient.mapHttpError(403, "Forbidden"))
        assertEquals("Модель или endpoint не найдены", VibeCodeApiClient.mapHttpError(404, "Not Found"))
        assertEquals("Превышен лимит, повторите через минуту", VibeCodeApiClient.mapHttpError(429, "Too Many"))
        assertEquals("Ошибка на стороне LLM", VibeCodeApiClient.mapHttpError(502, "Bad Gateway"))
    }

    @Test
    fun testCoursework40PagesPlannedInto12Stages() {
        val stages = StagePlanner.planStages("kursovaya", 40)
        // 1 введение + 9 подразделов (3 главы × 3 подраздела) + 1 заключение + 1 список литературы = 12 этапов
        assertEquals(12, stages.size)
        assertEquals("intro", stages.first().section)
        assertEquals("Глава 1.1", stages[1].label)
        assertEquals("chapter_1_1", stages[1].section)
        assertEquals(StageType.SUBSECTION, stages[1].type)
        assertTrue(stages[1].maxTokens <= 4000)
        assertEquals("Глава 3.3", stages[9].label)
        assertEquals("conclusion", stages[10].section)
        assertEquals("references", stages[11].section)
    }

    @Test
    fun testContextCompressorUpdateSummaryUnder2000Chars() {
        val shortSummary = ContextCompressor.updateSummary("Введение.", "Параграф 1.1.")
        assertTrue(shortSummary.length < 2000)

        val longPrev = "А".repeat(1500)
        val longNew = "Б".repeat(1500)
        val compressed = ContextCompressor.updateSummary(longPrev, longNew)
        assertTrue("Compressed summary must be < 2000 chars, was ${compressed.length}", compressed.length < 2000)
        assertTrue(compressed.contains("…"))
    }

    @Test
    fun testSubsectionSingleStageJsonParsing() {
        val stage = Stage(
            id = "chapter_2_1",
            label = "Глава 2.1",
            section = "chapter_2_1",
            type = StageType.SUBSECTION,
            chapterIndex = 2,
            subsectionIndex = 1,
            maxTokens = 3000
        )
        val rawJson = """
            {
              "section": "chapter_2_1",
              "title": "2.1. Анализ показателей эффективности",
              "level": 2,
              "anchor": "h_2_1",
              "paragraphs": [
                "Первый абзац подраздела 2.1 с подробным анализом.",
                "Второй абзац подраздела 2.1 с расчетами.",
                "Третий абзац подраздела 2.1 с выводами."
              ]
            }
        """.trimIndent()

        val result = StageResponseParser.parse(stage, rawJson)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        // Должен содержать заголовок главы 2 (т.к. subsectionIndex == 1) + сам подраздел 2.1
        assertEquals(2, content.works.size)
        assertEquals(1, content.works[0].level)
        assertEquals(2, content.works[1].level)
        assertEquals("2.1. Анализ показателей эффективности", content.works[1].title)
        assertEquals(3, content.works[1].paragraphs.size)
    }

    @Test
    fun testWorkMetaDefaults() {
        val meta = WorkMeta(
            topic = "Тестовая тема исследования",
            type = WorkType.COURSEWORK,
            targetPages = 35
        )
        assertEquals(35, meta.targetPages)
        assertEquals(WorkType.COURSEWORK, meta.type)
        assertTrue(meta.university.isNotBlank())
    }
}
