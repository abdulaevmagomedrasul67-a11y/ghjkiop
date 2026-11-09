package com.example.presentation.subscription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.domain.model.SubscriptionOrder
import com.example.domain.model.SubscriptionPlanType
import com.example.domain.model.SubscriptionStatus
import kotlinx.coroutines.launch

private val SwissInk = Color(0xFF0A0A0A)
private val SwissPaper = Color(0xFFF0EDE6)
private val SwissPaperCard = Color(0xFFF8F6F1)
private val SwissAccent = Color(0xFFC8451F)
private val SwissGreen = Color(0xFF1E6B47)
private val SwissLine = Color(0xFFD6D1C7)
private val SwissMuted = Color(0xFF5A5751)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subRepo = appContainer.subscriptionRepository
    val userId = appContainer.sessionManager.getUserId().takeIf { it > 0L } ?: 1L
    val status by subRepo.observeSubscriptionStatus().collectAsState()
    val orders by subRepo.observeOrders().collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var showTariffsOverride by remember { mutableStateOf(false) }
    var activePaymentOrder by remember { mutableStateOf<SubscriptionOrder?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        isLoading = true
        subRepo.getSubscriptionStatus(userId)
        subRepo.getMySubscriptions(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ПОДПИСКА STUDTOOL",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp,
                            color = SwissInk
                        )
                        Text(
                            text = "БЕЗЛИМИТНЫЙ ДОСТУП К ГЕНЕРАЦИИ ПО ГОСТ",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SwissMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("subscription_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = SwissInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SwissPaper
                )
            )
        },
        containerColor = SwissPaper,
        modifier = modifier.testTag("subscription_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            HorizontalDivider(color = SwissInk, thickness = 1.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            if (statusBanner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SwissGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .border(1.dp, SwissGreen, RoundedCornerShape(4.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = statusBanner!!,
                        color = SwissGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            when {
                // 2.5. Экран оплаты реквизитами Сбера после создания заявки
                activePaymentOrder != null -> {
                    SubscriptionPaymentCard(
                        order = activePaymentOrder!!,
                        isSubmitting = isLoading,
                        onCopyCard = { card ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Sber Card", card.replace(" ", "")))
                            Toast.makeText(context, "Номер карты скопирован", Toast.LENGTH_SHORT).show()
                        },
                        onConfirmPaid = {
                            scope.launch {
                                isLoading = true
                                val orderId = activePaymentOrder!!.id
                                subRepo.confirmOrderPaid(userId, orderId)
                                activePaymentOrder = null
                                showTariffsOverride = false
                                statusBanner = "Заявка #$orderId отмечена как оплаченная. Подписка активирована!"
                                isLoading = false
                            }
                        },
                        onCancel = {
                            activePaymentOrder = null
                        }
                    )
                }

                // 2.3. Экран «Подписка активна»
                status.active && !showTariffsOverride -> {
                    ActiveSubscriptionView(
                        status = status,
                        onRenewClick = {
                            showTariffsOverride = true
                        },
                        onCancelAutoRenew = {
                            scope.launch {
                                subRepo.cancelAutoRenew(userId)
                                statusBanner = "Автопродление отключено. Подписка действует до ${status.expiresAt ?: "окончания срока"}."
                            }
                        }
                    )
                }

                // 2.2. Два тарифа в швейцарском стиле StudTool
                else -> {
                    if (status.active && showTariffsOverride) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ПРОДЛЕНИЕ ПОДПИСКИ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwissAccent
                            )
                            Text(
                                text = "← Вернуться к статусу",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwissInk,
                                modifier = Modifier.clickable { showTariffsOverride = false }
                            )
                        }
                    }

                    TariffSelectionSection(
                        hasHadSubscription = status.hasHadSubscription,
                        isSubmitting = isLoading,
                        onSelectPlan = { planType ->
                            scope.launch {
                                isLoading = true
                                val res = subRepo.createSubscriptionOrder(userId, planType)
                                isLoading = false
                                res.onSuccess { order ->
                                    activePaymentOrder = order
                                }.onFailure { err ->
                                    statusBanner = err.message ?: "Не удалось создать заявку"
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Блок «Что входит в подписку»
            IncludedFeaturesBlock()

            // История подписок / заявок
            if (orders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                SubscriptionOrdersSection(
                    orders = orders,
                    onOpenPendingOrder = { order ->
                        activePaymentOrder = order
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TariffSelectionSection(
    hasHadSubscription: Boolean,
    isSubmitting: Boolean,
    onSelectPlan: (SubscriptionPlanType) -> Unit
) {
    val yearlyPriceFormatted = if (hasHadSubscription) "21 600 ₽" else "14 000 ₽"
    val yearlyButtonText = if (hasHadSubscription) {
        "Продлить за 21 600 ₽"
    } else {
        "Оформить за 14 000 ₽"
    }

    Text(
        text = "ВЫБЕРИТЕ ТАРИФНЫЙ ПЛАН",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = SwissMuted
    )

    Spacer(modifier = Modifier.height(10.dp))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MonthlyTariffCard(
                    isSubmitting = isSubmitting,
                    onSelect = { onSelectPlan(SubscriptionPlanType.MONTHLY) },
                    modifier = Modifier.weight(1f)
                )
                YearlyTariffCard(
                    hasHadSubscription = hasHadSubscription,
                    priceFormatted = yearlyPriceFormatted,
                    buttonText = yearlyButtonText,
                    isSubmitting = isSubmitting,
                    onSelect = { onSelectPlan(SubscriptionPlanType.YEARLY) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                YearlyTariffCard(
                    hasHadSubscription = hasHadSubscription,
                    priceFormatted = yearlyPriceFormatted,
                    buttonText = yearlyButtonText,
                    isSubmitting = isSubmitting,
                    onSelect = { onSelectPlan(SubscriptionPlanType.YEARLY) },
                    modifier = Modifier.fillMaxWidth()
                )
                MonthlyTariffCard(
                    isSubmitting = isSubmitting,
                    onSelect = { onSelectPlan(SubscriptionPlanType.MONTHLY) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MonthlyTariffCard(
    isSubmitting: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SwissPaperCard, RoundedCornerShape(4.dp))
            .border(1.dp, SwissInk, RoundedCornerShape(4.dp))
            .padding(18.dp)
            .testTag("card_monthly_subscription")
    ) {
        Text(
            text = "ТАРИФ 01 · 30 ДНЕЙ",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SwissMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Подписка на месяц",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = SwissInk
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "1 800 ₽",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = SwissInk
        )
        Text(
            text = "за 30 дней полного доступа",
            fontSize = 12.sp,
            color = SwissMuted
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = SwissLine)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Неограниченная генерация работ в течение 30 дней",
            fontSize = 13.sp,
            color = SwissInk,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSelect,
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = SwissInk,
                contentColor = SwissPaper
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_subscribe_monthly")
        ) {
            Text(
                text = "Оформить за 1 800 ₽",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun YearlyTariffCard(
    hasHadSubscription: Boolean,
    priceFormatted: String,
    buttonText: String,
    isSubmitting: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SwissPaperCard, RoundedCornerShape(4.dp))
            .border(2.dp, SwissAccent, RoundedCornerShape(4.dp))
            .padding(18.dp)
            .testTag("card_yearly_subscription")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ТАРИФ 02 · 365 ДНЕЙ",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SwissAccent
            )
            if (!hasHadSubscription) {
                Surface(
                    color = SwissAccent,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = "выгодно · -35%",
                        color = SwissPaper,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Подписка на год",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = SwissInk
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = priceFormatted,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = SwissAccent
            )
            if (!hasHadSubscription) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "21 600 ₽",
                    fontSize = 15.sp,
                    color = SwissMuted,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        if (!hasHadSubscription) {
            Text(
                text = "вместо 21 600 ₽ — экономия 7 600 ₽",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SwissGreen
            )
        } else {
            Text(
                text = "12 месяцев × 1 800 ₽ — полный годовой доступ",
                fontSize = 12.sp,
                color = SwissMuted
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = SwissLine)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Неограниченная генерация курсовых, рефератов, докладов, эссе и контрольных работ весь учебный год",
            fontSize = 13.sp,
            color = SwissInk,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSelect,
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = SwissAccent,
                contentColor = SwissPaper
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_subscribe_yearly")
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ActiveSubscriptionView(
    status: SubscriptionStatus,
    onRenewClick: () -> Unit,
    onCancelAutoRenew: () -> Unit
) {
    val planTitle = when (status.planType) {
        SubscriptionPlanType.YEARLY -> "Годовая"
        else -> "Месячная"
    }
    val expiresFormatted = status.expiresAt ?: "31.12.2026"
    val daysRemaining = if (status.daysRemaining > 0) status.daysRemaining else 30

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SwissPaperCard, RoundedCornerShape(4.dp))
            .border(2.dp, SwissInk, RoundedCornerShape(4.dp))
            .padding(20.dp)
            .testTag("active_subscription_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = SwissGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Подписка активна",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = SwissInk
                )
            }
            Surface(
                color = SwissGreen,
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = SwissPaper,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SwissLine)
        Spacer(modifier = Modifier.height(14.dp))

        SwissMetaRow(label = "Тип подписки", value = planTitle)
        Spacer(modifier = Modifier.height(8.dp))
        SwissMetaRow(label = "Дата окончания", value = expiresFormatted)
        Spacer(modifier = Modifier.height(8.dp))
        SwissMetaRow(label = "Осталось дней", value = "$daysRemaining")

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onRenewClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = SwissAccent,
                contentColor = SwissPaper
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_renew_subscription")
        ) {
            Text(
                text = "Продлить",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (status.autoRenew) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCancelAutoRenew,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_cancel_autorenew")
            ) {
                Text(
                    text = "Отменить автопродление",
                    color = SwissInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SwissMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = SwissMuted
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = SwissInk
        )
    }
}

@Composable
private fun SubscriptionPaymentCard(
    order: SubscriptionOrder,
    isSubmitting: Boolean,
    onCopyCard: (String) -> Unit,
    onConfirmPaid: () -> Unit,
    onCancel: () -> Unit
) {
    val planTitle = if (order.planType == SubscriptionPlanType.YEARLY) "Подписка на год" else "Подписка на месяц"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SwissPaperCard, RoundedCornerShape(4.dp))
            .border(2.dp, SwissInk, RoundedCornerShape(4.dp))
            .padding(20.dp)
            .testTag("subscription_payment_card")
    ) {
        Text(
            text = "ОПЛАТА ЗАЯВКИ #${order.id}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SwissAccent
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = planTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = SwissInk
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Сумма к переводу: ${order.priceRub} ₽",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SwissInk
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = SwissLine)
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "РЕКВИЗИТЫ ДЛЯ ПЕРЕВОДА (СБЕР):",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SwissMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SwissPaper, RoundedCornerShape(4.dp))
                .border(1.dp, SwissInk, RoundedCornerShape(4.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = order.cardNumber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = SwissInk
                )
                Text(
                    text = "${order.cardHolder} · ${order.bankName}",
                    fontSize = 12.sp,
                    color = SwissMuted
                )
            }
            IconButton(onClick = { onCopyCard(order.cardNumber) }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Скопировать номер карты",
                    tint = SwissInk
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Переведите точную сумму ${order.priceRub} ₽ по указанным реквизитам и нажмите кнопку «Я оплатил». После проверки администратором подписка будет активирована.",
            fontSize = 12.sp,
            color = SwissMuted,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onConfirmPaid,
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = SwissAccent,
                contentColor = SwissPaper
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_confirm_paid")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = SwissPaper,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Я оплатил",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text("Назад к тарифам", color = SwissInk)
        }
    }
}

@Composable
private fun IncludedFeaturesBlock() {
    val features = listOf(
        "Неограниченная генерация (реферат, доклад, курсовая, эссе, контрольная)",
        "Все профили вузов",
        "Приоритетная поддержка",
        "Экспорт .docx без ограничений"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SwissPaperCard, RoundedCornerShape(4.dp))
            .border(1.dp, SwissInk, RoundedCornerShape(4.dp))
            .padding(18.dp)
            .testTag("subscription_features_block")
    ) {
        Text(
            text = "ЧТО ВХОДИТ В ПОДПИСКУ",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = SwissInk
        )
        Spacer(modifier = Modifier.height(12.dp))
        features.forEach { item ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SwissAccent,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item,
                    fontSize = 13.sp,
                    color = SwissInk,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SubscriptionOrdersSection(
    orders: List<SubscriptionOrder>,
    onOpenPendingOrder: (SubscriptionOrder) -> Unit
) {
    Text(
        text = "ИСТОРИЯ ПОДПИСОК",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissMuted
    )
    Spacer(modifier = Modifier.height(8.dp))

    orders.take(5).forEach { order ->
        val planLabel = if (order.planType == SubscriptionPlanType.YEARLY) "Подписка на год" else "Подписка на месяц"
        val statusLabel = when (order.status) {
            "active", "approved", "paid" -> "Оплачена · Активна"
            else -> "Ожидает оплаты"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(SwissPaperCard, RoundedCornerShape(4.dp))
                .border(1.dp, SwissLine, RoundedCornerShape(4.dp))
                .clickable(enabled = order.status == "pending") { onOpenPendingOrder(order) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (order.status == "pending") Icons.Default.Schedule else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (order.status == "pending") SwissAccent else SwissGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = planLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SwissInk
                    )
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        color = SwissMuted
                    )
                }
            }
            Text(
                text = "${order.priceRub} ₽",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SwissInk
            )
        }
    }
}
