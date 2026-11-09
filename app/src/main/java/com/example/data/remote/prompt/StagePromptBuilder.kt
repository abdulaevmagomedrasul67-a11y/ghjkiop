package com.example.data.remote.prompt

import com.example.domain.model.ContextCompressor
import com.example.domain.model.GostProfile
import com.example.domain.model.PartialWork
import com.example.domain.model.Stage
import com.example.domain.model.StageType
import com.example.domain.model.TocItem
import com.example.domain.model.WorkRequest

object StagePromptBuilder {

    const val COMMON_STAGE_SYSTEM_PROMPT = """
Ты — ведущий научный сотрудник, университетский профессор и академический редактор высшей квалификации.
Ты участвуешь в последовательном конвейере генерации академической работы для студента (Sequential Generation Pipeline).
На каждом этапе ты генерируешь СТРОГО одну указанную часть работы.

СТРОГИЕ ПРАВИЛА И ТРЕБОВАНИЯ К ТЕКСТУ:
1. АКАДЕМИЧЕСКИЙ СТИЛЬ И ЧЕЛОВЕЧНОСТЬ (BURSTINESS И PERPLEXITY ДЛЯ ОБХОДА ДЕТЕКТОРОВ ИИ):
   - Чередование длины предложений (Burstiness): Категорически запрещено писать однообразными фразами одинакового размера. Сочетай лаконичные, энергичные тезисные предложения (6-10 слов) с развернутыми сложными академическими периодами (25-45 слов), включающими причастные и деепричастные обороты, тире, двоеточия и аналитические противопоставления.
   - Богатство языка и связность (Perplexity): Используй специализированную терминологию предметной области, разнообразные логические переходы мысли («в контексте концепции...», «применительно к механизму...», «дифференциация подходов демонстрирует...»).

2. СТРОЖАЙШИЙ ЗАПРЕТ ИИ-ФРАЗ-ФИЛЛЕРОВ:
   - Категорически ЗАПРЕЩЕНО использовать типичные паразитные маркеры и клише генеративных моделей:
     * «Важно отметить» / «Следует отметить» / «Нельзя не отметить»
     * «Таким образом» (особенно в начале абзацев)
     * «В заключение» / «Подводя итоги»
     * «В современном мире» / «С незапамятных времен» / «Ни для кого не секрет»
     * «Актуальность данной темы не вызывает сомнений»
     * «Как известно» / «Стоит подчеркнуть» / «Необходимо сказать»
   - Начинай абзацы сразу с существа научной мысли, предметных фактов, названий концепций, фамилий исследователей, нормативных актов или конкретных аналитических утверждений.

3. ВЫСОКАЯ ПЛОТНОСТЬ И ТОЧНОЕ СОБЛЮДЕНИЕ ОБЪЕМА (ФГОС):
   - Текст должен строго заполнять заданный целевой объем символов и страниц (~1800 символов на страницу при стандартном форматировании 14pt по ФГОС).
   - Изложение обязано быть исчерпывающим, глубоким, с подробной аргументацией, классификациями, критериями и примерами.

4. ССЫЛКИ НА ИСТОЧНИКИ:
   - В тексте обязательно используй библиографические ссылки в квадратных скобках [1], [2, с. 45], [3-5], [6, 7].

5. ФОРМАТ ОТВЕТА:
   - Верни ИСКЛЮЧИТЕЛЬНО валидный JSON без markdown-блоков (```json), без пояснений до и после.
    """

    /**
     * 4.2. Промпт для отдельного подраздела (параграфа) длинной работы.
     */
    fun buildSubsectionPrompt(
        chapter: Int,
        sub: Int,
        topic: String,
        subject: String,
        targetChars: Int,
        previousSummary: String
    ): String = """
        Ты пишешь подраздел $chapter.$sub курсовой работы.
        
        Тема: $topic
        Предмет: $subject
        Целевой объём: ~$targetChars символов (${targetChars / 1800} стр.)
        
        Краткое содержание предыдущих разделов:
        $previousSummary
        
        ТРЕБОВАНИЯ:
        - Напиши ТОЛЬКО подраздел $chapter.$sub
        - Придумай точное научное название подраздела
        - 8–12 содержательных абзацев по 4–6 предложений
        - Академический стиль, ссылки [1], [2]
        - НЕ повторяй уже написанное
        - ЗАПРЕЩЕНЫ клише: «Важно отметить», «Таким образом», «В современном мире»
        
        ВЕРНИ ТОЛЬКО JSON (без markdown):
        {
          "section": "chapter_${chapter}_$sub",
          "title": "$chapter.$sub Название подраздела",
          "level": 2,
          "anchor": "h_ch${chapter}_$sub",
          "paragraphs": ["Абзац 1...", "Абзац 2...", "..."]
        }
    """.trimIndent()

    fun buildPrompt(
        stage: Stage,
        request: WorkRequest,
        previousWorks: List<PartialWork>,
        toc: List<TocItem>,
        profile: GostProfile,
        contextSummary: String = ContextCompressor.createThreeSentenceSummary(
            previousWorks, request.topic, request.subject, stage.label
        )
    ): String {
        return when (stage.type) {
            StageType.INTRO_AND_TOC -> buildIntroPrompt(stage, request, profile)
            StageType.SUBSECTION -> {
                val ch = stage.chapterIndex.coerceAtLeast(1)
                val sub = stage.subsectionIndex.coerceAtLeast(1)
                val targetChars = if (stage.targetChars > 0) stage.targetChars else 5400
                buildSubsectionPrompt(
                    chapter = ch,
                    sub = sub,
                    topic = request.topic,
                    subject = request.subject,
                    targetChars = targetChars,
                    previousSummary = contextSummary
                )
            }
            StageType.CHAPTER -> buildChapterPrompt(stage, request, previousWorks, toc, profile, contextSummary)
            StageType.CONCLUSION -> buildConclusionOnlyPrompt(stage, request, previousWorks, toc, profile, contextSummary)
            StageType.REFERENCES -> buildReferencesOnlyPrompt(stage, request, previousWorks, profile)
            StageType.CONCLUSION_AND_REFS -> buildConclusionAndRefsPrompt(stage, request, previousWorks, profile)
            StageType.ALL_IN_ONE -> AcademicSystemPrompt.buildUserPrompt(request, profile)
        }
    }

    private fun buildIntroPrompt(
        stage: Stage,
        request: WorkRequest,
        profile: GostProfile
    ): String {
        val chapterCount = if (request.targetPages >= 25) 3 else 2
        val subsPerChapter = if (request.targetPages >= 30) 3 else 2
        val targetPages = if (stage.targetPages > 0) stage.targetPages else (if (request.targetPages >= 30) 3 else 2)
        val targetChars = if (stage.targetChars > 0) stage.targetChars else targetPages * 1800
        val targetWords = if (stage.targetWords > 0) stage.targetWords else targetChars / 7

        val tocBuilder = StringBuilder()
        tocBuilder.appendLine("""    {"level": 1, "title": "Введение", "anchor": "h_intro"},""")
        for (ch in 1..chapterCount) {
            tocBuilder.appendLine("""    {"level": 1, "title": "$ch. [Точное научное название Главы $ch]", "anchor": "h_ch$ch"},""")
            for (sub in 1..subsPerChapter) {
                tocBuilder.appendLine("""    {"level": 2, "title": "$ch.$sub. [Название параграфа $ch.$sub]", "anchor": "h_ch${ch}_$sub"},""")
            }
        }
        tocBuilder.appendLine("""    {"level": 1, "title": "Заключение", "anchor": "h_concl"},""")
        tocBuilder.append("""    {"level": 1, "title": "Список использованных источников", "anchor": "h_refs"}""")

        return """
Ты генерируешь ПЕРВЫЙ ЭТАП академической работы: ВВЕДЕНИЕ и ПЛАН (ОГЛАВЛЕНИЕ).

ПАРАМЕТРЫ РАБОТЫ:
- Тип работы: ${request.type.titleRu}
- Тема: ${request.topic}
- Предмет / дисциплина: ${request.subject}
- Общий целевой объём работы: ~${request.targetPages} страниц
- Студент: ${request.studentName.ifBlank { "Иванов И.И." }}, курс ${request.studentCourse}, группа ${request.studentGroup.ifBlank { "Группа 1" }}
- ВУЗ: ${profile.universityName.ifBlank { "Ведущий университет" }} (${profile.name})
- Доп. требования: ${request.additionalRequirements.ifBlank { "Стандартные требования ГОСТ" }}

КВАНТОВАНИЕ ОБЪЕМА ЭТОГО РАЗДЕЛА (ФГОС 14pt):
- Целевой объем введения: ровно ~${targetPages} страниц (~${targetChars} символов с пробелами, ~${targetWords} слов).

ТРЕБОВАНИЯ К ВВЕДЕНИЮ:
1. Актуальность выбранной темы (научная и прикладная проблема).
2. Степень научной разработанности проблемы (исследования отечественных и зарубежных авторов со ссылками [1, 2]).
3. Объект и предмет исследования.
4. Цель исследования.
5. Задачи исследования (четкий список из 3-5 пунктов, строго соответствующих будущим главам).
6. Теоретико-методологическая основа и методы исследования.
7. Научная новизна и практическая значимость.
8. Структура работы: краткое описание того, чему посвящены будущие $chapterCount главы.

ВЕРНИ СТРОГО JSON В СЛЕДУЮЩЕМ ФОРМАТЕ:
{
  "section": "intro",
  "title": "Введение",
  "level": 1,
  "anchor": "h_intro",
  "paragraphs": [
    "Первый абзац с обоснованием актуальности темы...",
    "Второй абзац с анализом научной разработанности темы...",
    "Третий абзац с формулировкой объекта и предмета исследования...",
    "Четвертый абзац: Целью работы является...",
    "Пятый абзац: Для достижения поставленной цели необходимо решить следующие задачи: 1) ...; 2) ...; 3) ...",
    "Шестой абзац о методологической базе исследования...",
    "Седьмой абзац о практической значимости и структуре работы..."
  ],
  "toc": [
$tocBuilder
  ]
}
        """.trimIndent()
    }

    private fun buildChapterPrompt(
        stage: Stage,
        request: WorkRequest,
        previousWorks: List<PartialWork>,
        toc: List<TocItem>,
        profile: GostProfile,
        contextSummary: String = ContextCompressor.createThreeSentenceSummary(
            previousWorks, request.topic, request.subject, stage.label
        )
    ): String {
        val chapterNum = stage.chapterIndex.coerceAtLeast(1)
        val chapterAnchor = "h_ch$chapterNum"

        val chapterTocItem = toc.firstOrNull { it.anchor == chapterAnchor }
        val chapterTitle = chapterTocItem?.title ?: "Глава $chapterNum. Исследование темы «${request.topic}»"

        val subTocItems = toc.filter { it.anchor.startsWith("h_ch${chapterNum}_") }

        val targetPages = if (stage.targetPages > 0) stage.targetPages else 6
        val targetChars = if (stage.targetChars > 0) stage.targetChars else targetPages * 1800
        val targetWords = if (stage.targetWords > 0) stage.targetWords else targetChars / 7

        return """
Ты генерируешь ЭТАП: ГЛАВА $chapterNum академической работы.

ПАРАМЕТРЫ РАБОТЫ:
- Тип работы: ${request.type.titleRu}
- Тема работы: ${request.topic}
- Предмет: ${request.subject}
- Название этой главы: $chapterTitle
${if (subTocItems.isNotEmpty()) "- Запланированные подразделы:\n" + subTocItems.joinToString("\n") { "  * ${it.title}" } else ""}

КОНТЕКСТНАЯ ВЫЖИМКА ПРЕДЫДУЩИХ РАЗДЕЛОВ:
$contextSummary

КВАНТОВАНИЕ ОБЪЕМА ЭТОЙ ГЛАВЫ (ФГОС 14pt):
- Целевой объем этой главы: ровно ~${targetPages} страниц (~${targetChars} символов с пробелами, ~${targetWords} слов).

ВЕРНИ СТРОГО JSON В СЛЕДУЮЩЕМ ФОРМАТЕ:
{
  "section": "chapter_$chapterNum",
  "title": "$chapterTitle",
  "level": 1,
  "anchor": "$chapterAnchor",
  "paragraphs": [
    "Вводный абзац к главе $chapterNum с постановкой целей исследования данного раздела..."
  ],
  "subsections": [
    {
      "title": "${subTocItems.getOrNull(0)?.title ?: "$chapterNum.1. Теоретический анализ проблемы"}",
      "level": 2,
      "anchor": "${subTocItems.getOrNull(0)?.anchor ?: "h_ch${chapterNum}_1"}",
      "paragraphs": [
        "Развернутый первый абзац подраздела с дефиницией и научной базой [1]...",
        "Второй абзац с классификацией и анализом подходов [2, с. 15]...",
        "Третий абзац с подробными научными фактами и примерами...",
        "Четвертый абзац с сопоставлением различных методик...",
        "Пятый абзац с промежуточными выводами подраздела..."
      ]
    },
    {
      "title": "${subTocItems.getOrNull(1)?.title ?: "$chapterNum.2. Практические аспекты и специфика"}",
      "level": 2,
      "anchor": "${subTocItems.getOrNull(1)?.anchor ?: "h_ch${chapterNum}_2"}",
      "paragraphs": [
        "Первый абзац второго подраздела...",
        "Второй абзац с практическим материалом и аналитическими данными...",
        "Третий абзац...",
        "Четвертый абзац...",
        "Пятый абзац с выводами по подразделу..."
      ]
    }
  ]
}
        """.trimIndent()
    }

    private fun buildConclusionOnlyPrompt(
        stage: Stage,
        request: WorkRequest,
        previousWorks: List<PartialWork>,
        toc: List<TocItem>,
        profile: GostProfile,
        contextSummary: String = ContextCompressor.createThreeSentenceSummary(
            previousWorks, request.topic, request.subject, stage.label
        )
    ): String {
        val chapterTitles = previousWorks.map { it.title }.joinToString("\n- ")
        val targetPages = if (stage.targetPages > 0) stage.targetPages else 2
        val targetChars = if (stage.targetChars > 0) stage.targetChars else targetPages * 1800

        return """
Ты генерируешь ЧАСТЬ академической работы — ЗАКЛЮЧЕНИЕ.

ПАРАМЕТРЫ РАБОТЫ:
- Тип работы: ${request.type.titleRu}
- Тема работы: ${request.topic}
- Предмет: ${request.subject}
- Написанные разделы работы:
- $chapterTitles

КОНТЕКСТНАЯ ВЫЖИМКА ВСЕГО ИССЛЕДОВАНИЯ:
$contextSummary

КВАНТОВАНИЕ ОБЪЕМА ЗАКЛЮЧЕНИЯ:
- Целевой объем заключения: ~${targetPages} страниц (~${targetChars} символов).
- НЕ включай список литературы в этот этап (он генерируется отдельным запросом).

ВЕРНИ СТРОГО JSON:
{
  "section": "conclusion",
  "title": "Заключение",
  "level": 1,
  "anchor": "h_concl",
  "paragraphs": [
    "Первый абзац с констатацией достижения цели исследования по теме «${request.topic}»...",
    "Второй абзац с выводами по решению первой задачи...",
    "Третий абзац с выводами по решению второй задачи...",
    "Четвертый абзац с выводами по решению третьей задачи...",
    "Пятый абзац с описанием теоретической и практической значимости полученных результатов...",
    "Шестой абзац с рекомендациями по дальнейшему внедрению и развитию тематики."
  ]
}
        """.trimIndent()
    }

    private fun buildReferencesOnlyPrompt(
        stage: Stage,
        request: WorkRequest,
        previousWorks: List<PartialWork>,
        profile: GostProfile
    ): String {
        val count = stage.maxReferences.coerceAtLeast(10)

        return """
Ты генерируешь ЧАСТЬ академической работы — СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ.

ПАРАМЕТРЫ РАБОТЫ:
- Тип работы: ${request.type.titleRu}
- Тема работы: ${request.topic}
- Предмет: ${request.subject}
- Требуемое количество источников: минимум $count

ВЕРНИ СТРОГО JSON:
{
  "section": "references",
  "title": "Список использованных источников",
  "level": 1,
  "anchor": "h_refs",
  "references": [
    "1. Автор А.А. Название книги: монография / под ред. И.И. Сидорова. — М.: Наука, 2021. — 250 с.",
    "2. Автор Б.Б., Автор В.В. Анализ тенденций и подходов // Вестник университета. — 2022. — № 3. — С. 45–54.",
    "3. Федеральный закон или официальный нормативный документ по теме...",
    "4. ..."
  ]
}
        """.trimIndent()
    }

    private fun buildConclusionAndRefsPrompt(
        stage: Stage,
        request: WorkRequest,
        previousWorks: List<PartialWork>,
        profile: GostProfile
    ): String {
        val chapterTitles = previousWorks.map { it.title }.joinToString("\n- ")

        return """
Ты генерируешь ЗАКЛЮЧИТЕЛЬНЫЙ ЭТАП: ЗАКЛЮЧЕНИЕ и СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ.

ПАРАМЕТРЫ РАБОТЫ:
- Тип работы: ${request.type.titleRu}
- Тема работы: ${request.topic}
- Предмет: ${request.subject}
- Написанные разделы:
- $chapterTitles

ВЕРНИ СТРОГО JSON:
{
  "sections": [
    {
      "section": "conclusion",
      "title": "Заключение",
      "level": 1,
      "anchor": "h_concl",
      "paragraphs": [
        "Первый абзац с общим итогом исследования по теме «${request.topic}»...",
        "Второй абзац с выводами по первой задаче...",
        "Третий абзац с выводами по второй задаче...",
        "Четвертый абзац с практической значимостью..."
      ]
    }
  ],
  "references": [
    "1. Автор А.А. Название монографии. — М.: Наука, 2022. — 250 с.",
    "2. ..."
  ]
}
        """.trimIndent()
    }
}
