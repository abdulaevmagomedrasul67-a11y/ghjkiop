package com.example.domain.quantization

import com.example.domain.model.SectionType
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkSection
import com.example.domain.model.WorkType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Квантователь академического объема работы по стандартам ФГОС / ГОСТ.
 *
 * Базовый академический норматив:
 * 1 страница формата А4 (шрифт 14pt Times New Roman, полуторный интервал,
 * поля 30/10/20/20 мм) содержит ровно ~1800 символов с пробелами.
 */
object SectionQuantizer {

    const val CHARS_PER_PAGE: Int = 1800

    data class QuantizedPlan(
        val totalPages: Int,
        val totalTargetCharacters: Int,
        val sections: List<WorkSection>
    )

    fun quantize(meta: WorkMeta): QuantizedPlan {
        val totalPages = max(meta.type.minPages, minOf(meta.targetPages, meta.type.maxPages))
        val totalChars = totalPages * CHARS_PER_PAGE

        // Структурные страницы (титул + оглавление не требуют отдельной квоты генерации текста)
        val titleAndTocPages = 2
        val contentPages = max(5, totalPages - titleAndTocPages)

        // Пропорциональное академическое распределение
        val introShare: Double
        val conclusionShare: Double
        val biblioShare: Double
        val numChapters: Int
        val paragraphsPerChapter: Int

        when (meta.type) {
            WorkType.ESSAY_REFERAT -> {
                introShare = 0.08
                conclusionShare = 0.06
                biblioShare = 0.04
                numChapters = 2
                paragraphsPerChapter = 2
            }
            WorkType.SCIENTIFIC_ARTICLE -> {
                introShare = 0.12
                conclusionShare = 0.10
                biblioShare = 0.08
                numChapters = 1
                paragraphsPerChapter = 3
            }
            WorkType.DIPLOMA_VKR, WorkType.MASTER_DISSERTATION -> {
                introShare = 0.06
                conclusionShare = 0.05
                biblioShare = 0.04
                numChapters = 3
                paragraphsPerChapter = 3
            }
            WorkType.COURSEWORK -> {
                introShare = 0.08
                conclusionShare = 0.06
                biblioShare = 0.04
                numChapters = 2
                paragraphsPerChapter = 3
            }
        }

        val introPages = max(1.5, (contentPages * introShare).roundToOneDecimal())
        val conclusionPages = max(1.5, (contentPages * conclusionShare).roundToOneDecimal())
        val biblioPages = max(1.0, (contentPages * biblioShare).roundToOneDecimal())
        val bodyPages = max(2.0, contentPages - introPages - conclusionPages - biblioPages)

        val sections = mutableListOf<WorkSection>()

        // 1. ВВЕДЕНИЕ
        val introChars = (introPages * CHARS_PER_PAGE).roundToInt()
        sections.add(
            WorkSection(
                id = "sec_intro",
                title = "ВВЕДЕНИЕ",
                type = SectionType.INTRODUCTION,
                targetPages = introPages,
                targetCharacters = introChars,
                minCharacters = (introChars * 0.90).roundToInt(),
                maxCharacters = (introChars * 1.10).roundToInt()
            )
        )

        // 2. ГЛАВЫ И ПОДРАЗДЕЛЫ (ОСНОВНАЯ ЧАСТЬ)
        val chapterShareWeights = when (numChapters) {
            1 -> listOf(1.0)
            2 -> listOf(0.45, 0.55) // Глава 1 Теория (45%), Глава 2 Практика/Анализ (55%)
            else -> listOf(0.33, 0.37, 0.30) // Глава 1 Теория, Глава 2 Анализ, Глава 3 Рекомендации
        }

        for (chIndex in 0 until numChapters) {
            val chNumber = chIndex + 1
            val chWeight = chapterShareWeights[chIndex]
            val chPages = bodyPages * chWeight
            val chTitle = generateChapterTitle(meta.topic, chNumber, meta.type)

            // Добавляем заголовок главы
            sections.add(
                WorkSection(
                    id = "sec_ch_$chNumber",
                    title = "ГЛАВА $chNumber. $chTitle",
                    type = SectionType.CHAPTER,
                    chapterNumber = chNumber,
                    targetPages = 0.0,
                    targetCharacters = 0,
                    minCharacters = 0,
                    maxCharacters = 0
                )
            )

            // Подразделы / параграфы
            val subPages = (chPages / paragraphsPerChapter).roundToOneDecimal()
            val subChars = (subPages * CHARS_PER_PAGE).roundToInt()

            for (pIndex in 0 until paragraphsPerChapter) {
                val pNumber = pIndex + 1
                val subTitle = generateSubsectionTitle(meta.topic, chNumber, pNumber)

                sections.add(
                    WorkSection(
                        id = "sec_ch_${chNumber}_sub_$pNumber",
                        title = "$chNumber.$pNumber. $subTitle",
                        type = SectionType.SUBSECTION,
                        chapterNumber = chNumber,
                        subsectionNumber = pNumber,
                        targetPages = subPages,
                        targetCharacters = subChars,
                        minCharacters = (subChars * 0.90).roundToInt(),
                        maxCharacters = (subChars * 1.10).roundToInt()
                    )
                )
            }
        }

        // 3. ЗАКЛЮЧЕНИЕ
        val conclusionChars = (conclusionPages * CHARS_PER_PAGE).roundToInt()
        sections.add(
            WorkSection(
                id = "sec_conclusion",
                title = "ЗАКЛЮЧЕНИЕ",
                type = SectionType.CONCLUSION,
                targetPages = conclusionPages,
                targetCharacters = conclusionChars,
                minCharacters = (conclusionChars * 0.90).roundToInt(),
                maxCharacters = (conclusionChars * 1.10).roundToInt()
            )
        )

        // 4. СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ
        val biblioChars = (biblioPages * CHARS_PER_PAGE).roundToInt()
        sections.add(
            WorkSection(
                id = "sec_biblio",
                title = "СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ",
                type = SectionType.BIBLIOGRAPHY,
                targetPages = biblioPages,
                targetCharacters = biblioChars,
                minCharacters = (biblioChars * 0.85).roundToInt(),
                maxCharacters = (biblioChars * 1.15).roundToInt()
            )
        )

        return QuantizedPlan(
            totalPages = totalPages,
            totalTargetCharacters = totalChars,
            sections = sections
        )
    }

    private fun Double.roundToOneDecimal(): Double =
        (this * 10.0).roundToInt() / 10.0

    private fun generateChapterTitle(topic: String, chapterNum: Int, type: WorkType): String = when (chapterNum) {
        1 -> "Теоретико-методологические основы исследования «$topic»"
        2 -> "Анализ современного состояния и практические аспекты «$topic»"
        3 -> "Разработка научно обоснованных рекомендаций и предложений по совершенствованию «$topic»"
        else -> "Исследование прикладных аспектов темы"
    }

    private fun generateSubsectionTitle(topic: String, chapterNum: Int, subNum: Int): String = when (chapterNum) {
        1 -> when (subNum) {
            1 -> "Понятие, сущность и ключевые характеристики предметной области"
            2 -> "Эволюция подходов и нормативно-правовое регулирование"
            else -> "Сравнительный анализ отечественного и зарубежного опыта"
        }
        2 -> when (subNum) {
            1 -> "Организационно-экономическая и технологическая характеристика объекта"
            2 -> "Комплексная оценка факторов и текущих показателей эффективности"
            else -> "Выявление проблемных зон, узких мест и системных ограничений"
        }
        3 -> when (subNum) {
            1 -> "Концептуальные направления модернизации и устранения рисков"
            2 -> "Алгоритм внедрения практических мероприятий и дорожная карта"
            else -> "Оценка социально-экономической и технической эффективности решений"
        }
        else -> "Специальные аспекты исследования"
    }
}
