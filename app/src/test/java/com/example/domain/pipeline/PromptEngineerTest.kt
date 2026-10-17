package com.example.domain.pipeline

import com.example.domain.model.SectionType
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkSection
import com.example.domain.model.WorkType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptEngineerTest {

    @Test
    fun testSanitizeRemovesBannedClichés() {
        val rawAiText = "Важно отметить, что исследуемый процесс сложен. Таким образом, мы получаем результат. В заключение следует сказать, что система эффективна."
        val sanitized = PromptEngineer.sanitizeAndHumanize(rawAiText)

        assertFalse(sanitized.contains("Важно отметить", ignoreCase = true))
        assertFalse(sanitized.contains("Таким образом", ignoreCase = true))
        assertFalse(sanitized.contains("В заключение следует сказать", ignoreCase = true))
        assertTrue(sanitized.contains("Следовательно", ignoreCase = true))
    }

    @Test
    fun testSystemPromptDemandsBurstinessAndPerplexity() {
        val systemPrompt = PromptEngineer.buildSystemPrompt()
        assertTrue("Должно быть требование к Burstiness", systemPrompt.contains("Burstiness", ignoreCase = true))
        assertTrue("Должно быть требование к Perplexity", systemPrompt.contains("Perplexity", ignoreCase = true))
        assertTrue("Должен быть запрет клише", systemPrompt.contains("КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО", ignoreCase = true))
    }

    @Test
    fun testSectionPromptIncludesSummaryRelayWhenPresent() {
        val meta = WorkMeta(topic = "ИИ в медицине", type = WorkType.COURSEWORK, targetPages = 35)
        val section = WorkSection(
            id = "sec_1",
            title = "1.1. Сущность технологии",
            type = SectionType.SUBSECTION,
            targetPages = 4.0,
            targetCharacters = 7200,
            minCharacters = 6480,
            maxCharacters = 7920
        )

        // Без эстафетного резюме
        val promptWithoutSummary = PromptEngineer.buildSectionPrompt(meta, section, null)
        assertFalse(promptWithoutSummary.contains("КРАТКИЙ КОНТЕКСТ ПРЕДЫДУЩИХ РАЗДЕЛОВ"))

        // С эстафетным резюме (Summary Relay)
        val summary = "Первое предложение о теории. Второе предложение о методах. Третье предложение о направлении анализа."
        val promptWithSummary = PromptEngineer.buildSectionPrompt(meta, section, summary)
        assertTrue(promptWithSummary.contains("КРАТКИЙ КОНТЕКСТ ПРЕДЫДУЩИХ РАЗДЕЛОВ"))
        assertTrue(promptWithSummary.contains(summary))
    }
}
