package com.example.domain.usecase

import com.example.domain.model.GenerationProgress
import com.example.domain.model.LlmSettings
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.pipeline.SequentialGenerationController

/**
 * UseCase последовательной квантованной генерации академической работы по ГОСТу
 * с эстафетой сжатия контекста (Summary Relay).
 */
class GenerateWorkUseCase(
    private val controller: SequentialGenerationController
) {
    suspend operator fun invoke(
        meta: WorkMeta,
        settings: LlmSettings,
        onProgress: (GenerationProgress) -> Unit = {}
    ): Result<WorkDocument> {
        return controller.generateWork(
            meta = meta,
            settings = settings,
            onProgress = onProgress
        )
    }
}
