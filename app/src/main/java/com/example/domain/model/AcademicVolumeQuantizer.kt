package com.example.domain.model

object AcademicVolumeQuantizer {

    const val CHARS_PER_PAGE_GOST = 1800
    const val WORDS_PER_PAGE_GOST = 260

    data class SectionQuantization(
        val pages: Int,
        val chars: Int,
        val words: Int,
        val maxTokens: Int
    )

    fun quantizeSection(pages: Int, minTokens: Int = 3500): SectionQuantization {
        val safePages = pages.coerceAtLeast(1)
        val chars = safePages * CHARS_PER_PAGE_GOST
        val words = safePages * WORDS_PER_PAGE_GOST
        val estimatedTokens = (chars / 2.3).toInt() + 1000
        val maxTokens = estimatedTokens.coerceIn(minTokens, 8192)
        return SectionQuantization(
            pages = safePages,
            chars = chars,
            words = words,
            maxTokens = maxTokens
        )
    }

    data class WorkVolumeDistribution(
        val totalPages: Int,
        val introPages: Int,
        val chapterPages: List<Int>,
        val conclusionPages: Int,
        val referencePages: Int,
        val maxReferences: Int
    )

    fun distributePages(
        workType: WorkType,
        targetPages: Int
    ): WorkVolumeDistribution {
        val totalPages = targetPages.coerceAtLeast(workType.defaultPages)
        val netPages = (totalPages - 2).coerceAtLeast(4)

        val chapterCount = when (workType) {
            WorkType.COURSEWORK, WorkType.DIPLOMA_VKR, WorkType.MASTER_DISSERTATION -> when {
                totalPages >= 55 -> 4
                totalPages >= 30 -> 3
                else -> 2
            }
            WorkType.REFERAT, WorkType.ESSAY_REFERAT -> when {
                totalPages >= 50 -> 4
                totalPages >= 30 -> 3
                else -> 2
            }
            WorkType.CONTROL_WORK, WorkType.SCIENTIFIC_ARTICLE -> 1
            WorkType.REPORT -> if (totalPages >= 20) 3 else if (totalPages >= 12) 2 else 1
            WorkType.ESSAY -> if (totalPages >= 15) 2 else 1
        }

        val introPages = when {
            totalPages >= 70 -> 6
            totalPages >= 45 -> 4
            totalPages >= 25 -> 4
            totalPages >= 15 -> 3
            else -> 2
        }

        val conclusionPages = when {
            totalPages >= 70 -> 5
            totalPages >= 45 -> 4
            totalPages >= 25 -> 3
            else -> 2
        }

        val refPages = when {
            totalPages >= 60 -> 3
            totalPages >= 30 -> 2
            else -> 1
        }

        val maxReferences = when {
            totalPages >= 70 -> 35
            totalPages >= 50 -> 30
            totalPages >= 35 -> 25
            totalPages >= 20 -> 20
            else -> 12
        }

        val remainingForChapters = (netPages - introPages - conclusionPages - refPages)
            .coerceAtLeast(chapterCount * 2)

        val baseChapterPages = remainingForChapters / chapterCount
        val extraPages = remainingForChapters % chapterCount

        val chapterPagesList = (1..chapterCount).map { i ->
            baseChapterPages + if (i <= extraPages) 1 else 0
        }

        return WorkVolumeDistribution(
            totalPages = totalPages,
            introPages = introPages,
            chapterPages = chapterPagesList,
            conclusionPages = conclusionPages,
            referencePages = refPages,
            maxReferences = maxReferences
        )
    }
}
