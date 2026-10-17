package com.example.presentation.coins

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.domain.model.DepositRequest
import com.example.domain.model.PaymentDetails
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudAmberDark
import com.example.ui.theme.StudAmberDeep
import com.example.ui.theme.StudAmberGold
import com.example.ui.theme.StudEmerald
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoinsScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onNavigateToSupportRefund: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coinRepo = appContainer.coinRepository
    val depositRepo = appContainer.depositRepository
    val balance by coinRepo.getBalance().collectAsState(initial = appContainer.sessionManager.getCoinBalance())
    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "animated_scoin_balance"
    )
    val packages = coinRepo.getPackages()
    val deposits by depositRepo.localDeposits.collectAsState()
    val scope = rememberCoroutineScope()

    var paymentDetails by remember { mutableStateOf(PaymentDetails()) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Пополнение, 1 = Мои запросы
    var amountRubText by remember { mutableStateOf("500") }
    var promocodeText by remember { mutableStateOf("") }
    var appliedDiscount by remember { mutableIntStateOf(0) }
    var appliedPromoId by remember { mutableStateOf<Long?>(null) }
    var promoStatusMessage by remember { mutableStateOf<String?>(null) }
    var promoIsSuccess by remember { mutableStateOf(false) }
    var showPaymentStep by remember { mutableStateOf(false) }
    var paymentSubmittedBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        paymentDetails = depositRepo.getPaymentInfo()
        depositRepo.getMyDeposits()
    }

    val amountRub = amountRubText.toIntOrNull() ?: 0
    val calculatedCoins = (amountRub * paymentDetails.rate).toInt()
    val finalRubWithDiscount = if (appliedDiscount > 0) {
        (amountRub * (100 - appliedDiscount) / 100.0).toInt().coerceAtLeast(1)
    } else {
        amountRub
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Баланс и Пополнение S-coin",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Курс: 1 ₽ = ${paymentDetails.rate} SC · Мин. ${paymentDetails.minDeposit} ₽",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = StudAmberDark
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        modifier = modifier.testTag("coins_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Hero карточка текущего баланса
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, StudAmberGold.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    StudAmberGold.copy(alpha = 0.18f),
                                    StudAmberGold.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = StudAmberDark,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "БАЛАНС S-COIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = StudAmberDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$animatedBalance S-coin",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = StudAmberDeep
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Внутренняя академическая валюта для генерации работ по ГОСТ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Переключатель вкладок: Пополнение по карте Сбер / Пакеты | Мои запросы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Пополнение S-coin" to Icons.Default.AccountBalanceWallet,
                    "Мои запросы (${deposits.size})" to Icons.Default.History
                ).forEachIndexed { idx, pair ->
                    val selected = activeTab == idx
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = idx }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = pair.second,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pair.first,
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

            Spacer(modifier = Modifier.height(16.dp))

            if (activeTab == 0) {
                // Секция 1: Перевод по карте Сбербанка с промокодом (как в оригинальном приложении)
                EditorialSectionHeader(
                    kicker = "ОПЛАТА ПЕРЕВОДОМ · СБЕРБАНК",
                    title = "Пополнение с промокодом"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Выберите или введите сумму пополнения (мин. ${paymentDetails.minDeposit} ₽):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(500, 1000, 1500, 2500, 5000).forEach { preset ->
                                FilterChip(
                                    selected = amountRub == preset,
                                    onClick = { amountRubText = preset.toString() },
                                    label = {
                                        Text(
                                            text = "$preset ₽ → ${(preset * paymentDetails.rate).toInt()} SC",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = amountRubText,
                            onValueChange = { amountRubText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Сумма в рублях (₽)") },
                            supportingText = {
                                Text(
                                    text = if (amountRub >= paymentDetails.minDeposit) {
                                        "К зачислению: +$calculatedCoins S-coin"
                                    } else {
                                        "Минимальная сумма: ${paymentDetails.minDeposit} ₽"
                                    },
                                    color = if (amountRub >= paymentDetails.minDeposit) StudEmerald else MaterialTheme.colorScheme.error
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_deposit_amount")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Промокод
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promocodeText,
                                onValueChange = {
                                    promocodeText = it
                                    promoStatusMessage = null
                                },
                                label = { Text("Промокод (например, STUD2026)") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_promocode")
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = depositRepo.checkPromocode(promocodeText)
                                        if (res.isSuccess) {
                                            val body = res.getOrNull()!!
                                            appliedDiscount = body.discountPercent
                                            appliedPromoId = body.promocodeId
                                            promoIsSuccess = true
                                            promoStatusMessage = "Промокод применён: скидка ${body.discountPercent}%"
                                        } else {
                                            appliedDiscount = 0
                                            appliedPromoId = null
                                            promoIsSuccess = false
                                            promoStatusMessage = res.exceptionOrNull()?.message ?: "Неверный промокод"
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Применить")
                            }
                        }

                        if (promoStatusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = promoStatusMessage!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (promoIsSuccess) StudEmerald else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showPaymentStep = true },
                            enabled = amountRub >= paymentDetails.minDeposit,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_proceed_to_payment")
                        ) {
                            Text(
                                text = "Перейти к реквизитам ($finalRubWithDiscount ₽ → +$calculatedCoins SC)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Реквизиты карты Сбербанка и кнопка «Я оплатил»
                        AnimatedVisibility(
                            visible = showPaymentStep,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StudAmberGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "РЕКВИЗИТЫ ДЛЯ ПЕРЕВОДА (${paymentDetails.bank.uppercase()})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Surface(
                                                color = StudEmerald.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = paymentDetails.bank,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = StudEmerald,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = paymentDetails.card,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("card", paymentDetails.card.replace(" ", "")))
                                                    Toast.makeText(context, "Номер карты скопирован", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Скопировать номер карты",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Получатель: ${paymentDetails.holder}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Сумма к переводу: $finalRubWithDiscount ₽" +
                                                if (appliedDiscount > 0) " (скидка $appliedDiscount%)" else "",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = StudAmberDeep,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val sberIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("sberbankonline://")
                                                )
                                                context.startActivity(sberIntent)
                                            } catch (_: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Скопируйте номер карты ${paymentDetails.card} и выполните перевод в приложении банка",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Открыть Сбербанк", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val created = depositRepo.createDeposit(
                                                    amountRub = amountRub,
                                                    discountPercent = appliedDiscount,
                                                    promocodeId = appliedPromoId
                                                )
                                                coinRepo.addCoins(calculatedCoins)
                                                val reqId = created.getOrNull()?.id ?: 1001L
                                                paymentSubmittedBanner =
                                                    "Заявка #$reqId создана! Коины придут в течение 10 минут после проверки (+$calculatedCoins SC зачислено)."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StudEmerald),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_i_have_paid")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Я оплатил", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (paymentSubmittedBanner != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = StudEmerald.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = paymentSubmittedBanner!!,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = StudEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Секция 2: Экспресс-пакеты S-coin
                EditorialSectionHeader(
                    kicker = "ГОТОВЫЕ ПАКЕТЫ S-COIN",
                    title = "Быстрое пополнение под тип работы"
                )

                Spacer(modifier = Modifier.height(8.dp))

                packages.forEach { pkg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                width = if (pkg.isPopular) 1.8.dp else 0.8.dp,
                                color = if (pkg.isPopular) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${pkg.amount} S-coin",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FrauncesFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (pkg.isPopular) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ХИТ",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = pkg.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        coinRepo.addCoins(pkg.amount)
                                        depositRepo.createDeposit(amountRub = pkg.priceRub)
                                    }
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "${pkg.priceRub} ₽", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Вкладка «Мои запросы на пополнение»
                EditorialSectionHeader(
                    kicker = "ИСТОРИЯ ПЛАТЕЖЕЙ · HUPRI.ONLINE",
                    title = "Мои запросы на пополнение"
                )
                Spacer(modifier = Modifier.height(10.dp))

                deposits.forEach { req ->
                    DepositRequestCard(req)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Плашка возврата коинов
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Работа не сгенерировалась?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Оформите обращение на возврат списанных S-coin через службу поддержки.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Button(
                        onClick = onNavigateToSupportRefund,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Возврат")
                    }
                }
            }
        }
    }
}

@Composable
private fun DepositRequestCard(request: DepositRequest) {
    val statusColor = when (request.status) {
        "approved" -> StudEmerald
        "rejected" -> MaterialTheme.colorScheme.error
        else -> StudAmberDark
    }
    val statusLabel = when (request.status) {
        "approved" -> "ОДОБРЕНО"
        "rejected" -> "ОТКЛОНЕНО"
        else -> "НА ПРОВЕРКЕ"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Заявка #${request.id} · +${request.eskoinsAmount} SC",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Сумма: ${request.finalAmountRub.ifBlank { request.amountRub }} ₽" +
                    if (request.discountPercent > 0) " (промокод -${request.discountPercent}%)" else "",
                style = MaterialTheme.typography.bodySmall
            )
            if (!request.adminComment.isNullOrBlank()) {
                Text(
                    text = request.adminComment,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Создано: ${request.createdAt}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
