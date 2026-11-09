package com.example.presentation.generator

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.GenerationProgress
import com.example.domain.model.GenerationStage
import com.example.domain.model.WorkSection
import com.example.domain.model.WorkType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel,
    onNavigateToSubscription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    // 2.6. Диалог «Нужна подписка»
    if (uiState.showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSubscriptionDialog() },
            title = {
                Text(
                    text = "Нужна подписка",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Нужна активная подписка. Оформите в Профиле → Подписка")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSubscriptionDialog()
                        onNavigateToSubscription()
                    },
                    modifier = Modifier.testTag("dialog_btn_subscribe")
                ) {
                    Text("Оформить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissSubscriptionDialog() },
                    modifier = Modifier.testTag("dialog_btn_cancel")
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StudTool",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ГОСТ 7.32",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Последовательный квантованный пайплайн LLM",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = !showSettingsDialog },
                        modifier = Modifier.testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки API",
                            tint = if (showSettingsDialog) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("generator_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Диалог / блок настроек API LLM (Vibe Code / Gemini Flash / DeepSeek)
            AnimatedVisibility(visible = showSettingsDialog) {
                LlmSettingsCard(
                    settings = uiState.settings,
                    onEndpointChange = { viewModel.setEndpoint(it) },
                    onApiKeyChange = { viewModel.setApiKey(it) },
                    onModelChange = { viewModel.setModel(it) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 4.5. Баннер продолжения прерванной генерации из Room GenerationSession
            if (uiState.savedSession != null && !uiState.isGenerating && uiState.error == null) {
                val saved = uiState.savedSession!!
                val nextStage = (saved.currentStage + 1).coerceAtMost(saved.totalStages)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("saved_session_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Найдена незавершённая генерация",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Сохранено этапов: ${saved.currentStage} из ${saved.totalStages}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.startGeneration(
                                        startFromStage = nextStage,
                                        resumeSessionId = saved.id
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Продолжить с этапа $nextStage")
                            }
                            OutlinedButton(onClick = { viewModel.dismissSavedSession() }) {
                                Text("Сбросить")
                            }
                        }
                    }
                }
            }

            // 5.2. Блок ошибки на этапе генерации с 3 кнопками
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("stage_error_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.retryFromFailedStage() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_retry_from_stage")
                            ) {
                                Text("Повторить с этого этапа", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_open_api_settings")
                            ) {
                                Text("Открыть настройки API", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("btn_cancel_error")
                        ) {
                            Text("Отмена", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // 1. ВВОД ТЕМЫ
            Text(
                text = "ТЕМА НАУЧНО-ИССЛЕДОВАТЕЛЬСКОЙ РАБОТЫ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = uiState.topic,
                onValueChange = { viewModel.setTopic(it) },
                enabled = !uiState.isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_topic"),
                placeholder = { Text("Например: Применение методов искусственного интеллекта в банковском скоринге") },
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. ВЫБОР ТИПА РАБОТЫ
            Text(
                text = "ТИП АКАДЕМИЧЕСКОЙ РАБОТЫ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            WorkTypeDropdown(
                selectedType = uiState.workType,
                onTypeSelected = { viewModel.setWorkType(it) },
                enabled = !uiState.isGenerating
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. СЛАЙДЕР СТРАНИЦ И ШАГ 1: КВАНТОВАНИЕ ОБЪЕМА
            QuantizationConfigCard(
                targetPages = uiState.targetPages,
                minPages = uiState.workType.minPages,
                maxPages = uiState.workType.maxPages,
                plan = uiState.quantizedPlan,
                onPagesChange = { viewModel.setTargetPages(it) },
                enabled = !uiState.isGenerating
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. КНОПКА ЗАПУСКА ПОСЛЕДОВАТЕЛЬНОЙ ГЕНЕРАЦИИ
            Button(
                onClick = { viewModel.startGeneration() },
                enabled = !uiState.isGenerating && uiState.topic.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_start_generation"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Генерация (${uiState.progress.overallPercentage}%)...",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Сгенерировать работу по ГОСТ (${uiState.targetPages} стр.)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // 5.1. КАРТОЧКА ПРОГРЕССА (ЭТАП 5: ПРОГРЕСС ГЕНЕРАЦИИ В UI)
            if (uiState.isGenerating || uiState.progress.stage !is GenerationStage.Idle) {
                Spacer(modifier = Modifier.height(18.dp))
                GenerationProgressCard(
                    progress = uiState.progress,
                    checklist = uiState.stageChecklist,
                    isGenerating = uiState.isGenerating,
                    onCancel = { viewModel.cancelGeneration() }
                )
            }

            // 6. ГОТОВЫЙ ДОКУМЕНТ И ЭКСПОРТ В .DOCX
            if (uiState.currentDocument != null) {
                Spacer(modifier = Modifier.height(20.dp))
                DocumentResultCard(
                    document = uiState.currentDocument!!,
                    docxPath = uiState.docxFilePath,
                    onOpenDocx = { path ->
                        openDocxFile(context, path)
                    },
                    onRecompileDocx = {
                        viewModel.compileDocx()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuantizationConfigCard(
    targetPages: Int,
    minPages: Int,
    maxPages: Int,
    plan: com.example.domain.quantization.SectionQuantizer.QuantizedPlan,
    onPagesChange: (Int) -> Unit,
    enabled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ШАГ 1: КВАНТОВАНИЕ ОБЪЕМА (ГОСТ)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$targetPages стр. (~${plan.totalTargetCharacters} знаков)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = targetPages.toFloat(),
                onValueChange = { onPagesChange(it.toInt()) },
                valueRange = minPages.toFloat()..maxPages.toFloat(),
                steps = (maxPages - minPages - 1).coerceAtLeast(0),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("slider_pages")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(25, 35, 40, 50, 65).filter { it in minPages..maxPages }.forEach { preset ->
                    Surface(
                        color = if (targetPages == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable(enabled = enabled) { onPagesChange(preset) }
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "$preset стр",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (targetPages == preset) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerationProgressCard(
    progress: GenerationProgress,
    checklist: List<StageCheckItem>,
    isGenerating: Boolean,
    onCancel: () -> Unit
) {
    val totalStages = checklist.size.coerceAtLeast(progress.totalSections).coerceAtLeast(1)
    val currentStage = progress.currentSectionIndex.coerceIn(1, totalStages)
    val stageTitle = progress.currentSectionTitle.ifBlank { "Введение" }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generation_progress_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Крупные цифры процента: (current / total) * 100
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "ПРОГРЕСС ГЕНЕРАЦИИ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$stageTitle · Этап $currentStage из $totalStages",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "${progress.overallPercentage}%",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 42.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.overallPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Список выполненных этапов с галочками
            Text(
                text = "ЭТАПЫ РАБОТЫ:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            checklist.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                item.isDone -> Icons.Default.CheckCircle
                                item.isError -> Icons.Default.ErrorOutline
                                else -> Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = when {
                                item.isDone -> Color(0xFF1E6B47)
                                item.isError -> MaterialTheme.colorScheme.error
                                item.isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${item.index}. ${item.label}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (item.isCurrent || item.isDone) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Text(
                        text = when {
                            item.isDone -> "✓"
                            item.isError -> "Ошибка"
                            item.isCurrent -> "В работе..."
                            else -> "Ожидание"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                item.isDone -> Color(0xFF1E6B47)
                                item.isError -> MaterialTheme.colorScheme.error
                                item.isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }
            }

            if (isGenerating) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_cancel_generation")
                ) {
                    Text("Отменить")
                }
            }
        }
    }
}

@Composable
private fun DocumentResultCard(
    document: com.example.domain.model.WorkDocument,
    docxPath: String?,
    onOpenDocx: (String) -> Unit,
    onRecompileDocx: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Работа успешно сформирована!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Объем: ${document.meta.targetPages} стр. (${document.sections.size} разделов)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (docxPath != null) {
                Button(
                    onClick = { onOpenDocx(docxPath) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Открыть готовый Word (.docx)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Файл сохранен: $docxPath",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                Button(
                    onClick = onRecompileDocx,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Скомпилировать в .DOCX")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "СГЕНЕРИРОВАННЫЕ РАЗДЕЛЫ:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            document.sections.forEach { section ->
                SectionPreviewItem(section = section)
            }
        }
    }
}

@Composable
private fun SectionPreviewItem(section: WorkSection) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .clickable { expanded = !expanded }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${section.content.length} знаков",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (expanded && section.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = section.content.take(600) + if (section.content.length > 600) "..." else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun WorkTypeDropdown(
    selectedType: WorkType,
    onTypeSelected: (WorkType) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedType.titleRu,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                IconButton(onClick = { if (enabled) expanded = !expanded }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Выбрать")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }
                .testTag("dropdown_work_type")
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            WorkType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = type.titleRu, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Стандарт: ${type.defaultPages} стр. (${type.minPages} - ${type.maxPages} стр.)",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LlmSettingsCard(
    settings: com.example.domain.model.LlmSettings,
    onEndpointChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "КОНФИГУРАЦИЯ LLM (VIBE CODE / GEMINI / DEEPSEEK)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = settings.endpoint,
                onValueChange = onEndpointChange,
                label = { Text("API Endpoint URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = settings.model,
                onValueChange = onModelChange,
                label = { Text("Model ID (e.g. vibe-code-fast, gemini-1.5-flash)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key (Bearer Token)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

private fun openDocxFile(context: android.content.Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Открыть Word документ"))
    } catch (e: Exception) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Поделиться документом"))
        } catch (_: Exception) {}
    }
}
