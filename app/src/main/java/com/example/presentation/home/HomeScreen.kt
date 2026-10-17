package com.example.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.core.utils.FileSharingHelper
import com.example.data.hadith.HadithProvider
import com.example.domain.model.UniversityProfile
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkType
import com.example.ui.components.AnimatedCoinBadge
import com.example.ui.components.AnimatedScaleCard
import com.example.ui.components.DailyHadithCard
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.PulseIndicatorDot
import com.example.ui.components.StudToolBrandHeader
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudAmberDark
import com.example.ui.theme.StudAmberGold
import com.example.ui.theme.StudNavyDark
import com.example.ui.theme.StudPrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    appContainer: AppContainer,
    onNavigateToGenerator: (WorkType?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToCoins: () -> Unit,
    onOpenTour: () -> Unit = {},
    onPreviewWork: (WorkDocument) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sessionManager = appContainer.sessionManager
    val coinBalance by appContainer.coinRepository.getBalance().collectAsState(initial = sessionManager.getCoinBalance())
    val selectedProfile by appContainer.universityProfileRepository.getSelectedProfile().collectAsState(
        initial = UniversityProfile()
    )
    val works by appContainer.workRepository.getAllWorks().collectAsState(initial = emptyList())
    val appLanguage by appContainer.settingsRepository.appLanguage.collectAsState()
    val isEn = appLanguage == "en"
    val dailyHadith = remember { HadithProvider.getDailyHadith() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    StudToolBrandHeader(badgeText = "ГОСТ / ВАК")
                },
                actions = {
                    AnimatedCoinBadge(
                        balance = coinBalance,
                        onClick = onNavigateToCoins,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("home_coin_badge")
                    )

                    IconButton(
                        onClick = onOpenTour,
                        modifier = Modifier.testTag("btn_open_tour")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = if (isEn) "App Tour" else "Тур по приложению",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier.testTag("home_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Ежедневное напоминание о пользе знаний (Хадис дня)
            DailyHadithCard(
                hadith = dailyHadith,
                isEnglish = isEn
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Главный академический Hero-баннер с градиентом и микроанимацией
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudNavyDark),
                border = BorderStroke(1.dp, StudAmberGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    StudNavyDark,
                                    StudPrimaryBlue,
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PulseIndicatorDot(color = StudAmberGold)
                            Text(
                                text = if (isEn) "QUANTIZED GOST 7.32 PIPELINE READY" else "КВАНТОВАННЫЙ ПАЙПЛАЙН ГОСТ 7.32 АКТИВЕН",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = StudAmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isEn) {
                                "Welcome, ${sessionManager.getUserName()}!"
                            } else {
                                "Здравствуйте, ${sessionManager.getUserName()}!"
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FrauncesFontFamily,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${selectedProfile.shortName} · ${selectedProfile.standard}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigateToGenerator(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudAmberGold,
                                    contentColor = StudNavyDark
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_hero_create_work")
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEn) "Create New Paper" else "Создать новую работу",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable { onOpenTour() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEn) "Tour" else "Тур",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Быстрые плитки навигации с пружинной анимацией
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.AutoAwesome,
                    title = if (isEn) "Generator" else "Генератор",
                    subtitle = if (isEn) "GOST pipeline" else "Поэтапный ГОСТ",
                    onClick = { onNavigateToGenerator(null) },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.History,
                    title = if (isEn) "Archive" else "Мои работы",
                    subtitle = if (isEn) "${works.size} saved" else "${works.size} в архиве",
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = if (isEn) "Top Up" else "Пополнение",
                    subtitle = "$coinBalance SC",
                    onClick = onNavigateToCoins,
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    icon = Icons.Default.HelpOutline,
                    title = if (isEn) "Support" else "Поддержка",
                    subtitle = if (isEn) "Tickets & refund" else "Возвраты SC",
                    onClick = onNavigateToSupport,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Стоимость и выбор типа студенческой работы (тарифы из WorkPricesResponse)
            EditorialSectionHeader(
                kicker = if (isEn) "ACADEMIC FORMATS & PRICING" else "ФОРМАТЫ РАБОТ И СТОИМОСТЬ В S-COIN",
                title = if (isEn) "Tap any format to start creating" else "Выберите формат для быстрого запуска",
                actionText = if (isEn) "Top up SC" else "Пополнить SC",
                onActionClick = onNavigateToCoins
            )

            Spacer(modifier = Modifier.height(10.dp))

            val priceItems = listOf(
                Triple(WorkType.ESSAY_REFERAT, "Реферат", 200),
                Triple(WorkType.SCIENTIFIC_ARTICLE, "Доклад / Статья", 120),
                Triple(WorkType.COURSEWORK, "Курсовая", 480),
                Triple(WorkType.ESSAY_REFERAT, "Эссе", 320),
                Triple(WorkType.DIPLOMA_VKR, "Диплом (ВКР)", 960),
                Triple(WorkType.MASTER_DISSERTATION, "Магистерская", 1200)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                priceItems.forEach { (type, label, price) ->
                    AnimatedScaleCard(
                        onClick = { onNavigateToGenerator(type) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${type.minPages}–${type.maxPages} стр.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = StudAmberGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "$price SC",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = StudAmberDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Последние сгенерированные работы
            EditorialSectionHeader(
                kicker = if (isEn) "RECENT PAPERS" else "ПОСЛЕДНИЕ РАБОТЫ",
                title = if (isEn) "Ready for preview & Word export" else "Готовы к предпросмотру и экспорту в Word",
                actionText = if (works.isNotEmpty()) (if (isEn) "View all (${works.size})" else "Смотреть все (${works.size})") else null,
                onActionClick = onNavigateToHistory
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (works.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Вы пока не создали ни одной работы",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Нажмите «Создать новую работу» для запуска генератора",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                works.take(3).forEach { work ->
                    HomeWorkItem(
                        work = work,
                        onPreview = { onPreviewWork(work) },
                        onOpenDocx = {
                            scope.launch {
                                val path = work.docxPath ?: appContainer.buildDocxUseCase(work).getOrNull()?.also {
                                    appContainer.workRepository.updateDocxPath(work.id, it)
                                }
                                FileSharingHelper.openDocx(context, path)
                            }
                        },
                        onShareDocx = {
                            scope.launch {
                                val path = work.docxPath ?: appContainer.buildDocxUseCase(work).getOrNull()?.also {
                                    appContainer.workRepository.updateDocxPath(work.id, it)
                                }
                                FileSharingHelper.shareDocx(context, path, work.meta.topic)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedScaleCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeWorkItem(
    work: WorkDocument,
    onPreview: () -> Unit,
    onOpenDocx: () -> Unit,
    onShareDocx: () -> Unit
) {
    AnimatedScaleCard(
        onClick = onPreview,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.meta.topic,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${work.meta.type.titleRu} · ${work.meta.targetPages} стр. · ГОСТ 7.32",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
            IconButton(onClick = onPreview) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Предпросмотр",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onOpenDocx) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Открыть Word",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onShareDocx) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
