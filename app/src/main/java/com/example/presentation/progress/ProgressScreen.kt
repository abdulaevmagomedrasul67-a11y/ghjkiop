package com.example.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WorkRequest
import com.example.ui.components.AppDivider
import com.example.ui.components.AppIcon
import com.example.ui.components.BottomActionBar
import com.example.ui.components.FlatProgressBar
import com.example.ui.components.GhostButton
import com.example.ui.components.MetaBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProgressStageRow
import com.example.ui.components.SectionLabel
import com.example.ui.components.StageStatus
import com.example.ui.components.StudToolTopBar
import com.example.ui.icons.Lucide
import com.example.ui.theme.Dimens
import com.example.ui.theme.StudToolTheme
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    workRequest: WorkRequest,
    onGenerationComplete: (Long) -> Unit,
    onCancel: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(workRequest) {
        viewModel.startGeneration(workRequest)
    }

    val colors = StudToolTheme.colors
    val typography = StudToolTheme.typography

    val percentInt = (uiState.progressPercent * 100).roundToInt().coerceIn(0, 100)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper)
            .testTag("progress_screen")
    ) {
        StudToolTopBar(
            title = "Генерация",
            navigationIcon = Lucide.ArrowLeft,
            onNavigationClick = {
                viewModel.cancelGeneration()
                onCancel()
            },
            testTag = "progress_top_bar"
        )

        MetaBar(
            leftText = "ПОЭТАПНАЯ ГЕНЕРАЦИЯ (CHUNKED)",
            rightText = "${workRequest.type.titleRu.uppercase()} · ${workRequest.targetPages} СТР"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.lg)
        ) {
            Text(
                text = workRequest.topic,
                style = typography.titleSerif.copy(color = colors.ink),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(Dimens.md))

            Text(
                text = uiState.statusMessage,
                style = typography.labelMono.copy(
                    color = if (uiState.isError) colors.accent else colors.inkSoft,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(Dimens.sm))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "$percentInt",
                    style = typography.displayLargeSerif.copy(
                        color = colors.ink,
                        fontSize = 92.sp,
                        lineHeight = 92.sp
                    )
                )
                Text(
                    text = "%",
                    style = typography.displaySerif.copy(
                        color = colors.accent,
                        fontStyle = FontStyle.Italic,
                        fontSize = 44.sp,
                        lineHeight = 52.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlatProgressBar(
                progress = uiState.progressPercent,
                testTag = "generation_progress_bar"
            )

            Spacer(modifier = Modifier.height(Dimens.betweenFieldGroups))
            AppDivider(color = colors.line)
            Spacer(modifier = Modifier.height(Dimens.betweenFieldGroups))

            if (uiState.isError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.accent.copy(alpha = 0.08f))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(
                                imageVector = Lucide.AlertCircle,
                                contentDescription = null,
                                tint = colors.accent,
                                size = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ОШИБКА НА ЭТАПЕ ГЕНЕРАЦИИ",
                                style = typography.labelMono.copy(color = colors.accent)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.errorMessage ?: "Произошла непредвиденная ошибка",
                            style = typography.bodySmall.copy(color = colors.inkSoft)
                        )

                        if (!uiState.errorDetail.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            GhostButton(
                                text = if (uiState.isErrorDetailExpanded) "Скрыть подробности" else "Подробнее об ошибке",
                                onClick = { viewModel.toggleErrorDetail() }
                            )

                            if (uiState.isErrorDetailExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.paperWarm)
                                        .border(width = 1.dp, color = colors.line)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = uiState.errorDetail!!,
                                        style = typography.codeMono.copy(fontSize = 11.sp, color = colors.ink),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.betweenFieldGroups))
            }

            SectionLabel(text = "ЭТАПЫ ВЫПОЛНЕНИЯ (${uiState.currentStageIndex} ИЗ ${uiState.totalStages})")

            uiState.stages.forEachIndexed { index, stageItem ->
                ProgressStageRow(
                    title = stageItem.label,
                    status = stageItem.status,
                    trailingTiming = when (stageItem.status) {
                        StageStatus.DONE -> "Готово ✓"
                        StageStatus.IN_PROGRESS -> "В работе..."
                        StageStatus.ERROR -> "Ошибка"
                        StageStatus.WAITING -> "В очереди"
                    },
                    showBottomDivider = index < uiState.stages.size - 1
                )
            }

            Spacer(modifier = Modifier.height(Dimens.betweenFieldGroups))
            AppDivider(color = colors.line)
            Spacer(modifier = Modifier.height(Dimens.betweenFieldGroups))

            SectionLabel(text = "ПАМЯТКА ГОСТ")
            Text(
                text = uiState.tipMessage,
                style = typography.bodySmall.copy(color = colors.inkSoft)
            )

            Spacer(modifier = Modifier.height(Dimens.betweenBlocks))
        }

        BottomActionBar {
            when {
                uiState.generatedWorkId != null -> {
                    PrimaryButton(
                        text = "Открыть готовую работу →",
                        onClick = { onGenerationComplete(uiState.generatedWorkId!!) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "progress_done_button"
                    )
                }
                uiState.isError -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimaryButton(
                            text = "Повторить с этого этапа",
                            onClick = { viewModel.retryFromFailedStage(workRequest) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_retry_from_stage"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GhostButton(
                                text = "Открыть настройки API",
                                onClick = onNavigateToSettings,
                                modifier = Modifier.weight(1f)
                            )
                            GhostButton(
                                text = "Отмена",
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                else -> {
                    GhostButton(
                        text = "Отменить",
                        onClick = {
                            viewModel.cancelGeneration()
                            onCancel()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "cancel_generation_button"
                    )
                }
            }
        }
    }
}
