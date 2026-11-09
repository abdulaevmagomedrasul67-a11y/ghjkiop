package com.example.presentation.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.GenerationSessionDao
import com.example.data.local.entity.GenerationSession
import com.example.data.local.security.SessionManager
import com.example.domain.model.GenerationProgress
import com.example.domain.model.GenerationStage
import com.example.domain.model.LlmSettings
import com.example.domain.model.StagePlanner
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import com.example.domain.quantization.SectionQuantizer
import com.example.domain.repository.WorkRepository
import com.example.domain.usecase.BuildDocxUseCase
import com.example.domain.usecase.GenerateWorkUseCase
import com.example.domain.usecase.StageExecutionException
import com.example.domain.usecase.SubscriptionRequiredException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StageCheckItem(
    val index: Int,
    val label: String,
    val isDone: Boolean = false,
    val isCurrent: Boolean = false,
    val isError: Boolean = false
)

data class GeneratorUiState(
    val topic: String = "Разработка интеллектуальной системы анализа данных на основе методов машинного обучения",
    val workType: WorkType = WorkType.COURSEWORK,
    val targetPages: Int = 35,
    val university: String = "Московский государственный технический университет",
    val authorName: String = "Иванов И.И.",
    val supervisorName: String = "д.т.н., проф. Смирнов А.В.",
    val settings: LlmSettings = LlmSettings(),
    val progress: GenerationProgress = GenerationProgress(),
    val stageChecklist: List<StageCheckItem> = emptyList(),
    val isGenerating: Boolean = false,
    val currentDocument: WorkDocument? = null,
    val docxFilePath: String? = null,
    val error: String? = null,
    val failedStageIndex: Int? = null,
    val failedStageLabel: String? = null,
    val activeSessionId: String? = null,
    val savedSession: GenerationSession? = null,
    val showSubscriptionDialog: Boolean = false,
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
    private val sessionManager: SessionManager? = null,
    private val generationSessionDao: GenerationSessionDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        sessionManager?.loadLlmSettings()?.let { savedSettings ->
            _uiState.update { it.copy(settings = savedSettings) }
        }
        checkSavedGenerationSession()
        rebuildStageChecklist()
    }

    private fun checkSavedGenerationSession() {
        viewModelScope.launch {
            val latest = generationSessionDao?.getLatestSession()
            if (latest != null && latest.currentStage in 1 until latest.totalStages) {
                _uiState.update {
                    it.copy(
                        savedSession = latest,
                        activeSessionId = latest.id
                    )
                }
            }
        }
    }

    private fun rebuildStageChecklist() {
        val state = _uiState.value
        val stages = StagePlanner.plan(state.workType, state.targetPages)
        val items = stages.mapIndexed { idx, st ->
            StageCheckItem(
                index = idx + 1,
                label = st.label
            )
        }
        _uiState.update { it.copy(stageChecklist = items) }
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
        rebuildStageChecklist()
    }

    fun setTargetPages(pages: Int) {
        _uiState.update { current ->
            val bounded = maxOf(current.workType.minPages, minOf(pages, current.workType.maxPages))
            val updated = current.copy(targetPages = bounded)
            updated.copy(quantizedPlan = recalculatePlan(updated))
        }
        rebuildStageChecklist()
    }

    fun setApiKey(key: String) {
        _uiState.update { it.copy(settings = it.settings.copy(apiKey = key)) }
        sessionManager?.saveLlmSettings(_uiState.value.settings)
    }

    fun setEndpoint(url: String) {
        _uiState.update { it.copy(settings = it.settings.copy(endpoint = url)) }
        sessionManager?.saveLlmSettings(_uiState.value.settings)
    }

    fun setModel(model: String) {
        _uiState.update { it.copy(settings = it.settings.copy(model = model)) }
        sessionManager?.saveLlmSettings(_uiState.value.settings)
    }

    fun dismissSubscriptionDialog() {
        _uiState.update { it.copy(showSubscriptionDialog = false) }
    }

    fun dismissSavedSession() {
        viewModelScope.launch {
            _uiState.value.savedSession?.let { generationSessionDao?.deleteSession(it.id) }
            _uiState.update { it.copy(savedSession = null, activeSessionId = null) }
        }
    }

    private fun recalculatePlan(state: GeneratorUiState): SectionQuantizer.QuantizedPlan {
        return SectionQuantizer.quantize(
            WorkMeta(
                topic = state.topic,
                type = state.workType,
                targetPages = state.targetPages,
                university = state.university,
                authorName = state.authorName,
                supervisorName = state.supervisorName
            )
        )
    }

    fun startGeneration(startFromStage: Int = 1, resumeSessionId: String? = null) {
        val state = _uiState.value
        if (state.topic.isBlank()) {
            _uiState.update { it.copy(error = "Укажите тему студенческой работы") }
            return
        }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val userId = sessionManager?.getUserId()?.toInt() ?: 0
            if (userId > 0) {
                val canGen = generateWorkUseCase.canGenerate(userId)
                if (canGen.isFailure) {
                    _uiState.update {
                        it.copy(
                            showSubscriptionDialog = true,
                            isGenerating = false
                        )
                    }
                    return@launch
                }
            }

            val stages = StagePlanner.plan(state.workType, state.targetPages)
            val initialChecklist = stages.mapIndexed { idx, st ->
                val stageNum = idx + 1
                StageCheckItem(
                    index = stageNum,
                    label = st.label,
                    isDone = stageNum < startFromStage,
                    isCurrent = stageNum == startFromStage
                )
            }

            _uiState.update {
                it.copy(
                    isGenerating = true,
                    error = null,
                    failedStageIndex = null,
                    failedStageLabel = null,
                    docxFilePath = null,
                    currentDocument = null,
                    stageChecklist = initialChecklist
                )
            }

            val meta = WorkMeta(
                topic = state.topic.trim(),
                type = state.workType,
                targetPages = state.targetPages,
                university = state.university,
                authorName = state.authorName,
                supervisorName = state.supervisorName
            )

            val result = generateWorkUseCase(
                meta = meta,
                settings = state.settings,
                userId = userId,
                startFromStage = startFromStage,
                sessionId = resumeSessionId,
                onProgress = { p ->
                    val currentIdx = p.currentSectionIndex
                    val total = p.totalSections.coerceAtLeast(1)
                    val pct = ((currentIdx.toDouble() / total) * 100).toInt().coerceIn(5, 99)
                    _uiState.update { curr ->
                        val updatedList = curr.stageChecklist.map { item ->
                            item.copy(
                                isDone = item.index < currentIdx,
                                isCurrent = item.index == currentIdx,
                                isError = false
                            )
                        }
                        curr.copy(
                            progress = p.copy(overallPercentage = pct),
                            stageChecklist = updatedList
                        )
                    }
                }
            )

            result.onSuccess { doc ->
                workRepository.saveWork(doc)
                _uiState.update { curr ->
                    curr.copy(
                        isGenerating = false,
                        currentDocument = doc,
                        savedSession = null,
                        stageChecklist = curr.stageChecklist.map { it.copy(isDone = true, isCurrent = false) }
                    )
                }
                compileDocx(doc)
            }.onFailure { err ->
                if (err is SubscriptionRequiredException) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            showSubscriptionDialog = true
                        )
                    }
                    return@onFailure
                }

                val stageEx = err as? StageExecutionException
                val failedIdx = stageEx?.stageIndex ?: _uiState.value.progress.currentSectionIndex.coerceAtLeast(1)
                val failedLbl = stageEx?.stageLabel ?: _uiState.value.progress.currentSectionTitle.ifBlank { "Этап $failedIdx" }

                _uiState.update { curr ->
                    curr.copy(
                        isGenerating = false,
                        error = err.message ?: "Этап \"$failedLbl\": модель не уложилась в лимит",
                        failedStageIndex = failedIdx,
                        failedStageLabel = failedLbl,
                        activeSessionId = stageEx?.sessionId ?: curr.activeSessionId,
                        stageChecklist = curr.stageChecklist.map { item ->
                            item.copy(
                                isCurrent = false,
                                isError = item.index == failedIdx
                            )
                        },
                        progress = curr.progress.copy(
                            stage = GenerationStage.Failed(err.message ?: "Ошибка")
                        )
                    )
                }
            }
        }
    }

    fun retryFromFailedStage() {
        val state = _uiState.value
        val stageToRetry = state.failedStageIndex ?: (state.savedSession?.currentStage?.plus(1)) ?: 1
        val sessionId = state.activeSessionId ?: state.savedSession?.id
        startGeneration(startFromStage = stageToRetry, resumeSessionId = sessionId)
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _uiState.update {
            it.copy(
                isGenerating = false,
                error = "Генерация отменена пользователем"
            )
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
                            overallPercentage = 100,
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
        _uiState.update { it.copy(error = null, failedStageIndex = null, failedStageLabel = null) }
    }

    class Factory(
        private val generateWorkUseCase: GenerateWorkUseCase,
        private val buildDocxUseCase: BuildDocxUseCase,
        private val workRepository: WorkRepository,
        private val sessionManager: SessionManager? = null,
        private val generationSessionDao: GenerationSessionDao? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GeneratorViewModel(
                generateWorkUseCase = generateWorkUseCase,
                buildDocxUseCase = buildDocxUseCase,
                workRepository = workRepository,
                sessionManager = sessionManager,
                generationSessionDao = generationSessionDao
            ) as T
        }
    }
}
