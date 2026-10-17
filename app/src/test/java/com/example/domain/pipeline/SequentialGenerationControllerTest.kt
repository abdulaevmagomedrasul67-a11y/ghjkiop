package com.example.domain.pipeline

import com.example.domain.model.LlmSettings
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import com.example.domain.repository.LlmRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SequentialGenerationControllerTest {

    @Test
    fun testSequentialPipelineIteratesSectionBySectionWithSummaryRelay() = runTest {
        val promptLogs = mutableListOf<String>()

        val fakeLlmRepository = object : LlmRepository {
            override suspend fun generateCompletion(
                systemPrompt: String,
                userPrompt: String,
                settings: LlmSettings
            ): Result<String> {
                promptLogs.add(userPrompt)

                // Если это запрос на саммари из 3 предложений
                if (userPrompt.contains("Сформулируйте ровно ТРИ")) {
                    return Result.success("Первый тезис резюме. Второй аналитический результат. Третье направление для следующей главы.")
                }

                // Обычный раздел работы
                return Result.success("Научный академический текст раздела. Важно отметить, что методология апробирована. Экспериментальные данные подтверждают гипотезу.")
            }
        }

        val controller = SequentialGenerationController(fakeLlmRepository)
        val meta = WorkMeta(
            topic = "Интеллектуальные алгоритмы анализа временных рядов",
            type = WorkType.COURSEWORK,
            targetPages = 30
        )

        val result = controller.generateWork(meta, LlmSettings())
        assertTrue("Генерация должна завершиться успешно", result.isSuccess)

        val doc = result.getOrNull()
        assertNotNull(doc)

        // Проверяем, что все разделы наполнены текстом
        val textSections = doc!!.sections.filter { it.targetCharacters > 0 }
        for (sec in textSections) {
            assertTrue("Раздел ${sec.title} должен быть сгенерирован", sec.isGenerated)
            assertTrue("Раздел ${sec.title} должен содержать текст", sec.content.isNotBlank())
            // Проверяем удаление запрещенных клише
            assertTrue("Клише 'Важно отметить' должно быть вырезано", !sec.content.contains("Важно отметить"))
        }

        // Проверяем, что эстафета резюме (Summary Relay) передавалась:
        // Первый сгенерированный раздел (Введение) НЕ должен содержать блок "КРАТКИЙ КОНТЕКСТ"
        val firstSectionPrompt = promptLogs.first { it.contains("РАЗДЕЛ ДЛЯ ГЕНЕРАЦИИ: «ВВЕДЕНИЕ»") }
        assertTrue(!firstSectionPrompt.contains("КРАТКИЙ КОНТЕКСТ ПРЕДЫДУЩИХ РАЗДЕЛОВ"))

        // Последующие разделы ДОЛЖНЫ содержать блок "КРАТКИЙ КОНТЕКСТ ПРЕДЫДУЩИХ РАЗДЕЛОВ" с 3 предложениями
        val laterSectionPrompts = promptLogs.filter {
            it.contains("РАЗДЕЛ ДЛЯ ГЕНЕРАЦИИ:") && !it.contains("РАЗДЕЛ ДЛЯ ГЕНЕРАЦИИ: «ВВЕДЕНИЕ»")
        }
        assertTrue("Должны быть последующие разделы", laterSectionPrompts.isNotEmpty())

        for (prompt in laterSectionPrompts) {
            assertTrue(
                "Последующий раздел должен содержать Summary Relay вместо сырого текста",
                prompt.contains("КРАТКИЙ КОНТЕКСТ ПРЕДЫДУЩИХ РАЗДЕЛОВ")
            )
            assertTrue(
                "Должно передаваться 3-предложенческое резюме",
                prompt.contains("Первый тезис резюме. Второй аналитический результат. Третье направление для следующей главы.")
            )
        }
    }
}
