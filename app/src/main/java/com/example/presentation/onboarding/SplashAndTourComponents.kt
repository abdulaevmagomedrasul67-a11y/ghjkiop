package com.example.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudAmberDark
import com.example.ui.theme.StudAmberGold
import com.example.ui.theme.StudNavyDark
import com.example.ui.theme.StudPrimaryBlue
import kotlinx.coroutines.delay

/**
 * Анимированная заставка StudTool при запуске приложения.
 */
@Composable
fun StudToolSplashOverlay(
    visible: Boolean,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onStartTour: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )
    val progress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.05f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 160f,
        targetValue = 250f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )

    LaunchedEffect(visible) {
        if (visible) {
            startAnim = true
            delay(1750)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(450))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StudNavyDark,
                            Color(0xFF152342),
                            StudNavyDark
                        )
                    )
                )
                .clickable(onClick = onDismiss)
                .testTag("splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Архитектурная сетка ГОСТ и мягкое золотое свечение
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 36.dp.toPx()
                val gridColor = Color.White.copy(alpha = 0.04f)
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            StudAmberGold.copy(alpha = 0.22f),
                            StudPrimaryBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius * 2f
                    ),
                    radius = glowRadius * 2f,
                    center = center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(32.dp)
                    .scale(scale)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, StudAmberGold.copy(alpha = 0.65f)),
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = StudAmberGold,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Surface(
                    color = StudAmberGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.8.dp, StudAmberGold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = if (isEnglish) "GOST 7.32-2017 · ACADEMIC AI STUDIO" else "ГОСТ 7.32-2017 · АКАДЕМИЧЕСКАЯ СТУДИЯ",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = StudAmberGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "StudTool",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 40.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isEnglish) {
                        "Quantized generation of student papers, term projects & theses in Word (.docx)"
                    } else {
                        "Квантованная генерация рефератов, курсовых и дипломных работ в Word (.docx)"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(200.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = StudAmberGold,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onStartTour) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StudAmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "App Tour" else "Тур по приложению",
                            color = StudAmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = if (isEnglish) "Continue →" else "Перейти в приложение →",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private data class TourStepData(
    val badge: String,
    val title: String,
    val subtitle: String,
    val bullets: List<String>,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * Интерактивный анимированный тур по приложению StudTool (Onboarding / Tutorial).
 */
@Composable
fun StudToolAppTourDialog(
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToDeposit: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val steps = remember(isEnglish) {
        if (isEnglish) {
            listOf(
                TourStepData(
                    badge = "STEP 01 / 04 · OVERVIEW",
                    title = "Welcome to StudTool Studio",
                    subtitle = "Your complete academic assistant for university papers formatted strictly to GOST 7.32-2017.",
                    bullets = listOf(
                        "Daily wisdom & motivation on the Home tab",
                        "Transparent S-coin balance & instant work pricing",
                        "4 convenient tabs: Overview, Create, History, Settings"
                    ),
                    icon = Icons.Default.School,
                    accentColor = StudAmberGold
                ),
                TourStepData(
                    badge = "STEP 02 / 04 · QUANTIZED GENERATOR",
                    title = "Multi-Stage Volume Pipeline",
                    subtitle = "Unlike standard chatbots that stop after 2 pages, StudTool splits your paper into quantized chapters.",
                    bullets = listOf(
                        "Supports Reports, Presentations, Essays, Term Papers & Theses (5–70 pages)",
                        "Real-time character & section tracking with context compression",
                        "Automatic GOST Title Page, Table of Contents & Bibliography"
                    ),
                    icon = Icons.Default.AutoAwesome,
                    accentColor = Color(0xFF3B82F6)
                ),
                TourStepData(
                    badge = "STEP 03 / 04 · WORD (.DOCX) & ARCHIVE",
                    title = "Instant Preview & Word Export",
                    subtitle = "Inspect every section inside the app or export a ready-to-print .docx file.",
                    bullets = listOf(
                        "Built-in profiles: MSU, HSE, SPbU, BMSTU, GPK & custom university rules",
                        "Times New Roman 14pt, 1.5 spacing, 30/15/20/20mm GOST margins",
                        "One-tap Open in Word or Share via Telegram / WhatsApp"
                    ),
                    icon = Icons.Default.Description,
                    accentColor = Color(0xFF10B981)
                ),
                TourStepData(
                    badge = "STEP 04 / 04 · LLM, S-COIN & SUPPORT",
                    title = "Custom LLM Models & S-coin Top-Up",
                    subtitle = "Connect your own LLM endpoint or top up S-coin with promo codes.",
                    bullets = listOf(
                        "Configure Vibe Code API, OpenAI, Gemini, or DeepSeek in Settings",
                        "Top up S-coin via Sberbank card with promo code discounts (e.g. STUD2026)",
                        "PIN & Biometric security + 24/7 Support & automatic S-coin refunds"
                    ),
                    icon = Icons.Default.SettingsSuggest,
                    accentColor = StudAmberDark
                )
            )
        } else {
            listOf(
                TourStepData(
                    badge = "ШАГ 01 / 04 · ГЛАВНАЯ И ОБЗОР",
                    title = "Добро пожаловать в StudTool",
                    subtitle = "Академическая платформа для создания студенческих и научных работ строго по ГОСТ 7.32-2017 и ГОСТ Р 7.0.100-2018.",
                    bullets = listOf(
                        "Напоминание дня о пользе знаний и быстрый обзор тарифов",
                        "Удобная нижняя навигация: Главная, Создать, История и Настройки",
                        "Прозрачный баланс S-coin и мгновенный доступ к последним работам"
                    ),
                    icon = Icons.Default.School,
                    accentColor = StudAmberGold
                ),
                TourStepData(
                    badge = "ШАГ 02 / 04 · КВАНТОВАННЫЙ ПАЙПЛАЙН",
                    title = "Генерация полного объёма (5–70 стр.)",
                    subtitle = "Вместо обрывочных ответов StudTool делит работу на квантованные главы со сжатием контекста.",
                    bullets = listOf(
                        "Реферат, Доклад, Эссе, Контрольная, Курсовая и Дипломная работа",
                        "Точный расчёт целевого объёма в символах и страницах по ГОСТ",
                        "Автоматическое формирование Введения, Глав, Заключения и Списка литературы"
                    ),
                    icon = Icons.Default.AutoAwesome,
                    accentColor = Color(0xFF3B82F6)
                ),
                TourStepData(
                    badge = "ШАГ 03 / 04 · ИСТОРИЯ И WORD (.DOCX)",
                    title = "Предпросмотр и экспорт в Word",
                    subtitle = "Каждая работа сохраняется в Архиве с полным титульным листом и оглавлением.",
                    bullets = listOf(
                        "Готовые профили ВУЗов: МГУ, НИУ ВШЭ, СПбГУ, МГТУ им. Баумана, ГПК",
                        "Вёрстка .docx: Times New Roman 14 пт, интервал 1.5, поля 30/15/20/20 мм",
                        "Встроенный предпросмотр документа и отправка в Telegram / WhatsApp / Word"
                    ),
                    icon = Icons.Default.Description,
                    accentColor = Color(0xFF10B981)
                ),
                TourStepData(
                    badge = "ШАГ 04 / 04 · НАСТРОЙКИ И ПОПОЛНЕНИЕ",
                    title = "LLM-модели, S-coin и Безопасность",
                    subtitle = "Полный контроль над ИИ-движком, темой оформления, языком и балансом.",
                    bullets = listOf(
                        "Подключение Vibe Code API, OpenAI, Gemini, DeepSeek с тестом соединения",
                        "Пополнение S-coin по карте Сбербанка + промокоды на скидку (STUD2026)",
                        "Светлая/Тёмная тема, языки RU/EN, PIN-код и Служба поддержки с возвратом"
                    ),
                    icon = Icons.Default.SettingsSuggest,
                    accentColor = StudAmberDark
                )
            )
        }
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .testTag("app_tour_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                // Верхняя панель с индикатором шага и кнопкой закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = step.accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = step.badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = step.accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть тур"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Анимированный переход между шагами тура
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { w -> w / 3 } + fadeIn() + scaleIn(initialScale = 0.95f))
                                .togetherWith(slideOutHorizontally { w -> -w / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { w -> -w / 3 } + fadeIn() + scaleIn(initialScale = 0.95f))
                                .togetherWith(slideOutHorizontally { w -> w / 3 } + fadeOut())
                        }
                    },
                    label = "tour_step_transition"
                ) { stepIndex ->
                    val item = steps[stepIndex]
                    Column {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = item.accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.accentColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        item.bullets.forEach { bullet ->
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = item.accentColor.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = item.accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Анимированные пружинные индикаторы страниц
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { idx ->
                        val isSelected = idx == currentStep
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) step.accentColor
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                                .clickable { currentStep = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Быстрые кнопки перехода на финальном шаге
                if (currentStep == steps.lastIndex) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onNavigateToSettings()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "LLM Settings" else "Настройки LLM",
                                fontSize = 12.sp
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onNavigateToDeposit()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) "Top up SC" else "Пополнить SC",
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Навигация Назад / Далее / Начать
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEnglish) "Back" else "Назад")
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text(if (isEnglish) "Skip" else "Пропустить")
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.lastIndex) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (currentStep < steps.lastIndex) {
                                if (isEnglish) "Next" else "Далее"
                            } else {
                                if (isEnglish) "Start working" else "Начать работу"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep < steps.lastIndex) {
                                Icons.AutoMirrored.Filled.ArrowForward
                            } else {
                                Icons.Default.Check
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
