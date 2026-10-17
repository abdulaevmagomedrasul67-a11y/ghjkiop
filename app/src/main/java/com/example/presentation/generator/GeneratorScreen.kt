package com.example.presentation.generator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.example.core.utils.FileSharingHelper
import com.example.domain.model.GenerationProgress
import com.example.domain.model.GenerationStage
import com.example.domain.model.UniversityProfile
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkSection
import com.example.domain.model.WorkType
import com.example.ui.components.AnimatedCoinBadge
import com.example.ui.components.StudToolBrandHeader
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudAmberDark
import com.example.ui.theme.StudAmberGold
import com.example.ui.theme.StudEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel,
    coinBalance: Int = 1500,
    onNavigateToCoins: () -> Unit = {},
    onPreviewWork: (WorkDocument) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTitlePageDetails by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    StudToolBrandHeader(badgeText = "ГОСТ 7.32")
                },
                actions = {
                    AnimatedCoinBadge(
                        balance = coinBalance,
                        onClick = onNavigateToCoins,
                        modifier = Modifier.padding(end = 4.dp)
                    )
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

            // Ошибка если есть
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ошибка: ${uiState.error}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Text("✕", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // 1. ВВОД ТЕМЫ
            Text(
                text = "ТЕМА НАУЧНО-ИССЛЕДОВАТЕЛЬСКОЙ РАБОТЫ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
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

            Spacer(modifier = Modifier.height(14.dp))

            // 2. ВЫБОР ТИПА РАБОТЫ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ТИП АКАДЕМИЧЕСКОЙ РАБОТЫ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                )
                Surface(
                    color = StudAmberGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Тариф: ${uiState.workType.priceSc} S-coin",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = StudAmberDark
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            WorkTypeDropdown(
                selectedType = uiState.workType,
                onTypeSelected = { viewModel.setWorkType(it) },
                enabled = !uiState.isGenerating
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2.5 ПРОФИЛЬ ВУЗА И ТИТУЛЬНЫЙ ЛИСТ ГОСТ
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTitlePageDetails = !showTitlePageDetails },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Профиль ВУЗа и титульный лист ГОСТ",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FrauncesFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${uiState.university} · ${uiState.authorName}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showTitlePageDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Развернуть"
                        )
                    }

                    if (showTitlePageDetails) {
                        Spacer(modifier = Modifier.height(10.dp))

                        val uniPresets = remember {
                            listOf(
                                UniversityProfile(
                                    id = "bmstu",
                                    fullName = "Московский государственный технический университет им. Н.Э. Баумана",
                                    shortName = "МГТУ им. Баумана",
                                    faculty = "Информатика и системы управления",
                                    department = "Программная инженерия и ИИ",
                                    city = "Москва"
                                ),
                                UniversityProfile(
                                    id = "msu",
                                    fullName = "Московский государственный университет имени М.В. Ломоносова",
                                    shortName = "МГУ",
                                    faculty = "Факультет ВМК",
                                    department = "Системное программирование",
                                    city = "Москва"
                                ),
                                UniversityProfile(
                                    id = "hse",
                                    fullName = "Национальный исследовательский университет «Высшая школа экономики»",
                                    shortName = "НИУ ВШЭ",
                                    faculty = "Факультет компьютерных наук",
                                    department = "Анализ данных и ИИ",
                                    city = "Москва"
                                ),
                                UniversityProfile(
                                    id = "spbu",
                                    fullName = "Санкт-Петербургский государственный университет",
                                    shortName = "СПбГУ",
                                    faculty = "Математико-механический факультет",
                                    department = "Информационные системы",
                                    city = "Санкт-Петербург"
                                ),
                                UniversityProfile(
                                    id = "gpk",
                                    fullName = "Гуманитарно-педагогический колледж",
                                    shortName = "ГПК",
                                    faculty = "Отделение информационных технологий",
                                    department = "Предметно-цикловая комиссия",
                                    city = "Махачкала"
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uniPresets.forEach { preset ->
                                FilterChip(
                                    selected = uiState.university == preset.shortName || uiState.university == preset.fullName,
                                    onClick = { viewModel.applyUniversityProfile(preset) },
                                    label = { Text(preset.shortName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.university,
                            onValueChange = { viewModel.setUniversity(it) },
                            label = { Text("Учебное заведение (ВУЗ / Колледж)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.faculty,
                            onValueChange = { viewModel.setFaculty(it) },
                            label = { Text("Факультет / Институт") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.department,
                            onValueChange = { viewModel.setDepartment(it) },
                            label = { Text("Кафедра и направление") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.authorName,
                                onValueChange = { viewModel.setAuthorName(it) },
                                label = { Text("ФИО студента") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.city,
                                onValueChange = { viewModel.setCity(it) },
                                label = { Text("Город") },
                                singleLine = true,
                                modifier = Modifier.weight(0.75f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.supervisorName,
                            onValueChange = { viewModel.setSupervisorName(it) },
                            label = { Text("Научный руководитель") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    .height(54.dp)
                    .testTag("btn_start_generation"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Итеративная генерация (${uiState.progress.overallPercentage}%)...",
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

            // 5. КАРТОЧКА ПРОГРЕССА (ШАГ 2 И ШАГ 3 В РЕАЛЬНОМ ВРЕМЕНИ)
            if (uiState.isGenerating || uiState.progress.stage !is GenerationStage.Idle) {
                Spacer(modifier = Modifier.height(18.dp))
                GenerationProgressCard(progress = uiState.progress)
            }

            // 6. ГОТОВЫЙ ДОКУМЕНТ И ЭКСПОРТ В .DOCX
            if (uiState.currentDocument != null) {
                Spacer(modifier = Modifier.height(20.dp))
                DocumentResultCard(
                    document = uiState.currentDocument!!,
                    docxPath = uiState.docxFilePath,
                    onPreviewWork = { onPreviewWork(uiState.currentDocument!!) },
                    onOpenDocx = { path ->
                        FileSharingHelper.openDocx(context, path)
                    },
                    onShareDocx = { path ->
                        FileSharingHelper.shareDocx(context, path, uiState.currentDocument!!.meta.topic)
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
        shape = RoundedCornerShape(12.dp),
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
                        fontFamily = JetBrainsMonoFontFamily,
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
                            fontFamily = JetBrainsMonoFontFamily,
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
                steps = maxOf(0, maxPages - minPages - 1),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("slider_pages")
            )

            // Быстрые пресеты страниц
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(12, 18, 25, 35, 45, 65, 80).filter { it in minPages..maxPages }.take(5).forEach { preset ->
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
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (targetPages == preset) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Таблица квантования разделов
            Text(
                text = "Математическое распределение объема по главам (~1800 зн./стр.):",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            plan.sections.forEach { section ->
                if (section.targetCharacters > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${section.targetPages} стр. (${section.targetCharacters} зн.)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerationProgressCard(progress: GenerationProgress) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ШАГ 2 И 3: ИТЕРАТИВНЫЙ ПАЙПЛАЙН",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Text(
                    text = "${progress.overallPercentage}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.overallPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = progress.statusMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )

            if (progress.currentSectionTargetChars > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Текущий раздел: ${progress.currentSectionTitle}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                    Text(
                        text = "Квота: ${progress.currentSectionTargetChars} знаков",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = StudEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Summary Relay активен: передается строго 3 предложения резюме, исключая переполнение контекста (context window overflow).",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentResultCard(
    document: WorkDocument,
    docxPath: String?,
    onPreviewWork: () -> Unit,
    onOpenDocx: (String) -> Unit,
    onShareDocx: (String) -> Unit,
    onRecompileDocx: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StudEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Работа успешно сформирована!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Объем: ${document.meta.targetPages} стр. (${document.sections.size} разделов)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Кнопка предпросмотра по ГОСТ
            OutlinedButton(
                onClick = onPreviewWork,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Предпросмотр титульного листа и глав ГОСТ", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопки открытия и отправки DOCX
            if (docxPath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOpenDocx(docxPath) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Открыть Word (.docx)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onShareDocx(docxPath) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Поделиться")
                    }
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

            // Список сгенерированных разделов
            Text(
                text = "СГЕНЕРИРОВАННЫЕ РАЗДЕЛЫ:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
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
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
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
                        fontFamily = JetBrainsMonoFontFamily,
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
                                text = "Стандарт: ${type.defaultPages} стр. (${type.minPages} - ${type.maxPages} стр.) · ${type.priceSc} S-coin",
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
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "КОНФИГУРАЦИЯ LLM (VIBE CODE / GEMINI / DEEPSEEK)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
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
                label = { Text("Model ID (e.g. vibe-code-fast, gemini-2.5-flash)") },
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
