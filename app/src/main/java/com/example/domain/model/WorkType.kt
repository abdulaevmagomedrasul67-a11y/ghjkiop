package com.example.domain.model

/**
 * Тип академической работы.
 */
enum class WorkType(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val defaultPages: Int,
    val minPages: Int,
    val maxPages: Int,
    val defaultChapters: Int,
    val priceSc: Int,
    val apiValue: String
) {
    REFERAT(
        id = "referat",
        titleRu = "Реферат",
        titleEn = "Report / Referat",
        defaultPages = 15,
        minPages = 10,
        maxPages = 28,
        defaultChapters = 2,
        priceSc = 200,
        apiValue = "referat"
    ),
    COURSEWORK(
        id = "coursework",
        titleRu = "Курсовая работа",
        titleEn = "Coursework / Term Paper",
        defaultPages = 35,
        minPages = 25,
        maxPages = 55,
        defaultChapters = 3,
        priceSc = 480,
        apiValue = "kursovaya"
    ),
    REPORT(
        id = "report",
        titleRu = "Доклад",
        titleEn = "Presentation Report",
        defaultPages = 10,
        minPages = 5,
        maxPages = 20,
        defaultChapters = 1,
        priceSc = 120,
        apiValue = "doklad"
    ),
    ESSAY(
        id = "essay",
        titleRu = "Эссе",
        titleEn = "Essay",
        defaultPages = 5,
        minPages = 3,
        maxPages = 15,
        defaultChapters = 1,
        priceSc = 320,
        apiValue = "esse"
    ),
    CONTROL_WORK(
        id = "control_work",
        titleRu = "Контрольная работа",
        titleEn = "Test / Control Paper",
        defaultPages = 12,
        minPages = 8,
        maxPages = 25,
        defaultChapters = 1,
        priceSc = 160,
        apiValue = "kontrolnaya"
    ),
    DIPLOMA_VKR(
        id = "diploma_vkr",
        titleRu = "Дипломная работа (ВКР)",
        titleEn = "Diploma Thesis (VKR)",
        defaultPages = 65,
        minPages = 50,
        maxPages = 90,
        defaultChapters = 3,
        priceSc = 1200,
        apiValue = "diploma"
    ),
    MASTER_DISSERTATION(
        id = "master_dissertation",
        titleRu = "Магистерская диссертация",
        titleEn = "Master's Dissertation",
        defaultPages = 85,
        minPages = 70,
        maxPages = 120,
        defaultChapters = 3,
        priceSc = 1800,
        apiValue = "master"
    ),
    ESSAY_REFERAT(
        id = "essay_referat",
        titleRu = "Реферат / Эссе",
        titleEn = "Essay / Referat",
        defaultPages = 18,
        minPages = 12,
        maxPages = 28,
        defaultChapters = 2,
        priceSc = 220,
        apiValue = "referat"
    ),
    SCIENTIFIC_ARTICLE(
        id = "scientific_article",
        titleRu = "Научная статья (ВАК / РИНЦ)",
        titleEn = "Scientific Article (VAK / RSCI)",
        defaultPages = 12,
        minPages = 8,
        maxPages = 20,
        defaultChapters = 1,
        priceSc = 350,
        apiValue = "article"
    );

    val code: String get() = apiValue

    companion object {
        val ALLOWED_API_TYPES = listOf("referat", "doklad", "kursovaya", "esse", "kontrolnaya")

        fun fromCode(code: String): WorkType = fromString(code)

        fun fromString(value: String): WorkType =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                        it.id.equals(value, ignoreCase = true) ||
                        it.apiValue.equals(value, ignoreCase = true)
            } ?: COURSEWORK

        fun fromId(id: String): WorkType {
            return entries.firstOrNull {
                it.id.equals(id, ignoreCase = true) ||
                        it.apiValue.equals(id, ignoreCase = true) ||
                        it.name.equals(id, ignoreCase = true)
            } ?: REFERAT
        }

        fun fromApiValue(value: String): WorkType {
            return entries.firstOrNull {
                it.apiValue.equals(value, ignoreCase = true) ||
                        it.id.equals(value, ignoreCase = true)
            } ?: REFERAT
        }
    }
}
