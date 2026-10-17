package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.GostProfile

/**
 * Room-сущность профиля оформления документа (ГОСТ / Требования ВУЗа).
 */
@Entity(tableName = "university_profiles")
data class UniversityProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val universityName: String,
    val facultyName: String = "",
    val departmentName: String = "",
    val leftMarginMm: Float = 30f,
    val rightMarginMm: Float = 15f,
    val topMarginMm: Float = 20f,
    val bottomMarginMm: Float = 20f,
    val fontName: String = "Times New Roman",
    val fontSizePt: Float = 14f,
    val lineSpacing: Float = 1.5f,
    val paragraphIndentMm: Float = 12.5f,
    val startPageNumbering: Int = 2,
    val bibliographyFormat: String = "ГОСТ Р 7.0.100-2018",
    val isDefault: Boolean = false
) {
    fun toDomain(): GostProfile {
        return GostProfile(
            id = id,
            name = name,
            universityName = universityName,
            facultyName = facultyName,
            departmentName = departmentName,
            leftMarginMm = leftMarginMm,
            rightMarginMm = rightMarginMm,
            topMarginMm = topMarginMm,
            bottomMarginMm = bottomMarginMm,
            fontName = fontName,
            fontSizePt = fontSizePt,
            lineSpacing = lineSpacing,
            paragraphIndentMm = paragraphIndentMm,
            startPageNumbering = startPageNumbering,
            bibliographyFormat = bibliographyFormat,
            isDefault = isDefault
        )
    }

    companion object {
        fun fromDomain(profile: GostProfile): UniversityProfileEntity {
            return UniversityProfileEntity(
                id = profile.id,
                name = profile.name,
                universityName = profile.universityName,
                facultyName = profile.facultyName,
                departmentName = profile.departmentName,
                leftMarginMm = profile.leftMarginMm,
                rightMarginMm = profile.rightMarginMm,
                topMarginMm = profile.topMarginMm,
                bottomMarginMm = profile.bottomMarginMm,
                fontName = profile.fontName,
                fontSizePt = profile.fontSizePt,
                lineSpacing = profile.lineSpacing,
                paragraphIndentMm = profile.paragraphIndentMm,
                startPageNumbering = profile.startPageNumbering,
                bibliographyFormat = profile.bibliographyFormat,
                isDefault = profile.isDefault
            )
        }
    }
}
