package com.example.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.di.AppContainer
import com.example.presentation.auth.LoginScreen
import com.example.presentation.auth.RegisterScreen
import com.example.presentation.generator.GeneratorScreen
import com.example.presentation.generator.GeneratorViewModel
import com.example.presentation.history.HistoryScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.subscription.SubscriptionScreen
import com.example.presentation.support.SupportScreen

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (appContainer.authRepository.isLoggedIn()) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                appContainer = appContainer,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPassword = {
                    navController.navigate(Screen.Support.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                appContainer = appContainer,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                appContainer = appContainer,
                onNavigateToGenerator = { navController.navigate(Screen.Generator.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSupport = { navController.navigate(Screen.Support.route) },
                onNavigateToCoins = { navController.navigate(Screen.Subscription.route) }
            )
        }

        composable(Screen.Generator.route) {
            val generatorViewModel: GeneratorViewModel = viewModel(
                factory = GeneratorViewModel.Factory(
                    generateWorkUseCase = appContainer.generateWorkUseCase,
                    buildDocxUseCase = appContainer.buildDocxUseCase,
                    workRepository = appContainer.workRepository,
                    sessionManager = appContainer.sessionManager,
                    generationSessionDao = appContainer.generationSessionDao
                )
            )

            GeneratorScreen(
                viewModel = generatorViewModel,
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onNavigateToSupport = { navController.navigate(Screen.Support.route) },
                onNavigateToCoins = { navController.navigate(Screen.Subscription.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLogout = {
                    appContainer.sessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Support.route) {
            SupportScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
