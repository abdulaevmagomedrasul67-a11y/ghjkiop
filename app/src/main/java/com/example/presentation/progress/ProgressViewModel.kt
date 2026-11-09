package com.example.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.StagePlanner
import com.example.domain.model.WorkRequest
import com.example.domain.repository.WorkRepository
import com.example.domain.usecase.BuildDocxUseCase
import com.example.domain.usecase.GenerateWorkUseCase
import com.example.domain.usecase.StageExecutionException
import com.example.ui.components.StageStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StageProgressItem(
    val id: String,
    val label: String,
    val status: StageStatus = StageStatus.WAITING
)

data class ProgressUiState(
    val stages: List<StageProgressItem> = emptyList(),
    val currentStageIndex: Int = 1,
    val totalStages: Int = 1,
    val currentStageLabel: String = "Инициализация",
    val progressPercent: Float = 0.05f,
    val statusMessage: String = "Подготовка к генерации...",
    val tipMessage: String = "По ГОСТ 7.32 в конце названий глав и разделов точка не ставится.",
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val errorDetail: String? = null,
    val failedStageIndex: Int = 1,
    val sessionId: String? = null,
    val isErrorDetailExpanded: Boolean = false,
    val generatedWorkId: Long? = null
)

class ProgressViewModel(
    private val generateWorkUseCase: GenerateWorkUseCase,
    private val buildDocxUseCase: BuildDocxUseCase,
    private val workRepository: WorkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    private val tips = listOf(
        "По ГОСТ 7.32 в конце названий глав и параграфов точка никогда не ставится.",
        "Красная строка (абзацный отступ) по стандарту составляет ровно 1.25 см.",
        "Каждая глава работы первого уровня должна начинаться с новой страницы.",
        "Список использованных источников оформляется по правилам ГОСТ Р 7.0.100-2018.",
        "Все пункты в оглавлении StudTool автоматически кликабельны в Word и LibreOffice.",
        "Стандартные поля для подшивки: левое 30 мм, правое 15 мм, верхнее и нижнее 20 мм."
    )

    fun startGeneration(request: WorkRequest, startFromStage: Int = 1, resumeSessionId: String? = null) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                val plannedStages = StagePlanner.plan(request.type, request.targetPages)
                val totalSteps = plannedStages.size

                val initialStageItems = plannedStages.mapIndexed { idx, st ->
                    val num = idx + 1
                    StageProgressItem(
                        id = st.id,
                        label = st.label,
                        status = when {
                            num < startFromStage -> StageStatus.DONE
                            num == startFromStage -> StageStatus.IN_PROGRESS
                            else -> StageStatus.WAITING
                        }
                    )
                }

                val firstLabel = plannedStages.getOrNull(startFromStage - 1)?.label ?: "Введение"
                val initPct = (startFromStage.toFloat() / totalSteps.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)

                _uiState.value = ProgressUiState(
                    stages = initialStageItems,
                    currentStageIndex = startFromStage,
                    totalStages = totalSteps,
                    currentStageLabel = firstLabel,
                    progressPercent = initPct,
                    statusMessage = "$firstLabel · Этап $startFromStage из $totalSteps",
                    isError = false,
                    errorMessage = null,
                    errorDetail = null,
                    isErrorDetailExpanded = false
                )
                startTipsRotation()

                val llmResult = generateWorkUseCase(
                    request = request,
                    startFromStage = startFromStage,
                    existingSessionId = resumeSessionId,
                    onProgress = { stageIndex, totalLlmStages, stageLabel ->
                        val updatedStages = _uiState.value.stages.mapIndexed { idx, item ->
                            when {
                                idx + 1 < stageIndex -> item.copy(status = StageStatus.DONE)
                                idx + 1 == stageIndex -> item.copy(status = StageStatus.IN_PROGRESS)
                                else -> item.copy(status = StageStatus.WAITING)
                            }
                        }

                        val progressFraction = (stageIndex.toFloat() / totalLlmStages.coerceAtLeast(1)).coerceIn(0.08f, 0.95f)
                        _uiState.value = _uiState.value.copy(
                            stages = updatedStages,
                            currentStageIndex = stageIndex,
                            totalStages = totalLlmStages,
                            currentStageLabel = stageLabel,
                            progressPercent = progressFraction,
                            statusMessage = "$stageLabel · Этап $stageIndex из $totalLlmStages"
                        )
                    }
                )

                if (llmResult.isFailure) {
                    val ex = llmResult.exceptionOrNull()
                    val errorStages = _uiState.value.stages.map {
                        if (it.status == StageStatus.IN_PROGRESS) it.copy(status = StageStatus.ERROR) else it
                    }

                    val mainMessage: String
                    val detailMessage: String
                    var failedIdx = _uiState.value.currentStageIndex
                    var savedSessionId: String? = null

                    if (ex is StageExecutionException) {
                        mainMessage = ex.mainMessage
                        failedIdx = ex.stageIndex
                        savedSessionId = ex.sessionId
                        detailMessage = buildString {
                            append("ЭТАП: ").append(ex.stageLabel).append("\n\n")
                            append("ОШИБКА: ").append(ex.mainMessage).append("\n\n")
                            if (!ex.lastResponseTail.isNullOrBlank()) {
                                append("ПОСЛЕДНИЕ СИМВОЛЫ ОТВЕТА МОДЕЛИ:\n")
                                append(ex.lastResponseTail)
                            }
                        }
                    } else {
                        mainMessage = ex?.localizedMessage ?: "Ошибка генерации текста"
                        detailMessage = ex?.stackTraceToString() ?: "Детали ошибки отсутствуют"
                    }

                    _uiState.value = _uiState.value.copy(
                        stages = errorStages,
                        isError = true,
                        errorMessage = mainMessage,
                        errorDetail = detailMessage,
                        failedStageIndex = failedIdx,
                        sessionId = savedSessionId
                    )
                    return@launch
                }

                val document = llmResult.getOrThrow()

                val docxResult = buildDocxUseCase(document, request.profileId)
                val docxFile = docxResult.getOrNull()

                val docWithDocx = document.copy(
                    docxFilePath = docxFile?.absolutePath,
                    docxPath = docxFile?.absolutePath
                )
                val savedId = workRepository.saveWork(docWithDocx)

                val completedStages = _uiState.value.stages.map { it.copy(status = StageStatus.DONE) }
                _uiState.value = _uiState.value.copy(
                    stages = completedStages,
                    currentStageIndex = totalSteps,
                    currentStageLabel = "Готово!",
                    progressPercent = 1.0f,
                    statusMessage = "Работа успешно готова к сдаче!",
                    generatedWorkId = savedId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    errorMessage = e.localizedMessage ?: "Непредвиденная ошибка",
                    errorDetail = e.stackTraceToString()
                )
            }
        }
    }

    fun retryFromFailedStage(request: WorkRequest) {
        val state = _uiState.value
        startGeneration(
            request = request,
            startFromStage = state.failedStageIndex.coerceAtLeast(1),
            resumeSessionId = state.sessionId
        )
    }

    fun toggleErrorDetail() {
        _uiState.value = _uiState.value.copy(
            isErrorDetailExpanded = !_uiState.value.isErrorDetailExpanded
        )
    }

    private fun startTipsRotation() {
        viewModelScope.launch {
            var index = 0
            while (_uiState.value.generatedWorkId == null && !_uiState.value.isError) {
                delay(5000)
                index = (index + 1) % tips.size
                _uiState.value = _uiState.value.copy(tipMessage = tips[index])
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        val errorStages = _uiState.value.stages.map {
            if (it.status == StageStatus.IN_PROGRESS) it.copy(status = StageStatus.ERROR) else it
        }
        _uiState.value = _uiState.value.copy(
            stages = errorStages,
            isError = true,
            errorMessage = "Генерация отменена пользователем"
        )
    }

    class Factory(
        private val generateWorkUseCase: GenerateWorkUseCase,
        private val buildDocxUseCase: BuildDocxUseCase,
        private val workRepository: WorkRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(generateWorkUseCase, buildDocxUseCase, workRepository) as T
        }
    }
}
