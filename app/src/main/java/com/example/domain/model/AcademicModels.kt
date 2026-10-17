package com.example.domain.model

enum class WorkType(
    val titleRu: String,
    val defaultPages: Int,
    val minPages: Int,
    val maxPages: Int,
    val defaultChapters: Int,
    val priceSc: Int
) {
    COURSEWORK(
        titleRu = "Курсовая работа",
        defaultPages = 35,
        minPages = 25,
        maxPages = 55,
        defaultChapters = 2,
        priceSc = 480
    ),
    DIPLOMA_VKR(
        titleRu = "Дипломная работа (ВКР)",
        defaultPages = 65,
        minPages = 50,
        maxPages = 90,
        defaultChapters = 3,
        priceSc = 1200
    ),
    MASTER_DISSERTATION(
        titleRu = "Магистерская диссертация",
        defaultPages = 85,
        minPages = 70,
        maxPages = 120,
        defaultChapters = 3,
        priceSc = 1800
    ),
    ESSAY_REFERAT(
        titleRu = "Реферат / Эссе",
        defaultPages = 18,
        minPages = 12,
        maxPages = 28,
        defaultChapters = 2,
        priceSc = 220
    ),
    SCIENTIFIC_ARTICLE(
        titleRu = "Научная статья (ВАК / РИНЦ)",
        defaultPages = 12,
        minPages = 8,
        maxPages = 20,
        defaultChapters = 1,
        priceSc = 350
    );

    companion object {
        fun fromString(value: String): WorkType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COURSEWORK

        fun fromId(id: String): WorkType =
            entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: when (id.lowercase()) {
                "referat", "esse", "essay" -> ESSAY_REFERAT
                "kursovaya", "coursework" -> COURSEWORK
                "diploma", "vkr" -> DIPLOMA_VKR
                "dissertation", "master" -> MASTER_DISSERTATION
                "article" -> SCIENTIFIC_ARTICLE
                else -> COURSEWORK
            }

        fun fromApiValue(value: String): WorkType = fromId(value)
    }
}

enum class SectionType {
    TITLE,
    TABLE_OF_CONTENTS,
    INTRODUCTION,
    CHAPTER,
    SUBSECTION,
    CONCLUSION,
    BIBLIOGRAPHY,
    APPENDIX
}

data class WorkSection(
    val id: String,
    val title: String,
    val type: SectionType,
    val chapterNumber: Int? = null,
    val subsectionNumber: Int? = null,
    val targetPages: Double,
    val targetCharacters: Int,
    val minCharacters: Int,
    val maxCharacters: Int,
    var content: String = "",
    var isGenerated: Boolean = false
)

data class WorkMeta(
    val topic: String,
    val type: WorkType,
    val targetPages: Int,
    val university: String = "Российский университет",
    val faculty: String = "Факультет информационных технологий и экономики",
    val department: String = "Кафедра прикладных исследований",
    val discipline: String = "Профессиональные дисциплины",
    val authorName: String = "Студент И.И.",
    val supervisorName: String = "Научный руководитель, д.э.н., проф. Смирнов А.В.",
    val city: String = "Москва",
    val year: Int = 2026
)

data class WorkDocument(
    val id: Long = System.currentTimeMillis(),
    val meta: WorkMeta,
    val sections: List<WorkSection>,
    val docxPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
