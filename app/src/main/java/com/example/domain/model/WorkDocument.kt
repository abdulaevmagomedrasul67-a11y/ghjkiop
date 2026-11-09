package com.example.domain.model

data class TitlePage(
    val university: String = "Московский государственный университет",
    val faculty: String = "",
    val department: String = "",
    val workTypeLabel: String = "КУРСОВАЯ РАБОТА",
    val topic: String = "",
    val subject: String = "",
    val student: String = "Иванов И.И.",
    val group: String = "",
    val teacher: String = "Преподаватель",
    val city: String = "Москва",
    val year: Int = 2026
)

data class TocItem(
    val level: Int,
    val title: String,
    val anchor: String
)

data class Appendix(
    val title: String,
    val content: String
)

typealias Section = WorkSection
