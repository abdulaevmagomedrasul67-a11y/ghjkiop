package com.example.domain.usecase

import com.example.data.remote.parser.StageResponseParser
import com.example.data.remote.prompt.StagePromptBuilder
import com.example.domain.model.AcademicVolumeQuantizer
import com.example.domain.model.ContextCompressor
import com.example.domain.model.GostProfile
import com.example.domain.model.PartialWork
import com.example.domain.model.Stage
import com.example.domain.model.StagePlanner
import com.example.domain.model.StageResult
import com.example.domain.model.StageType
import com.example.domain.model.WorkRequest
import com.example.domain.model.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StagePlannerAndChunkedTest {

    @Test
    fun `test StagePlanner for Referat generates 5 separate stages`() {
        val stages = StagePlanner.plan(WorkType.REFERAT, 15)
        assertEquals(5, stages.size)
        assertEquals("Введение и план работы", stages[0].label)
        assertEquals(StageType.INTRO_AND_TOC, stages[0].type)
        assertEquals("Глава 1", stages[1].label)
        assertEquals(StageType.CHAPTER, stages[1].type)
        assertEquals(1, stages[1].chapterIndex)
        assertEquals("Глава 2", stages[2].label)
        assertEquals(StageType.CHAPTER, stages[2].type)
        assertEquals(2, stages[2].chapterIndex)
        assertEquals("Заключение", stages[3].label)
        assertEquals(StageType.CONCLUSION, stages[3].type)
        assertEquals("Список литературы", stages[4].label)
        assertEquals(StageType.REFERENCES, stages[4].type)
    }

    @Test
    fun `test StagePlanner for Doklad generates 1 stage`() {
        val stages = StagePlanner.plan(WorkType.REPORT, 10)
        assertEquals(1, stages.size)
        assertEquals(StageType.ALL_IN_ONE, stages[0].type)
        assertEquals("Доклад целиком", stages[0].label)
    }

    @Test
    fun `test StagePlanner for Kursovaya 30 pages generates 12 granular stages`() {
        val stages = StagePlanner.plan(WorkType.COURSEWORK, 30)
        assertEquals(12, stages.size)
        assertEquals("Введение", stages[0].label)
        assertEquals(StageType.INTRO_AND_TOC, stages[0].type)
        assertEquals("Глава 1.1", stages[1].label)
        assertEquals(StageType.SUBSECTION, stages[1].type)
        assertEquals("Глава 2.1", stages[4].label)
        assertEquals(StageType.SUBSECTION, stages[4].type)
        assertEquals("Глава 3.3", stages[9].label)
        assertEquals(StageType.SUBSECTION, stages[9].type)
        assertEquals("Заключение", stages[10].label)
        assertEquals(StageType.CONCLUSION, stages[10].type)
        assertEquals("Список литературы", stages[11].label)
        assertEquals(StageType.REFERENCES, stages[11].type)
    }

    @Test
    fun `test StagePlanner for Essay generates 1 stage`() {
        val stages = StagePlanner.plan(WorkType.ESSAY, 8)
        assertEquals(1, stages.size)
        assertEquals(StageType.ALL_IN_ONE, stages[0].type)
    }

    @Test
    fun `test StagePlanner for Kontrolnaya generates 4 stages`() {
        val stages = StagePlanner.plan(WorkType.CONTROL_WORK, 12)
        assertEquals(4, stages.size)
        assertEquals("Введение и постановка задач", stages[0].label)
        assertEquals("Основная часть", stages[1].label)
        assertEquals("Заключение", stages[2].label)
        assertEquals("Список литературы", stages[3].label)
    }

    @Test
    fun `test StageResponseParser parses Intro stage with TOC`() {
        val stage = Stage(id = "intro", label = "Введение", type = StageType.INTRO_AND_TOC)
        val json = """
            {
              "section": "intro",
              "title": "Введение",
              "level": 1,
              "anchor": "h_intro",
              "paragraphs": ["Абзац актуальности", "Абзац целей и задач"],
              "toc": [
                {"level": 1, "title": "Введение", "anchor": "h_intro"},
                {"level": 1, "title": "1. Теоретические основы", "anchor": "h_ch1"},
                {"level": 2, "title": "1.1. Понятие", "anchor": "h_ch1_1"},
                {"level": 1, "title": "Заключение", "anchor": "h_concl"},
                {"level": 1, "title": "Список использованных источников", "anchor": "h_refs"}
              ]
            }
        """.trimIndent()

        val result = StageResponseParser.parse(stage, json)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        assertEquals(1, content.works.size)
        assertEquals("Введение", content.works[0].title)
        assertEquals(2, content.works[0].paragraphs.size)
        assertNotNull(content.toc)
        assertEquals(5, content.toc?.size)
    }

    @Test
    fun `test StageResponseParser parses Chapter stage with Subsections`() {
        val stage = Stage(id = "chapter_1", label = "Глава 1", type = StageType.CHAPTER, chapterIndex = 1)
        val json = """
            {
              "section": "chapter_1",
              "title": "Глава 1. Теоретический анализ",
              "level": 1,
              "anchor": "h_ch1",
              "paragraphs": ["Вводный абзац главы"],
              "subsections": [
                {
                  "title": "1.1. Сущность процесса",
                  "level": 2,
                  "anchor": "h_ch1_1",
                  "paragraphs": ["Параграф 1", "Параграф 2"]
                },
                {
                  "title": "1.2. Нормативная база",
                  "level": 2,
                  "anchor": "h_ch1_2",
                  "paragraphs": ["Параграф 3"]
                }
              ]
            }
        """.trimIndent()

        val result = StageResponseParser.parse(stage, json)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        assertEquals(3, content.works.size)
        assertEquals("Глава 1. Теоретический анализ", content.works[0].title)
        assertEquals(1, content.works[0].level)
        assertEquals("1.1. Сущность процесса", content.works[1].title)
        assertEquals(2, content.works[1].level)
        assertEquals("1.2. Нормативная база", content.works[2].title)
        assertEquals(2, content.works[2].level)
    }

    @Test
    fun `test StageResponseParser parses separate Conclusion stage`() {
        val stage = Stage(id = "conclusion", label = "Заключение", type = StageType.CONCLUSION)
        val json = """
            {
              "section": "conclusion",
              "title": "Заключение",
              "level": 1,
              "anchor": "h_conclusion",
              "paragraphs": [
                "Итоги по первой задаче...",
                "Итоги по второй задаче...",
                "Общий вывод и практическая ценность..."
              ]
            }
        """.trimIndent()

        val result = StageResponseParser.parse(stage, json)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        assertEquals(1, content.works.size)
        assertEquals("Заключение", content.works[0].title)
        assertEquals(3, content.works[0].paragraphs.size)
    }

    @Test
    fun `test StageResponseParser parses separate References stage`() {
        val stage = Stage(id = "references", label = "Список литературы", type = StageType.REFERENCES)
        val json = """
            {
              "section": "references",
              "title": "Список использованных источников",
              "level": 1,
              "anchor": "h_references",
              "references": [
                "1. Иванов И.И. Теория систем. — М.: Наука, 2022. — 200 с.",
                "2. Петров П.П. Практика анализа // Вестник ВУЗа. — 2023. — № 2. — С. 10–20.",
                "3. Федеральный закон от 27.07.2006 № 149-ФЗ."
              ]
            }
        """.trimIndent()

        val result = StageResponseParser.parse(stage, json)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        assertEquals(1, content.works.size)
        assertEquals("references", content.works[0].section)
        assertEquals(3, content.references?.size)
        assertEquals("3. Федеральный закон от 27.07.2006 № 149-ФЗ.", content.references?.get(2))
    }

    @Test
    fun `test StagePromptBuilder generates prompts for Conclusion and References`() {
        val request = WorkRequest(
            type = WorkType.COURSEWORK,
            topic = "Информационная безопасность банковских систем",
            subject = "Информационная безопасность",
            targetPages = 30,
            studentName = "Сидоров А.В."
        )

        val stageConclusion = Stage(id = "conclusion", label = "Заключение", type = StageType.CONCLUSION)
        val promptConclusion = StagePromptBuilder.buildPrompt(stageConclusion, request, emptyList(), emptyList(), GostProfile.DEFAULT_GOST)
        assertTrue(promptConclusion.contains("ЗАКЛЮЧЕНИЕ"))
        assertTrue(promptConclusion.contains("НЕ включай список литературы"))

        val stageReferences = Stage(id = "references", label = "Список литературы", type = StageType.REFERENCES, maxReferences = 15)
        val promptReferences = StagePromptBuilder.buildPrompt(stageReferences, request, emptyList(), emptyList(), GostProfile.DEFAULT_GOST)
        assertTrue(promptReferences.contains("СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ"))
        assertTrue(promptReferences.contains("15"))
    }

    @Test
    fun `test StageResponseParser rescues raw numbered text for references without JSON`() {
        val stage = Stage(id = "references", label = "Список литературы", type = StageType.REFERENCES)
        val rawNumberedText = """
            Вот сформированный список литературы по теме:
            1. Кузнецов Н.М. Информационная безопасность: учебник. — М.: Юрайт, 2022. — 250 с.
            2. Смирнов А.В. Методы криптографической защиты данных. — СПб.: Питер, 2021. — 310 с.
            3. Федеральный закон "О персональных данных" от 27.07.2006 N 152-ФЗ.
            4. Васильев Д.Ю. Анализ угроз банковских систем // Вопросы безопасности. — 2023. — № 3. — С. 15–24.
            5. Попов С.А. Практика применения стандартов ГОСТ. — Казань: КФУ, 2022. — 180 с.
        """.trimIndent()

        val result = StageResponseParser.parse(stage, rawNumberedText)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        assertEquals(5, content.references?.size)
        assertTrue(content.references?.first()?.contains("Кузнецов") == true)
    }

    @Test
    fun `test StageResponseParser handles top-level array and alternate literature key`() {
        val stage = Stage(id = "references", label = "Список литературы", type = StageType.REFERENCES)
        val jsonWithLiteratureKey = """
            {
              "title": "Литература",
              "literature": [
                "1. Источник один. — М., 2021.",
                "2. Источник два. — СПб., 2022."
              ]
            }
        """.trimIndent()

        val result = StageResponseParser.parse(stage, jsonWithLiteratureKey)
        assertTrue(result is StageResult.Content)
        val content = result as StageResult.Content
        assertEquals(2, content.references?.size)
    }

    @Test
    fun `test AcademicVolumeQuantizer for 45 pages coursework calculates proportional characters and pages`() {
        val dist = AcademicVolumeQuantizer.distributePages(WorkType.COURSEWORK, 45)
        assertEquals(45, dist.totalPages)
        assertEquals(4, dist.introPages)
        assertEquals(4, dist.conclusionPages)
        assertEquals(3, dist.chapterPages.size)
        val totalChapterPages = dist.chapterPages.sum()
        assertTrue(totalChapterPages >= 30)
        dist.chapterPages.forEach { chPages ->
            assertTrue(chPages in 10..13)
            val q = AcademicVolumeQuantizer.quantizeSection(chPages)
            assertEquals(chPages * 1800, q.chars)
            assertEquals(chPages * 260, q.words)
            assertTrue(q.maxTokens >= 6000)
        }
    }

    @Test
    fun `test AcademicVolumeQuantizer for 80 pages coursework generates 4 chapters with balanced volume`() {
        val dist = AcademicVolumeQuantizer.distributePages(WorkType.COURSEWORK, 80)
        assertEquals(80, dist.totalPages)
        assertEquals(6, dist.introPages)
        assertEquals(5, dist.conclusionPages)
        assertEquals(4, dist.chapterPages.size)
        dist.chapterPages.forEach { chPages ->
            assertTrue("Chapter pages should be at least 15 for 80 pages work", chPages >= 15)
            val q = AcademicVolumeQuantizer.quantizeSection(chPages)
            assertEquals(chPages * 1800, q.chars)
        }
        val stages = StagePlanner.plan(WorkType.COURSEWORK, 80)
        assertEquals(12, stages.size) // 12 granular stages for long coursework
        assertTrue(stages[0].targetChars >= 4 * 1800)
    }

    @Test
    fun `test ContextCompressor produces dense 3-sentence summary without filler phrases`() {
        val introWork = PartialWork(
            section = "intro",
            title = "Введение",
            level = 1,
            anchor = "h_intro",
            paragraphs = listOf(
                "Исследование концептуальных оснований безопасности распределенных реестров базируется на комплексном анализе криптографических примитивов и сетевых протоколов.",
                "Целью работы выступает формализация методов защиты транзакционных узлов."
            )
        )
        val ch1Work = PartialWork(
            section = "chapter_1",
            title = "Глава 1. Теоретический базис",
            level = 1,
            anchor = "h_ch1",
            paragraphs = listOf(
                "Классификация угроз консенсуса выявила ключевые уязвимости алгоритмов византийской устойчивости."
            )
        )

        val summary = ContextCompressor.createThreeSentenceSummary(
            previousWorks = listOf(introWork, ch1Work),
            topic = "Безопасность распределенных реестров",
            subject = "Информационная безопасность",
            nextStageLabel = "Глава 2"
        )

        assertNotNull(summary)
        assertTrue(summary.isNotBlank())

        val sentenceCount = summary.split(Regex("[.!?]\\s+")).filter { it.isNotBlank() }.size
        assertEquals(3, sentenceCount)

        assertFalse(summary.contains("Важно отметить", ignoreCase = true))
        assertFalse(summary.contains("Таким образом", ignoreCase = true))
        assertFalse(summary.contains("В заключение", ignoreCase = true))
    }

    @Test
    fun `test StagePromptBuilder includes burstiness, anti-filler prohibitions and exact volume targets`() {
        val request = WorkRequest(
            type = WorkType.COURSEWORK,
            topic = "Анализ финансовых рисков банковских структур",
            subject = "Финансы и кредит",
            targetPages = 45
        )
        val stage = Stage(
            id = "chapter_1",
            label = "Глава 1",
            type = StageType.CHAPTER,
            chapterIndex = 1,
            targetPages = 11,
            targetChars = 19800,
            targetWords = 2860,
            maxTokens = 8000
        )

        val prompt = StagePromptBuilder.buildPrompt(
            stage = stage,
            request = request,
            previousWorks = emptyList(),
            toc = emptyList(),
            profile = GostProfile.DEFAULT_GOST,
            contextSummary = "Вводный контекст исследования. Раскрыта теоретическая база. Переход к анализу."
        )

        assertTrue(prompt.contains("11 страниц"))
        assertTrue(prompt.contains("19800 символов"))
        assertTrue(prompt.contains("КОНТЕКСТНАЯ ВЫЖИМКА ПРЕДЫДУЩИХ РАЗДЕЛОВ"))
        assertTrue(prompt.contains("Вводный контекст исследования"))

        val sysPrompt = StagePromptBuilder.COMMON_STAGE_SYSTEM_PROMPT
        assertTrue(sysPrompt.contains("BURSTINESS"))
        assertTrue(sysPrompt.contains("PERPLEXITY"))
        assertTrue(sysPrompt.contains("СТРОЖАЙШИЙ ЗАПРЕТ ИИ-ФРАЗ-ФИЛЛЕРОВ"))
        assertTrue(sysPrompt.contains("1800 символов на страницу"))
    }
}
