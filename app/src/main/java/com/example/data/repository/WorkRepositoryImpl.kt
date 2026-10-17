package com.example.data.repository

import com.example.domain.model.SectionType
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkSection
import com.example.domain.model.WorkType
import com.example.domain.repository.WorkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class WorkRepositoryImpl : WorkRepository {

    private val worksFlow = MutableStateFlow<List<WorkDocument>>(
        listOf(
            WorkDocument(
                id = 101L,
                meta = WorkMeta(
                    topic = "Архитектура современных больших языковых моделей и методы квантования контекста",
                    type = WorkType.COURSEWORK,
                    targetPages = 30,
                    university = "Московский государственный технический университет имени Н.Э. Баумана",
                    faculty = "Факультет информатики и систем управления",
                    department = "Кафедра программной инженерии",
                    discipline = "Интеллектуальные информационные системы",
                    authorName = "Абдулаев А.М.",
                    supervisorName = "д.т.н., проф. Соколов В.И.",
                    city = "Москва",
                    year = 2026
                ),
                sections = listOf(
                    WorkSection(
                        id = "sec_intro",
                        title = "ВВЕДЕНИЕ",
                        type = SectionType.INTRODUCTION,
                        targetPages = 3.0,
                        targetCharacters = 3500,
                        minCharacters = 3000,
                        maxCharacters = 4200,
                        content = "Актуальность темы исследования обусловлена стремительным развитием трансформерных архитектур и необходимостью генерации структурированных академических текстов большого объёма с соблюдением государственных стандартов оформления ГОСТ 7.32-2017.\n\nЦелью курсовой работы является исследование и программная реализация конвейера поэтапного квантования объёма текста со сжатием промежуточного контекста.",
                        isGenerated = true
                    ),
                    WorkSection(
                        id = "sec_ch1",
                        title = "1. ТЕОРЕТИЧЕСКИЕ ОСНОВЫ ТРАНСФОРМЕРНЫХ МОДЕЛЕЙ",
                        type = SectionType.CHAPTER,
                        chapterNumber = 1,
                        targetPages = 12.0,
                        targetCharacters = 12000,
                        minCharacters = 10000,
                        maxCharacters = 14000,
                        content = "Механизм самовнимания (Self-Attention) позволяет модели учитывать зависимости между токенами на произвольном расстоянии внутри контекстного окна. Однако при генерации длинных академических документов прямое формирование всего текста за один запрос приводит к преждевременному завершению генерации.\n\nПоэтапная декомпозиция документа на квантованные разделы решает проблему потери когерентности и обеспечивает точное соответствие целевому объёму в страницах.",
                        isGenerated = true
                    ),
                    WorkSection(
                        id = "sec_conclusion",
                        title = "ЗАКЛЮЧЕНИЕ",
                        type = SectionType.CONCLUSION,
                        targetPages = 3.0,
                        targetCharacters = 3500,
                        minCharacters = 3000,
                        maxCharacters = 4200,
                        content = "В ходе выполнения курсовой работы были проанализированы алгоритмы квантования академического объёма и разработан модуль автоматической вёрстки документов в формате Office Open XML (.docx) по стандарту ГОСТ 7.32-2017.",
                        isGenerated = true
                    ),
                    WorkSection(
                        id = "sec_biblio",
                        title = "СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ",
                        type = SectionType.BIBLIOGRAPHY,
                        targetPages = 2.0,
                        targetCharacters = 2500,
                        minCharacters = 2000,
                        maxCharacters = 3200,
                        content = "1. ГОСТ 7.32-2017. Система стандартов по информации, библиотечному и издательскому делу. Отчёт о научно-исследовательской работе. Структура и правила оформления. — М.: Стандартинформ, 2017. — 32 с.\n2. ГОСТ Р 7.0.100-2018. Библиографическая запись. Библиографическое описание. Общие требования и правила составления. — М.: Стандартинформ, 2018. — 124 с.",
                        isGenerated = true
                    )
                ),
                createdAt = System.currentTimeMillis() - 3600_000L
            )
        )
    )

    override fun getAllWorks(): Flow<List<WorkDocument>> = worksFlow.asStateFlow()

    override fun getWorksByType(type: WorkType): Flow<List<WorkDocument>> =
        worksFlow.map { list -> list.filter { it.meta.type == type } }

    override suspend fun getWorkById(id: Long): WorkDocument? =
        worksFlow.value.firstOrNull { it.id == id }

    override suspend fun saveWork(document: WorkDocument): Long {
        val current = worksFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == document.id }
        if (existingIndex >= 0) {
            current[existingIndex] = document
        } else {
            current.add(0, document)
        }
        worksFlow.value = current
        return document.id
    }

    override suspend fun updateDocxPath(workId: Long, path: String) {
        val current = worksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == workId }
        if (index >= 0) {
            val updated = current[index].copy(docxPath = path)
            current[index] = updated
            worksFlow.value = current
        }
    }

    override suspend fun deleteWork(id: Long) {
        val current = worksFlow.value.filter { it.id != id }
        worksFlow.value = current
    }
}
