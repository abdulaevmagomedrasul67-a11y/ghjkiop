package com.example.presentation.profile.requests

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.DepositRequest
import com.example.domain.repository.DepositRepository
import com.example.domain.repository.ProfileRepository
import com.example.ui.components.AppChip
import com.example.ui.components.AppDivider
import com.example.ui.components.StudToolTopBar
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.StudToolTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    depositRepository: DepositRepository,
    profileRepository: ProfileRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyRequestsViewModel = viewModel(
        factory = MyRequestsViewModel.Factory(depositRepository, profileRepository)
    )
) {
    val colors = StudToolTheme.colors
    val typography = StudToolTheme.typography
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Автообновление каждые 15 секунд, пока экран активен в состоянии RESUMED
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.loadRequests()
                delay(15_000)
            }
        }
    }

    // 2. Обработка событий (Snackbar при смене статуса на approved)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MyRequestsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Подписки, 1 = Восстановления

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_requests_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.paper
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StudToolTopBar(
                title = "Мои заявки",
                navigationIcon = com.example.ui.icons.Lucide.ArrowLeft,
                onNavigationClick = onNavigateBack,
                testTag = "my_requests_top_bar"
            )

            // Вкладки переключения (заявки на пополнение deposit_requests скрыты по требованию 2.1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppChip(
                    text = "ПОДПИСКИ",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )

                AppChip(
                    text = "ВОССТАНОВЛЕНИЯ (0)",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            AppDivider(color = colors.line)

            if (selectedTab == 0) {
                PullToRefreshBox(
                    isRefreshing = uiState.refreshing,
                    onRefresh = { viewModel.loadRequests(isPullToRefresh = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Управление подпиской в разделе «Подписка»",
                                fontFamily = FrauncesFontFamily,
                                fontSize = 18.sp,
                                color = colors.inkSoft
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Оформите месячную или годовую подписку в Профиле → Подписка",
                                style = typography.bodyRegular.copy(color = colors.inkFaint)
                            )
                        }
                    }
                }
            } else {
                // Список заявок на восстановление
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Заявок на восстановление нет",
                            fontFamily = FrauncesFontFamily,
                            fontSize = 18.sp,
                            color = colors.inkSoft
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Если возникнут сложности с доступом, отправьте форму сброса пароля",
                            style = typography.bodyRegular.copy(color = colors.inkFaint)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DepositRequestCard(req: DepositRequest) {
    val colors = StudToolTheme.colors
    val typography = StudToolTheme.typography

    val statusColor = when (req.status) {
        "approved" -> Color(0xFF4A8F5E)
        "rejected" -> Color(0xFFC8451F)
        else -> Color(0xFFE8C547)
    }

    val statusText = when (req.status) {
        "approved" -> "Активна"
        "rejected" -> "Отклонено"
        else -> "В обработке"
    }

    val statusDescription = when (req.status) {
        "approved" -> "Подписка активирована"
        "rejected" -> if (!req.adminComment.isNullOrEmpty()) {
            "Причина: ${req.adminComment}"
        } else {
            "Заявка отклонена администратором"
        }
        else -> "Подписка активируется после проверки оплаты"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paperWarm, shape = RoundedCornerShape(6.dp))
            .border(1.dp, colors.line, RoundedCornerShape(6.dp))
            .padding(14.dp)
            .testTag("deposit_request_card_${req.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ЗАЯВКА #${req.id}",
                    style = typography.labelMono.copy(color = colors.inkFaint, fontSize = 11.sp)
                )

                StatusBadge(
                    text = statusText,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Подписка",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.ink
                )

                Text(
                    text = "${req.amountRub} ₽",
                    style = typography.labelMono.copy(
                        color = colors.inkSoft,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusDescription,
                style = typography.bodyRegular.copy(
                    color = if (req.status == "rejected") statusColor else colors.inkSoft,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(req.createdAt),
                style = typography.metaMono.copy(color = colors.inkFaint, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color
) {
    val typography = StudToolTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, shape = CircleShape)
        )
        Text(
            text = text,
            style = typography.labelMono.copy(
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

fun formatDate(iso: String): String {
    // "2026-09-30 12:34:56" → "30.09.2026 · 12:34"
    val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val output = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault())
    return try {
        output.format(input.parse(iso) ?: return iso)
    } catch (e: Exception) {
        iso
    }
}
