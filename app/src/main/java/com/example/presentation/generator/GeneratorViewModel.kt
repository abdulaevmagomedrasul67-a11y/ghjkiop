package com.example.presentation.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.GenerationProgress
import com.example.domain.model.GenerationStage
import com.example.domain.model.LlmSettings
import com.example.domain.model.UniversityProfile
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import com.example.domain.quantization.SectionQuantizer
import com.example.domain.repository.CoinRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.UniversityProfileRepository
import com.example.domain.repository.WorkRepository
import com.example.domain.usecase.BuildDocxUseCase
import com.example.domain.usecase.GenerateWorkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeneratorUiState(
    val topic: String = "Разработка интеллектуальной системы анализа данных на основе методов машинного обучения",
    val workType: WorkType = WorkType.COURSEWORK,
    val targetPages: Int = 35,
    val university: String = "Московский государственный технический университет",
    val faculty: String = "Факультет информационных технологий и управления",
    val department: String = "Кафедра прикладных информационных систем",
    val discipline: String = "Интеллектуальные системы и анализ данных",
    val authorName: String = "Иванов И.И.",
    val supervisorName: String = "д.т.н., проф. Смирнов А.В.",
    val city: String = "Москва",
    val year: Int = 2026,
    val settings: LlmSettings = LlmSettings(),
    val progress: GenerationProgress = GenerationProgress(),
    val isGenerating: Boolean = false,
    val currentDocument: WorkDocument? = null,
    val docxFilePath: String? = null,
    val error: String? = null,
    val quantizedPlan: SectionQuantizer.QuantizedPlan = SectionQuantizer.quantize(
        WorkMeta(
            topic = "Разработка интеллектуальной системы анализа данных на основе методов машинного обучения",
            type = WorkType.COURSEWORK,
            targetPages = 35
        )
    )
)

class GeneratorViewModel(
    private val generateWorkUseCase: GenerateWorkUseCase,
    private val buildDocxUseCase: BuildDocxUseCase,
    private val workRepository: WorkRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val universityProfileRepository: UniversityProfileRepository? = null,
    private val coinRepository: CoinRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.getLlmSettings().collect { savedSettings ->
                    _uiState.update { it.copy(settings = savedSettings) }
                }
            }
        }
        universityProfileRepository?.let { repo ->
            viewModelScope.launch {
                repo.getSelectedProfile().collect { profile ->
                    applyUniversityProfile(profile)
                }
            }
        }
    }

    fun applyUniversityProfile(profile: UniversityProfile) {
        _uiState.update { current ->
            val updated = current.copy(
                university = profile.shortName.ifBlank { profile.fullName },
                faculty = profile.faculty.ifBlank { current.faculty },
                department = profile.department.ifBlank { current.department },
                city = profile.city.ifBlank { current.city }
            )
            updated.copy(quantizedPlan = recalculatePlan(updated))
        }
    }

    fun setTopic(topic: String) {
        _uiState.update { current ->
            val updated = current.copy(topic = topic)
            updated.copy(quantizedPlan = recalculatePlan(updated))
        }
    }

    fun setWorkType(type: WorkType) {
        _uiState.update { current ->
            val updatedPages = if (current.targetPages < type.minPages || current.targetPages > type.maxPages) {
                type.defaultPages
            } else {
                current.targetPages
            }
            val updated = current.copy(workType = type, targetPages = updatedPages)
            updated.copy(quantizedPlan = recalculatePlan(updated))
        }
    }

    fun setTargetPages(pages: Int) {
        _uiState.update { current ->
            val bounded = maxOf(current.workType.minPages, minOf(pages, current.workType.maxPages))
            val updated = current.copy(targetPages = bounded)
            updated.copy(quantizedPlan = recalculatePlan(updated))
        }
    }

    fun setUniversity(value: String) {
        _uiState.update { current ->
            val updated = current.copy(university = value)
            updated.copy(quantizedPlan = recalculatePlan(updated))
        }
    }

    fun setFaculty(value: String) {
        _uiState.update { it.copy(faculty = value) }
    }

    fun setDepartment(value: String) {
        _uiState.update { it.copy(department = value) }
    }

    fun setDiscipline(value: String) {
        _uiState.update { it.copy(discipline = value) }
    }

    fun setAuthorName(value: String) {
        _uiState.update { it.copy(authorName = value) }
    }

    fun setSupervisorName(value: String) {
        _uiState.update { it.copy(supervisorName = value) }
    }

    fun setCity(value: String) {
        _uiState.update { it.copy(city = value) }
    }

    fun setApiKey(key: String) {
        val updatedSettings = _uiState.value.settings.copy(apiKey = key)
        _uiState.update { it.copy(settings = updatedSettings) }
        viewModelScope.launch { settingsRepository?.saveLlmSettings(updatedSettings) }
    }

    fun setEndpoint(url: String) {
        val updatedSettings = _uiState.value.settings.copy(endpoint = url)
        _uiState.update { it.copy(settings = updatedSettings) }
        viewModelScope.launch { settingsRepository?.saveLlmSettings(updatedSettings) }
    }

    fun setModel(model: String) {
        val updatedSettings = _uiState.value.settings.copy(model = model)
        _uiState.update { it.copy(settings = updatedSettings) }
        viewModelScope.launch { settingsRepository?.saveLlmSettings(updatedSettings) }
    }

    private fun recalculatePlan(state: GeneratorUiState): SectionQuantizer.QuantizedPlan {
        return SectionQuantizer.quantize(
            WorkMeta(
                topic = state.topic,
                type = state.workType,
                targetPages = state.targetPages,
                university = state.university,
                faculty = state.faculty,
                department = state.department,
                discipline = state.discipline,
                authorName = state.authorName,
                supervisorName = state.supervisorName,
                city = state.city,
                year = state.year
            )
        )
    }

    fun startGeneration() {
        val state = _uiState.value
        if (state.topic.isBlank()) {
            _uiState.update { it.copy(error = "Укажите тему студенческой работы") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    error = null,
                    docxFilePath = null,
                    currentDocument = null
                )
            }

            val meta = WorkMeta(
                topic = state.topic.trim(),
                type = state.workType,
                targetPages = state.targetPages,
                university = state.university,
                faculty = state.faculty,
                department = state.department,
                discipline = state.discipline,
                authorName = state.authorName,
                supervisorName = state.supervisorName,
                city = state.city,
                year = state.year
            )

            val result = generateWorkUseCase(
                meta = meta,
                settings = state.settings,
                onProgress = { p ->
                    _uiState.update { it.copy(progress = p) }
                }
            )

            result.onSuccess { doc ->
                coinRepository?.deduct(state.workType.priceSc)
                workRepository.saveWork(doc)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        currentDocument = doc
                    )
                }
                compileDocx(doc)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = err.message ?: "Непредвиденная ошибка генерации",
                        progress = it.progress.copy(
                            stage = GenerationStage.Failed(err.message ?: "Ошибка")
                        )
                    )
                }
            }
        }
    }

    fun compileDocx(doc: WorkDocument? = null) {
        val documentToBuild = doc ?: _uiState.value.currentDocument ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progress = it.progress.copy(
                        stage = GenerationStage.CompilingDocx("Формирование Word документа (.docx)..."),
                        statusMessage = "Компиляция в ГОСТ .DOCX (поля 30/10/20/20 мм, 14pt, 1.5 интервал)..."
                    )
                )
            }

            val docxResult = buildDocxUseCase(documentToBuild)
            docxResult.onSuccess { path ->
                workRepository.updateDocxPath(documentToBuild.id, path)
                _uiState.update {
                    it.copy(
                        docxFilePath = path,
                        currentDocument = documentToBuild.copy(docxPath = path),
                        progress = it.progress.copy(
                            stage = GenerationStage.Completed(documentToBuild),
                            statusMessage = "Файл DOCX готов: $path"
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        error = "Ошибка компиляции DOCX: ${err.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val generateWorkUseCase: GenerateWorkUseCase,
        private val buildDocxUseCase: BuildDocxUseCase,
        private val workRepository: WorkRepository,
        private val settingsRepository: SettingsRepository? = null,
        private val universityProfileRepository: UniversityProfileRepository? = null,
        private val coinRepository: CoinRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GeneratorViewModel(
                generateWorkUseCase = generateWorkUseCase,
                buildDocxUseCase = buildDocxUseCase,
                workRepository = workRepository,
                settingsRepository = settingsRepository,
                universityProfileRepository = universityProfileRepository,
                coinRepository = coinRepository
            ) as T
        }
    }
}
