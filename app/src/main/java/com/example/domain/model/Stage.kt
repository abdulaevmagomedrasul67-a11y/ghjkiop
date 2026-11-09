package com.example.domain.model

/**
 * Тип этапа генерации.
 */
enum class StageType {
    ALL_IN_ONE,          // Доклад (5-10 стр) или Эссе (5-12 стр) в один полный запрос
    INTRO_AND_TOC,       // Титульник + Оглавление + Введение
    CHAPTER,             // Отдельная глава с подразделами
    SUBSECTION,          // Отдельный параграф (подраздел) для длинных работ (курсовая 30-40+ стр)
    CONCLUSION,          // Заключение (отдельный запрос, без литературы)
    REFERENCES,          // Список литературы (отдельный запрос, предотвращает обрыв токенов)
    CONCLUSION_AND_REFS  // Объединённый этап (обратная совместимость)
}

/**
 * Описание одного этапа генерации (chunked stage).
 */
data class Stage(
    val id: String = "",
    val label: String,
    val section: String = id.ifEmpty { label },
    val type: StageType = StageType.SUBSECTION,
    val chapterIndex: Int = 0,
    val subsectionIndex: Int = 0,
    val targetPages: Int = 0,
    val maxTokens: Int = 4000,
    val maxReferences: Int = 15,
    val targetChars: Int = 0,
    val targetWords: Int = 0
)

/**
 * Планировщик этапов генерации в зависимости от типа работы и целевого объёма страниц.
 * Для курсовых работ (30–40+ стр) разбивает каждую главу на отдельные параграфы (подразделы),
 * чтобы даже «простые» модели (Gemini Flash, DeepSeek, Vibe) генерировали 3–4 страницы за 1 запрос без обрыва.
 */
object StagePlanner {

    private const val CHARS_PER_PAGE = 1800

    /**
     * 4.1. Более мелкое разбиение по строковому коду работы ("kursovaya", "referat", etc.)
     * Для курсовой 30-40 страниц = 12 этапов (1 введение + 9 подразделов + 1 заключение + 1 литература).
     */
    fun planStages(workType: String, pages: Int): List<Stage> {
        val totalChars = pages * CHARS_PER_PAGE

        return when (workType.lowercase().trim()) {
            "kursovaya", "coursework" -> {
                val stages = mutableListOf<Stage>()

                // 1. Введение (~6%)
                val introChars = (totalChars * 0.06).toInt()
                stages.add(
                    Stage(
                        id = "intro",
                        label = "Введение",
                        section = "intro",
                        type = StageType.INTRO_AND_TOC,
                        targetPages = (introChars / CHARS_PER_PAGE).coerceAtLeast(2),
                        targetChars = introChars,
                        targetWords = introChars / 7,
                        maxTokens = 3000
                    )
                )

                // 2. 3 главы × 2-3 подраздела = 6-9 отдельных запросов
                val chaptersCount = 3
                val subsPerChapter = if (pages >= 30) 3 else 2
                val subChars = (totalChars * 0.75) / (chaptersCount * subsPerChapter)
                val subPages = (subChars / CHARS_PER_PAGE).toInt().coerceAtLeast(3)

                for (ch in 1..chaptersCount) {
                    for (sub in 1..subsPerChapter) {
                        val secId = "chapter_${ch}_$sub"
                        stages.add(
                            Stage(
                                id = secId,
                                label = "Глава $ch.$sub",
                                section = secId,
                                type = StageType.SUBSECTION,
                                chapterIndex = ch,
                                subsectionIndex = sub,
                                targetPages = subPages,
                                targetChars = subChars.toInt(),
                                targetWords = (subChars / 7).toInt(),
                                maxTokens = (subChars / 4).toInt().coerceIn(2000, 4000)
                            )
                        )
                    }
                }

                // 3. Заключение (~6%)
                val conclChars = (totalChars * 0.06).toInt()
                stages.add(
                    Stage(
                        id = "conclusion",
                        label = "Заключение",
                        section = "conclusion",
                        type = StageType.CONCLUSION,
                        targetPages = (conclChars / CHARS_PER_PAGE).coerceAtLeast(2),
                        targetChars = conclChars,
                        targetWords = conclChars / 7,
                        maxTokens = 3000
                    )
                )

                // 4. Список литературы (~13%)
                val refChars = (totalChars * 0.13).toInt()
                stages.add(
                    Stage(
                        id = "references",
                        label = "Список литературы",
                        section = "references",
                        type = StageType.REFERENCES,
                        targetChars = refChars,
                        maxTokens = 3000,
                        maxReferences = if (pages >= 30) 25 else 15
                    )
                )

                stages
            }

            else -> {
                val resolvedType = WorkType.fromCode(workType)
                plan(resolvedType, pages)
            }
        }
    }

    fun plan(workType: WorkType, targetPages: Int): List<Stage> {
        val pages = targetPages.coerceAtLeast(workType.defaultPages)
        if (workType == WorkType.COURSEWORK) {
            return planStages("kursovaya", pages)
        }

        val dist = AcademicVolumeQuantizer.distributePages(workType, pages)

        return when (workType) {
            WorkType.REFERAT -> {
                val stages = mutableListOf<Stage>()
                val introQ = AcademicVolumeQuantizer.quantizeSection(dist.introPages, minTokens = 3000)
                stages.add(
                    Stage(
                        id = "intro",
                        label = "Введение и план работы",
                        section = "intro",
                        type = StageType.INTRO_AND_TOC,
                        targetPages = dist.introPages,
                        targetChars = introQ.chars,
                        targetWords = introQ.words,
                        maxTokens = introQ.maxTokens.coerceAtMost(4000)
                    )
                )

                dist.chapterPages.forEachIndexed { idx, chPages ->
                    val chIndex = idx + 1
                    val chQ = AcademicVolumeQuantizer.quantizeSection(chPages, minTokens = 4000)
                    stages.add(
                        Stage(
                            id = "chapter_$chIndex",
                            label = "Глава $chIndex",
                            section = "chapter_$chIndex",
                            type = StageType.CHAPTER,
                            chapterIndex = chIndex,
                            targetPages = chPages,
                            targetChars = chQ.chars,
                            targetWords = chQ.words,
                            maxTokens = chQ.maxTokens.coerceAtMost(4000)
                        )
                    )
                }

                val conclQ = AcademicVolumeQuantizer.quantizeSection(dist.conclusionPages, minTokens = 3000)
                stages.add(
                    Stage(
                        id = "conclusion",
                        label = "Заключение",
                        section = "conclusion",
                        type = StageType.CONCLUSION,
                        targetPages = dist.conclusionPages,
                        targetChars = conclQ.chars,
                        targetWords = conclQ.words,
                        maxTokens = conclQ.maxTokens.coerceAtMost(3500)
                    )
                )

                stages.add(
                    Stage(
                        id = "references",
                        label = "Список литературы",
                        section = "references",
                        type = StageType.REFERENCES,
                        maxTokens = 3000,
                        maxReferences = dist.maxReferences.coerceAtLeast(10)
                    )
                )
                stages
            }

            WorkType.REPORT -> {
                if (pages <= 10) {
                    val q = AcademicVolumeQuantizer.quantizeSection(pages, minTokens = 4000)
                    listOf(
                        Stage(
                            id = "doklad_full",
                            label = "Доклад целиком",
                            section = "doklad_full",
                            type = StageType.ALL_IN_ONE,
                            targetPages = pages,
                            targetChars = q.chars,
                            targetWords = q.words,
                            maxTokens = 4000
                        )
                    )
                } else {
                    planSequentialReport(pages, dist)
                }
            }

            WorkType.COURSEWORK, WorkType.DIPLOMA_VKR, WorkType.MASTER_DISSERTATION -> planStages("kursovaya", pages)

            WorkType.ESSAY, WorkType.ESSAY_REFERAT, WorkType.SCIENTIFIC_ARTICLE -> {
                if (pages <= 10) {
                    val q = AcademicVolumeQuantizer.quantizeSection(pages, minTokens = 4000)
                    listOf(
                        Stage(
                            id = "essay_full",
                            label = "Эссе целиком",
                            section = "essay_full",
                            type = StageType.ALL_IN_ONE,
                            targetPages = pages,
                            targetChars = q.chars,
                            targetWords = q.words,
                            maxTokens = 4000
                        )
                    )
                } else {
                    planSequentialEssay(pages, dist)
                }
            }

            WorkType.CONTROL_WORK -> {
                val introQ = AcademicVolumeQuantizer.quantizeSection(2, minTokens = 3000)
                val mainPages = (pages - 4).coerceAtLeast(6)
                val mainQ = AcademicVolumeQuantizer.quantizeSection(mainPages, minTokens = 4000)
                val conclQ = AcademicVolumeQuantizer.quantizeSection(2, minTokens = 3000)
                listOf(
                    Stage(
                        id = "intro",
                        label = "Введение и постановка задач",
                        section = "intro",
                        type = StageType.INTRO_AND_TOC,
                        targetPages = 2,
                        targetChars = introQ.chars,
                        targetWords = introQ.words,
                        maxTokens = 3000
                    ),
                    Stage(
                        id = "main_part",
                        label = "Основная часть",
                        section = "main_part",
                        type = StageType.CHAPTER,
                        chapterIndex = 1,
                        targetPages = mainPages,
                        targetChars = mainQ.chars,
                        targetWords = mainQ.words,
                        maxTokens = mainQ.maxTokens.coerceAtMost(4000)
                    ),
                    Stage(
                        id = "conclusion",
                        label = "Заключение",
                        section = "conclusion",
                        type = StageType.CONCLUSION,
                        targetPages = 2,
                        targetChars = conclQ.chars,
                        targetWords = conclQ.words,
                        maxTokens = 3000
                    ),
                    Stage(
                        id = "references",
                        label = "Список литературы",
                        section = "references",
                        type = StageType.REFERENCES,
                        maxTokens = 3000,
                        maxReferences = dist.maxReferences.coerceAtLeast(10)
                    )
                )
            }
        }
    }

    private fun planSequentialReport(pages: Int, dist: AcademicVolumeQuantizer.WorkVolumeDistribution): List<Stage> {
        val stages = mutableListOf<Stage>()
        val introQ = AcademicVolumeQuantizer.quantizeSection(dist.introPages, minTokens = 3000)
        stages.add(
            Stage(
                id = "intro",
                label = "Введение и план работы",
                section = "intro",
                type = StageType.INTRO_AND_TOC,
                targetPages = dist.introPages,
                targetChars = introQ.chars,
                targetWords = introQ.words,
                maxTokens = introQ.maxTokens.coerceAtMost(3500)
            )
        )
        dist.chapterPages.forEachIndexed { idx, chPages ->
            val chIndex = idx + 1
            val chQ = AcademicVolumeQuantizer.quantizeSection(chPages, minTokens = 4000)
            stages.add(
                Stage(
                    id = "chapter_$chIndex",
                    label = "Раздел $chIndex",
                    section = "chapter_$chIndex",
                    type = StageType.CHAPTER,
                    chapterIndex = chIndex,
                    targetPages = chPages,
                    targetChars = chQ.chars,
                    targetWords = chQ.words,
                    maxTokens = chQ.maxTokens.coerceAtMost(4000)
                )
            )
        }
        val conclQ = AcademicVolumeQuantizer.quantizeSection(dist.conclusionPages, minTokens = 3000)
        stages.add(
            Stage(
                id = "conclusion",
                label = "Заключение",
                section = "conclusion",
                type = StageType.CONCLUSION,
                targetPages = dist.conclusionPages,
                targetChars = conclQ.chars,
                targetWords = conclQ.words,
                maxTokens = conclQ.maxTokens.coerceAtMost(3500)
            )
        )
        stages.add(
            Stage(
                id = "references",
                label = "Список литературы",
                section = "references",
                type = StageType.REFERENCES,
                maxTokens = 3000,
                maxReferences = dist.maxReferences.coerceAtLeast(10)
            )
        )
        return stages
    }

    private fun planSequentialEssay(pages: Int, dist: AcademicVolumeQuantizer.WorkVolumeDistribution): List<Stage> {
        val stages = mutableListOf<Stage>()
        val introQ = AcademicVolumeQuantizer.quantizeSection(dist.introPages, minTokens = 3000)
        stages.add(
            Stage(
                id = "intro",
                label = "Введение и план работы",
                section = "intro",
                type = StageType.INTRO_AND_TOC,
                targetPages = dist.introPages,
                targetChars = introQ.chars,
                targetWords = introQ.words,
                maxTokens = introQ.maxTokens.coerceAtMost(3000)
            )
        )
        dist.chapterPages.forEachIndexed { idx, chPages ->
            val chIndex = idx + 1
            val chQ = AcademicVolumeQuantizer.quantizeSection(chPages, minTokens = 3500)
            stages.add(
                Stage(
                    id = "chapter_$chIndex",
                    label = "Часть $chIndex",
                    section = "chapter_$chIndex",
                    type = StageType.CHAPTER,
                    chapterIndex = chIndex,
                    targetPages = chPages,
                    targetChars = chQ.chars,
                    targetWords = chQ.words,
                    maxTokens = chQ.maxTokens.coerceAtMost(4000)
                )
            )
        }
        val conclQ = AcademicVolumeQuantizer.quantizeSection(dist.conclusionPages, minTokens = 3000)
        stages.add(
            Stage(
                id = "conclusion",
                label = "Заключение",
                section = "conclusion",
                type = StageType.CONCLUSION,
                targetPages = dist.conclusionPages,
                targetChars = conclQ.chars,
                targetWords = conclQ.words,
                maxTokens = conclQ.maxTokens.coerceAtMost(3000)
            )
        )
        stages.add(
            Stage(
                id = "references",
                label = "Список литературы",
                section = "references",
                type = StageType.REFERENCES,
                maxTokens = 3000,
                maxReferences = dist.maxReferences.coerceAtLeast(8)
            )
        )
        return stages
    }
}
