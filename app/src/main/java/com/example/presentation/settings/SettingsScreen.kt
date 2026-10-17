package com.example.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.data.remote.LlmConnectionChecker
import com.example.domain.model.LlmSettings
import com.example.domain.model.UniversityProfile
import com.example.ui.components.AnimatedCoinBadge
import com.example.ui.components.AnimatedScaleCard
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudAmberDark
import com.example.ui.theme.StudAmberGold
import com.example.ui.theme.StudEmerald
import com.example.ui.theme.StudNavyDark
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class LlmPreset(
    val title: String,
    val endpoint: String,
    val model: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onNavigateToCoins: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onOpenTour: () -> Unit = {},
    onLogout: () -> Unit = {},
    showBackButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = showBackButton, onBack = onBack)

    val sessionManager = appContainer.sessionManager
    val pinManager = appContainer.pinManager
    val uniRepo = appContainer.universityProfileRepository
    val settingsRepo = appContainer.settingsRepository
    val coinBalance by appContainer.coinRepository.getBalance().collectAsState(initial = sessionManager.getCoinBalance())

    val selectedUni by uniRepo.getSelectedProfile().collectAsState(initial = UniversityProfile())
    val llmSettings by settingsRepo.getLlmSettings().collectAsState(initial = LlmSettings())
    val themeMode by settingsRepo.themeMode.collectAsState()
    val appLanguage by settingsRepo.appLanguage.collectAsState()
    val isEn = appLanguage == "en"

    var profilesList by remember { mutableStateOf(uniRepo.getProfiles()) }

    // Редактирование профиля пользователя
    var showEditProfile by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf(sessionManager.getUserName()) }
    var userEmail by remember { mutableStateOf(sessionManager.getUserEmail()) }

    // Поля LLM модели
    var endpoint by remember(llmSettings) { mutableStateOf(llmSettings.endpoint) }
    var apiKey by remember(llmSettings) { mutableStateOf(llmSettings.apiKey) }
    var model by remember(llmSettings) { mutableStateOf(llmSettings.model) }
    var timeoutStr by remember(llmSettings) { mutableStateOf(llmSettings.timeout.toString()) }
    var temperature by remember(llmSettings) { mutableFloatStateOf(llmSettings.temperature.toFloat()) }
    var showApiKey by remember { mutableStateOf(false) }

    var savedBanner by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionResultMessage by remember { mutableStateOf<String?>(null) }
    var connectionSuccess by remember { mutableStateOf(false) }

    // Добавление собственного ВУЗа
    var showAddUniForm by remember { mutableStateOf(false) }
    var customUniShort by remember { mutableStateOf("") }
    var customUniFull by remember { mutableStateOf("") }
    var customUniCity by remember { mutableStateOf("Москва") }

    // Настройки безопасности PIN и биометрии
    var hasPin by remember { mutableStateOf(pinManager.hasPin()) }
    var biometricEnabled by remember { mutableStateOf(pinManager.isBiometricEnabled()) }
    var showPinEditor by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pinSavedMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val llmPresets = remember {
        listOf(
            LlmPreset("Vibe Code API", "https://api.vibecode.online/v1/chat/completions", "vibe-code-pro-flash"),
            LlmPreset("OpenAI GPT-4o", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
            LlmPreset("DeepSeek V3", "https://api.deepseek.com/v1/chat/completions", "deepseek-chat"),
            LlmPreset("Gemini (OpenRouter)", "https://openrouter.ai/api/v1/chat/completions", "google/gemini-2.5-flash"),
            LlmPreset("Groq Llama 3.3", "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEn) "Settings & Profile" else "Настройки и Профиль",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isEn) "LLM Models · Theme · Language · GOST · S-coin" else "LLM-модели · Тема · Язык · ВУЗы · S-coin",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    AnimatedCoinBadge(
                        balance = coinBalance,
                        onClick = onNavigateToCoins,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .animateContentSize()
        ) {
            // 1. Карточка аккаунта, баланса S-coin и быстрых переходов
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "${selectedUni.shortName} · Баланс: $coinBalance SC",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = StudAmberDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        IconButton(onClick = { showEditProfile = !showEditProfile }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать профиль",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = showEditProfile) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("ФИО студента (для титульного листа)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = userEmail,
                                onValueChange = { userEmail = it },
                                label = { Text("Email аккаунта") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        appContainer.profileRepository.updateProfile(userName.trim(), userEmail.trim(), selectedUni.shortName)
                                        showEditProfile = false
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Сохранить данные профиля")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToCoins,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudAmberGold,
                                contentColor = StudNavyDark
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_settings_topup")
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Top Up SC" else "Пополнить SC",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToSupport,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_settings_support")
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Support" else "Поддержка",
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenTour,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEn) "Tour" else "Тур",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Тема оформления и Язык интерфейса
            EditorialSectionHeader(
                kicker = if (isEn) "INTERFACE & LOCALIZATION" else "ИНТЕРФЕЙС И ЛОКАЛИЗАЦИЯ",
                title = if (isEn) "Theme & Language" else "Тема оформления и язык"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEn) "Color Theme" else "Тема оформления",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "SYSTEM" to (if (isEn) "System" else "Системная"),
                            "LIGHT" to (if (isEn) "Light" else "Светлая"),
                            "DARK" to (if (isEn) "Dark" else "Тёмная")
                        ).forEach { (code, label) ->
                            val selected = themeMode == code
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { settingsRepo.setThemeMode(code) }
                                    .testTag("theme_option_${code.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEn) "Interface Language" else "Язык интерфейса и промптов",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ru" to "Русский (RU · ГОСТ)",
                            "en" to "English (EN · International)"
                        ).forEach { (code, label) ->
                            val selected = appLanguage == code
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { settingsRepo.setAppLanguage(code) }
                                    .testTag("lang_option_$code")
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3. Добавление и настройка LLM-модели
            EditorialSectionHeader(
                kicker = if (isEn) "AI ENGINE & LLM PROVIDER" else "ДОБАВЛЕНИЕ И НАСТРОЙКА LLM-МОДЕЛИ",
                title = if (isEn) "Connect OpenAI / VibeCode / Gemini / DeepSeek" else "Подключение провайдера генерации"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEn) "Quick Model Presets:" else "Быстрый выбор провайдера LLM:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        llmPresets.forEach { preset ->
                            FilterChip(
                                selected = model == preset.model && endpoint == preset.endpoint,
                                onClick = {
                                    endpoint = preset.endpoint
                                    model = preset.model
                                    savedBanner = false
                                    connectionResultMessage = null
                                },
                                label = { Text(preset.title, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it; savedBanner = false },
                        label = { Text("Endpoint URL (OpenAI / VibeCode compatible)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_llm_endpoint"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; savedBanner = false },
                        label = { Text("API Key (Bearer токен провайдера)") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Показать API ключ"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_llm_api_key"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it; savedBanner = false },
                            label = { Text("Имя модели (Model ID)") },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("input_llm_model"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = timeoutStr,
                            onValueChange = { timeoutStr = it.filter { ch -> ch.isDigit() }; savedBanner = false },
                            label = { Text("Таймаут (с)") },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("input_llm_timeout"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Температура генерации (креативность):",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${(temperature * 100).roundToInt() / 100.0}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it; savedBanner = false },
                        valueRange = 0.1f..1.0f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingConnection = true
                                    connectionResultMessage = null
                                    val res = appContainer.llmConnectionChecker.testConnection(
                                        endpoint = endpoint,
                                        apiKey = apiKey,
                                        model = model
                                    )
                                    isTestingConnection = false
                                    when (res) {
                                        is LlmConnectionChecker.CheckResult.Success -> {
                                            connectionSuccess = true
                                            connectionResultMessage = res.message
                                        }
                                        is LlmConnectionChecker.CheckResult.Error -> {
                                            connectionSuccess = false
                                            connectionResultMessage = res.message
                                        }
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_test_llm_connection")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Тест связи", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val timeout = timeoutStr.toLongOrNull() ?: 90L
                                    settingsRepo.saveLlmSettings(
                                        llmSettings.copy(
                                            endpoint = endpoint.trim(),
                                            apiKey = apiKey.trim(),
                                            model = model.trim(),
                                            timeout = timeout,
                                            temperature = temperature.toDouble()
                                        )
                                    )
                                    savedBanner = true
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("btn_save_settings")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить LLM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (connectionResultMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = (if (connectionSuccess) StudEmerald else MaterialTheme.colorScheme.error).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = connectionResultMessage!!,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (connectionSuccess) StudEmerald else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (savedBanner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Настройки LLM успешно сохранены!",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = StudEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Профили ВУЗов и ГОСТ 7.32-2017
            EditorialSectionHeader(
                kicker = if (isEn) "UNIVERSITY & GOST STANDARDS" else "УНИВЕРСИТЕТСКИЕ ПРОФИЛИ И ГОСТ",
                title = if (isEn) "Select active formatting template" else "Шаблон титульного листа и полей ГОСТ",
                actionText = if (showAddUniForm) "Скрыть" else "+ Свой ВУЗ",
                onActionClick = { showAddUniForm = !showAddUniForm }
            )

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(visible = showAddUniForm) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Новый профиль учебного заведения",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customUniShort,
                            onValueChange = { customUniShort = it },
                            label = { Text("Краткое название (например, ДГУ / РАНХиГС)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customUniFull,
                            onValueChange = { customUniFull = it },
                            label = { Text("Полное наименование для шапки титульного листа") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customUniCity,
                            onValueChange = { customUniCity = it },
                            label = { Text("Город") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (customUniShort.isNotBlank() && customUniFull.isNotBlank()) {
                                    scope.launch {
                                        val newProfile = UniversityProfile(
                                            id = "custom_${System.currentTimeMillis()}",
                                            fullName = customUniFull.trim(),
                                            shortName = customUniShort.trim(),
                                            city = customUniCity.trim().ifBlank { "Москва" },
                                            standard = "ГОСТ 7.32-2017 · Индивидуальный профиль"
                                        )
                                        uniRepo.addCustomProfile(newProfile)
                                        profilesList = uniRepo.getProfiles()
                                        customUniShort = ""
                                        customUniFull = ""
                                        showAddUniForm = false
                                    }
                                }
                            },
                            enabled = customUniShort.isNotBlank() && customUniFull.isNotBlank(),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Добавить и выбрать профиль ВУЗа")
                        }
                    }
                }
            }

            profilesList.forEach { profile ->
                val isSelected = profile.id == selectedUni.id
                AnimatedScaleCard(
                    onClick = {
                        scope.launch {
                            uniRepo.selectProfile(profile.id)
                        }
                    },
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.shortName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = profile.fullName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                maxLines = 2
                            )
                            Text(
                                text = "${profile.standard} · г. ${profile.city}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Выбрано",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. Безопасность (PIN-код и биометрия)
            EditorialSectionHeader(
                kicker = if (isEn) "SECURITY & ACCESS" else "БЕЗОПАСНОСТЬ И ЗАЩИТА ВХОДА",
                title = if (isEn) "PIN Code & Biometrics" else "PIN-код и биометрия"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Быстрый вход по PIN-коду",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (hasPin) "PIN-код установлен и активен" else "Защитите доступ 4-значным кодом",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showPinEditor = !showPinEditor },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (hasPin) "Изменить" else "Задать PIN", fontSize = 12.sp)
                        }
                    }

                    AnimatedVisibility(visible = showPinEditor) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPinInput,
                                    onValueChange = { if (it.length <= 4) newPinInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("4 цифры PIN") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newPinInput.length == 4) {
                                            pinManager.setPin(newPinInput)
                                            hasPin = true
                                            showPinEditor = false
                                            newPinInput = ""
                                            pinSavedMessage = "PIN-код успешно сохранён"
                                        }
                                    },
                                    enabled = newPinInput.length == 4,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Сохранить")
                                }
                                if (hasPin) {
                                    OutlinedButton(
                                        onClick = {
                                            pinManager.clearPin()
                                            hasPin = false
                                            biometricEnabled = false
                                            showPinEditor = false
                                            pinSavedMessage = "PIN-код отключён"
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Сбросить")
                                    }
                                }
                            }
                        }
                    }

                    if (pinSavedMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pinSavedMessage!!,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StudEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Биометрия (отпечаток пальца)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Разблокировка приложения через системный биометрический сенсор",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                pinManager.setBiometricEnabled(it)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Выход из аккаунта
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_logout")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEn) "Log Out" else "Выйти из аккаунта",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
