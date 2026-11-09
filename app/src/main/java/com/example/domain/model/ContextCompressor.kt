package com.example.domain.model

/**
 * Интеллектуальный компрессор контекста для последовательного конвейера генерации (Sequential Generation Pipeline).
 */
object ContextCompressor {

    /**
     * 4.3. Сжатие контекста между этапами, чтобы не превышать ~2000 символов.
     */
    fun updateSummary(previous: String, newContent: String): String {
        val combined = "$previous\n$newContent".trim()
        if (combined.length < 2000) return combined

        // Берём первые 800 и последние 800 символов
        val firstPart = combined.take(800)
        val lastPart = combined.takeLast(800)
        return "$firstPart\n…\n$lastPart"
    }

    /**
     * Создает плотную академическую выжимку ровно из 3 предложений по всем предшествующим разделам.
     */
    fun createThreeSentenceSummary(
        previousWorks: List<PartialWork>,
        topic: String,
        subject: String,
        nextStageLabel: String
    ): String {
        if (previousWorks.isEmpty()) {
            return ""
        }

        val introWork = previousWorks.firstOrNull { it.section == "intro" || it.level == 1 }
        val introSentence = buildIntroSentence(introWork, topic, subject)

        val chapterWorks = previousWorks.filter { it.section != "intro" && it.section != "references" }
        val chaptersSentence = buildChaptersSentence(chapterWorks, topic)

        val bridgeSentence = buildBridgeSentence(nextStageLabel, topic)

        val summary = "$introSentence $chaptersSentence $bridgeSentence".trim()
        return if (summary.length >= 2000) {
            updateSummary("", summary)
        } else {
            summary
        }
    }

    private fun buildIntroSentence(intro: PartialWork?, topic: String, subject: String): String {
        val firstPara = intro?.paragraphs?.firstOrNull { it.length > 50 }
        return if (firstPara != null) {
            val sentence = extractFirstCleanSentence(firstPara)
            if (sentence.length in 40..250 && !sentence.contains("важно", ignoreCase = true)) {
                sentence
            } else {
                "В исследовании по направлению «$subject» концептуализирована предметная область темы «$topic» и определены базовые методологические рамки научного поиска."
            }
        } else {
            "В вводной части исследования по теме «$topic» определены объект, предмет, цель и теоретико-методологическая основа изыскания."
        }
    }

    private fun buildChaptersSentence(chapters: List<PartialWork>, topic: String): String {
        if (chapters.isEmpty()) {
            return "Сформирован исходный понятийно-категориальный аппарат и структурированы ключевые теоретические предпосылки исследования."
        }

        val chapterTitles = chapters.map { it.title.replace(Regex("^\\d+(\\.\\d+)?\\.?\\s*"), "").trim() }
        val lastChapter = chapters.lastOrNull()
        val lastSummaryPara = lastChapter?.paragraphs?.lastOrNull { it.length > 50 }

        if (lastSummaryPara != null) {
            val sentence = extractFirstCleanSentence(lastSummaryPara)
            if (sentence.length in 40..250 && !sentence.contains("важно", ignoreCase = true) && !sentence.contains("таким образом", ignoreCase = true)) {
                return sentence
            }
        }

        val titlesJoined = chapterTitles.takeLast(3).joinToString(", ") { "«$it»" }
        return "В рамках предшествующих разделов ($titlesJoined) систематизированы концептуальные модели, классифицированы ключевые факторы и эмпирически обоснованы закономерности изучаемого процесса."
    }

    private fun buildBridgeSentence(nextStageLabel: String, topic: String): String {
        val cleanLabel = nextStageLabel.replace(Regex("^Глава \\d+(\\.\\d+)?\\.?\\s*"), "").trim()
        return when {
            nextStageLabel.contains("заключен", ignoreCase = true) ->
                "Полученные результаты сформировали доказательную аналитическую базу для итогового синтеза положений исследования, обобщения выводов и выработки практических рекомендаций."
            nextStageLabel.contains("литератур", ignoreCase = true) ->
                "Совокупность цитируемых научных концепций и источников нормативной базы подлежит строгой библиографической систематизации по правилам ГОСТ."
            else ->
                "Достигнутые промежуточные выводы обеспечивают непрерывность исследовательской аргументации и служат базисом для непосредственного раскрытия раздела «$cleanLabel»."
        }
    }

    private fun extractFirstCleanSentence(paragraph: String): String {
        val clean = paragraph.replace(Regex("^\\[\\d+\\]\\s*"), "").trim()
        val match = Regex("^([А-ЯЁA-Z][^.!?]+[.!?])").find(clean)
        val s = match?.value?.trim() ?: clean.take(150)
        return if (s.endsWith(".") || s.endsWith("!") || s.endsWith("?")) s else "$s."
    }
}
