package com.example.presentation.coins

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.di.AppContainer
import com.example.presentation.subscription.SubscriptionScreen

@Composable
fun CoinsScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    onNavigateToSupportRefund: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SubscriptionScreen(
        appContainer = appContainer,
        onBack = onBack,
        modifier = modifier
    )
}
