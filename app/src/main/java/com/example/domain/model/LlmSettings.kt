package com.example.domain.model

data class LlmSettings(
    val endpoint: String = "https://api.vibecode.online/v1/chat/completions",
    val apiKey: String = "",
    val model: String = "vibe-code-pro-flash",
    val timeout: Long = 90L,
    val temperature: Double = 0.65,
    val topP: Double = 0.95
)

sealed class GenerationStage {
    data object Idle : GenerationStage()
    data class Quantizing(val totalPages: Int, val totalTargetChars: Int) : GenerationStage()
    data class GeneratingSection(
        val sectionIndex: Int,
        val totalSections: Int,
        val sectionTitle: String,
        val targetChars: Int
    ) : GenerationStage()
    data class CompressingContext(val sectionTitle: String) : GenerationStage()
    data class CompilingDocx(val path: String) : GenerationStage()
    data class Completed(val document: WorkDocument) : GenerationStage()
    data class Failed(val error: String) : GenerationStage()
}

data class GenerationProgress(
    val stage: GenerationStage = GenerationStage.Idle,
    val currentSectionIndex: Int = 0,
    val totalSections: Int = 0,
    val currentSectionTitle: String = "",
    val currentSectionTargetChars: Int = 0,
    val currentSectionGeneratedChars: Int = 0,
    val overallPercentage: Int = 0,
    val statusMessage: String = "Готов к генерации"
)
