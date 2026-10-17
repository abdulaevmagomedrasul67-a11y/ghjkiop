package com.example.domain.model

/**
 * Профиль оформления документа (ГОСТ 7.32 / ГОСТ Р 7.0.11 или индивидуальные требования ВУЗа).
 */
data class GostProfile(
    val id: Long = 1L,
    val name: String = "ГОСТ 7.32 (Стандарт РФ)",
    val universityName: String = "Министерство науки и высшего образования РФ",
    val facultyName: String = "",
    val departmentName: String = "",
    val leftMarginMm: Float = 30f,    // 30 мм слева для подшивки
    val rightMarginMm: Float = 15f,   // 10-15 мм справа
    val topMarginMm: Float = 20f,     // 20 мм сверху
    val bottomMarginMm: Float = 20f,  // 20 мм снизу
    val fontName: String = "Times New Roman",
    val fontSizePt: Float = 14f,      // 14 pt кегль
    val lineSpacing: Float = 1.5f,    // 1.5 полуторный интервал
    val paragraphIndentMm: Float = 12.5f, // 1.25 см красная строка (абзацный отступ)
    val startPageNumbering: Int = 2,  // Титульник не нумеруется, со 2-й страницы
    val bibliographyFormat: String = "ГОСТ Р 7.0.100-2018",
    val isDefault: Boolean = true
) {
    companion object {
        val DEFAULT_GOST = GostProfile(
            id = 1L,
            name = "ГОСТ 7.32-2017 (Стандартный)",
            universityName = "Министерство науки и высшего образования Российской Федерации",
            leftMarginMm = 30f,
            rightMarginMm = 15f,
            topMarginMm = 20f,
            bottomMarginMm = 20f,
            fontName = "Times New Roman",
            fontSizePt = 14f,
            lineSpacing = 1.5f,
            paragraphIndentMm = 12.5f,
            isDefault = true
        )
    }
}
