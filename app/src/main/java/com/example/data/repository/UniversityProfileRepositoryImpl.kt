package com.example.data.repository

import com.example.domain.model.GostProfile
import com.example.domain.model.UniversityProfile
import com.example.domain.repository.UniversityProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UniversityProfileRepositoryImpl : UniversityProfileRepository {

    private val profiles = listOf(
        UniversityProfile(
            id = "bmstu",
            fullName = "Московский государственный технический университет имени Н.Э. Баумана",
            shortName = "МГТУ им. Н.Э. Баумана",
            faculty = "Информатика и системы управления",
            department = "Программная инженерия",
            city = "Москва",
            standard = "ГОСТ 7.32-2017 (ФГОС ВО 3++)"
        ),
        UniversityProfile(
            id = "msu",
            fullName = "Московский государственный университет имени М.В. Ломоносова",
            shortName = "МГУ им. М.В. Ломоносова",
            faculty = "Экономический факультет",
            department = "Финансы и кредит",
            city = "Москва",
            standard = "ГОСТ Р 7.0.11-2011"
        ),
        UniversityProfile(
            id = "spbu",
            fullName = "Санкт-Петербургский государственный университет",
            shortName = "СПбГУ",
            faculty = "Юридический факультет",
            department = "Гражданское право",
            city = "Санкт-Петербург",
            standard = "ГОСТ 7.32-2017"
        ),
        UniversityProfile(
            id = "hse",
            fullName = "Национальный исследовательский университет «Высшая школа экономики»",
            shortName = "НИУ ВШЭ",
            faculty = "Факультет компьютерных наук",
            department = "Анализ данных и искусственный интеллект",
            city = "Москва",
            standard = "ГОСТ 7.32-2017"
        )
    )

    private val gostProfiles = MutableStateFlow(
        listOf(
            GostProfile(
                id = 1L,
                name = "ГОСТ 7.32-2017 (Стандарт РФ)",
                universityName = "Министерство науки и высшего образования Российской Федерации",
                isDefault = true
            ),
            GostProfile(
                id = 2L,
                name = "МГУ им. М.В. Ломоносова",
                universityName = "Московский государственный университет имени М.В. Ломоносова",
                isDefault = false
            ),
            GostProfile(
                id = 3L,
                name = "НИУ ВШЭ (Высшая школа экономики)",
                universityName = "Национальный исследовательский университет «Высшая школа экономики»",
                isDefault = false
            ),
            GostProfile(
                id = 4L,
                name = "СПбГУ",
                universityName = "Санкт-Петербургский государственный университет",
                rightMarginMm = 10f,
                isDefault = false
            ),
            GostProfile(
                id = 5L,
                name = "МГТУ им. Н.Э. Баумана",
                universityName = "Московский государственный технический университет имени Н.Э. Баумана",
                isDefault = false
            ),
            GostProfile(
                id = 6L,
                name = "ГПК (Гуманитарно-педагогический колледж)",
                universityName = "ПРОФЕССИОНАЛЬНОЕ ОБРАЗОВАТЕЛЬНОЕ ЧАСТНОЕ УЧРЕЖДЕНИЕ «ГУМАНИТАРНО-ПЕДАГОГИЧЕСКИЙ КОЛЛЕДЖ»",
                isDefault = false
            )
        )
    )

    private val selectedFlow = MutableStateFlow(profiles.first())

    override fun getSelectedProfile(): Flow<UniversityProfile> = selectedFlow.asStateFlow()

    override suspend fun selectProfile(profile: UniversityProfile) {
        selectedFlow.value = profile
    }

    override suspend fun getAllProfiles(): List<UniversityProfile> = profiles

    override fun getAllGostProfiles(): Flow<List<GostProfile>> = gostProfiles.asStateFlow()

    override suspend fun getProfileById(id: Long): GostProfile? {
        return gostProfiles.value.firstOrNull { it.id == id }
    }

    override suspend fun getDefaultProfile(): GostProfile {
        return gostProfiles.value.firstOrNull { it.isDefault } ?: GostProfile.DEFAULT_GOST
    }

    override suspend fun insertProfile(profile: GostProfile): Long {
        val nextId = if (profile.id > 0L) profile.id else (gostProfiles.value.maxOfOrNull { it.id } ?: 1L) + 1L
        val withId = profile.copy(id = nextId)
        val current = gostProfiles.value.toMutableList()
        val idx = current.indexOfFirst { it.id == nextId }
        if (idx >= 0) current[idx] = withId else current.add(withId)
        gostProfiles.value = current
        return nextId
    }

    override suspend fun deleteProfile(id: Long) {
        gostProfiles.value = gostProfiles.value.filter { it.id != id }
    }
}
