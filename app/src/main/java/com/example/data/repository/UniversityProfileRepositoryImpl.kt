package com.example.data.repository

import com.example.domain.model.UniversityProfile
import com.example.domain.repository.UniversityProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UniversityProfileRepositoryImpl : UniversityProfileRepository {

    private val profiles = mutableListOf(
        UniversityProfile(
            id = "gost_default",
            fullName = "Министерство науки и высшего образования Российской Федерации",
            shortName = "ГОСТ 7.32-2017 (Стандарт РФ)",
            faculty = "Общеуниверситетский факультет",
            department = "Кафедра академических исследований",
            city = "Москва",
            standard = "ГОСТ 7.32-2017 · ГОСТ Р 7.0.100-2018"
        ),
        UniversityProfile(
            id = "bmstu",
            fullName = "Московский государственный технический университет имени Н.Э. Баумана",
            shortName = "МГТУ им. Н.Э. Баумана",
            faculty = "Информатика и системы управления",
            department = "Программная инженерия и ИИ",
            city = "Москва",
            standard = "ГОСТ 7.32-2017 (СТО МГТУ)"
        ),
        UniversityProfile(
            id = "msu",
            fullName = "Московский государственный университет имени М.В. Ломоносова",
            shortName = "МГУ им. М.В. Ломоносова",
            faculty = "Факультет вычислительной математики и кибернетики",
            department = "Системное программирование",
            city = "Москва",
            standard = "ГОСТ Р 7.0.11-2011 / ВАК РФ"
        ),
        UniversityProfile(
            id = "hse",
            fullName = "Национальный исследовательский университет «Высшая школа экономики»",
            shortName = "НИУ ВШЭ",
            faculty = "Факультет компьютерных наук",
            department = "Департамент анализа данных и ИИ",
            city = "Москва",
            standard = "ГОСТ 7.32-2017 / Регламент ВКР ВШЭ"
        ),
        UniversityProfile(
            id = "spbu",
            fullName = "Санкт-Петербургский государственный университет",
            shortName = "СПбГУ",
            faculty = "Математико-механический факультет",
            department = "Информационно-аналитические системы",
            city = "Санкт-Петербург",
            standard = "ГОСТ 7.32-2017 (Стандарт СПбГУ)"
        ),
        UniversityProfile(
            id = "gpk",
            fullName = "ПРОФЕССИОНАЛЬНОЕ ОБРАЗОВАТЕЛЬНОЕ ЧАСТНОЕ УЧРЕЖДЕНИЕ «ГУМАНИТАРНО-ПЕДАГОГИЧЕСКИЙ КОЛЛЕДЖ»",
            shortName = "ГПК (Гуманитарно-педагогический колледж)",
            faculty = "Отделение информационных технологий и педагогики",
            department = "Предметно-цикловая комиссия",
            city = "Махачкала",
            standard = "ГОСТ 7.32-2017 · ГОСТ Р 7.0.100-2018"
        )
    )

    private val selectedFlow = MutableStateFlow(profiles.first())

    override fun getProfiles(): List<UniversityProfile> = profiles.toList()

    override fun getSelectedProfile(): Flow<UniversityProfile> = selectedFlow.asStateFlow()

    override suspend fun selectProfile(id: String) {
        profiles.find { it.id == id }?.let {
            selectedFlow.value = it
        }
    }

    override suspend fun addCustomProfile(profile: UniversityProfile) {
        profiles.add(profile)
        selectedFlow.value = profile
    }
}
