package com.example.domain.model

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
    val id: String = "",
    val title: String = "",
    val type: SectionType = SectionType.CHAPTER,
    val chapterNumber: Int? = null,
    val subsectionNumber: Int? = null,
    val targetPages: Double = 2.0,
    val targetCharacters: Int = 3600,
    val minCharacters: Int = 3000,
    val maxCharacters: Int = 4500,
    var content: String = "",
    var isGenerated: Boolean = false,
    val anchor: String = id.ifBlank { "sec_${title.hashCode()}" },
    val level: Int = if (type == SectionType.SUBSECTION) 2 else 1,
    val heading: String = title,
    val paragraphs: List<String> = if (content.isNotBlank()) {
        content.split("\n\n", "\n").map { it.trim() }.filter { it.isNotEmpty() }
    } else {
        emptyList()
    }
) {
    constructor(
        anchor: String,
        level: Int,
        heading: String,
        paragraphs: List<String>
    ) : this(
        id = anchor,
        title = heading,
        type = when {
            anchor.contains("intro", ignoreCase = true) || heading.equals("Введение", ignoreCase = true) -> SectionType.INTRODUCTION
            anchor.contains("concl", ignoreCase = true) || heading.equals("Заключение", ignoreCase = true) -> SectionType.CONCLUSION
            anchor.contains("ref", ignoreCase = true) || heading.contains("источник", ignoreCase = true) || heading.contains("литератур", ignoreCase = true) -> SectionType.BIBLIOGRAPHY
            level >= 2 -> SectionType.SUBSECTION
            else -> SectionType.CHAPTER
        },
        targetPages = ((paragraphs.sumOf { it.length } / 1800.0).coerceAtLeast(1.0)),
        targetCharacters = paragraphs.sumOf { it.length }.coerceAtLeast(1800),
        minCharacters = 1500,
        maxCharacters = 10000,
        content = paragraphs.joinToString("\n\n"),
        isGenerated = paragraphs.isNotEmpty(),
        anchor = anchor,
        level = level,
        heading = heading,
        paragraphs = paragraphs
    )
}

data class WorkMeta(
    val topic: String = "",
    val type: WorkType = WorkType.COURSEWORK,
    val targetPages: Int = 35,
    val university: String = "Российский университет",
    val faculty: String = "Факультет информационных технологий и экономики",
    val department: String = "Кафедра прикладных исследований",
    val discipline: String = "Профессиональные дисциплины",
    val authorName: String = "Студент И.И.",
    val groupName: String = "",
    val supervisorName: String = "Научный руководитель, д.э.н., проф. Смирнов А.В.",
    val city: String = "Москва",
    val year: Int = 2026,
    val subject: String = discipline,
    val language: String = "ru",
    val gostProfile: String = "ГОСТ 7.32-2017",
    val estimatedWords: Int = targetPages * 260
) {
    val studentName: String get() = authorName
}

data class WorkDocument(
    val id: Long = System.currentTimeMillis(),
    val meta: WorkMeta,
    val sections: List<WorkSection> = emptyList(),
    val docxPath: String? = null,
    val docxFilePath: String? = docxPath,
    val titlePage: TitlePage = TitlePage(
        university = meta.university,
        faculty = meta.faculty,
        department = meta.department,
        workTypeLabel = meta.type.titleRu.uppercase(),
        topic = meta.topic,
        subject = meta.subject.ifBlank { meta.discipline },
        student = meta.authorName,
        teacher = meta.supervisorName,
        city = meta.city,
        year = meta.year
    ),
    val toc: List<TocItem> = sections.map {
        TocItem(
            level = it.level,
            title = it.title.ifBlank { it.heading },
            anchor = it.anchor.ifBlank { it.id }
        )
    },
    val references: List<String> = emptyList(),
    val appendix: List<Appendix> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
