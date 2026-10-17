package com.example.core.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.di.AppContainer
import com.example.domain.model.SupportCategory
import com.example.domain.model.WorkDocument
import com.example.presentation.auth.LoginScreen
import com.example.presentation.auth.RegisterScreen
import com.example.presentation.coins.CoinsScreen
import com.example.presentation.generator.GeneratorScreen
import com.example.presentation.generator.GeneratorViewModel
import com.example.presentation.history.HistoryScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.onboarding.StudToolAppTourDialog
import com.example.presentation.onboarding.StudToolSplashOverlay
import com.example.presentation.preview.PreviewScreen
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.support.SupportScreen
import kotlinx.coroutines.launch

private data class BottomNavTab(
    val route: String,
    val labelRu: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    showSplashInitially: Boolean = false
) {
    val startDestination = if (appContainer.authRepository.isLoggedIn()) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    val appLanguage by appContainer.settingsRepository.appLanguage.collectAsState()
    val coinBalance by appContainer.coinRepository.getBalance().collectAsState(
        initial = appContainer.sessionManager.getCoinBalance()
    )
    val isEn = appLanguage == "en"
    val scope = rememberCoroutineScope()

    var showSplash by rememberSaveable { mutableStateOf(showSplashInitially) }
    var showTourDialog by rememberSaveable { mutableStateOf(false) }
    var previewWork by remember { mutableStateOf<WorkDocument?>(null) }
    var supportInitialCategory by remember { mutableStateOf<SupportCategory?>(null) }

    val generatorViewModel: GeneratorViewModel = viewModel(
        factory = GeneratorViewModel.Factory(
            generateWorkUseCase = appContainer.generateWorkUseCase,
            buildDocxUseCase = appContainer.buildDocxUseCase,
            workRepository = appContainer.workRepository,
            settingsRepository = appContainer.settingsRepository,
            universityProfileRepository = appContainer.universityProfileRepository,
            coinRepository = appContainer.coinRepository
        )
    )

    val bottomTabs = remember {
        listOf(
            BottomNavTab(
                route = Screen.Home.route,
                labelRu = "Главная",
                labelEn = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_tab_home"
            ),
            BottomNavTab(
                route = Screen.Generator.route,
                labelRu = "Создать",
                labelEn = "Create",
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_tab_generator"
            ),
            BottomNavTab(
                route = Screen.History.route,
                labelRu = "Истории",
                labelEn = "History",
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                testTag = "nav_tab_history"
            ),
            BottomNavTab(
                route = Screen.Settings.route,
                labelRu = "Настройки",
                labelEn = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_tab_settings"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route } && previewWork == null

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (previewWork != null) {
            PreviewScreen(
                work = previewWork!!,
                appContainer = appContainer,
                onBack = { previewWork = null }
            )
        } else {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(visible = showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            bottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = if (isEn) tab.labelEn else tab.labelRu
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isEn) tab.labelEn else tab.labelRu,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    enterTransition = {
                        fadeIn(animationSpec = tween(240)) +
                            slideInHorizontally(initialOffsetX = { it / 10 }, animationSpec = tween(240))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(180)) +
                            slideOutHorizontally(targetOffsetX = { -it / 12 }, animationSpec = tween(180))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(initialOffsetX = { -it / 10 }, animationSpec = tween(220))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(180)) +
                            slideOutHorizontally(targetOffsetX = { it / 12 }, animationSpec = tween(180))
                    }
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
                                supportInitialCategory = SupportCategory.PASSWORD
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
                            onNavigateToGenerator = { selectedType ->
                                selectedType?.let { generatorViewModel.setWorkType(it) }
                                navigateToTab(Screen.Generator.route)
                            },
                            onNavigateToHistory = { navigateToTab(Screen.History.route) },
                            onNavigateToProfile = { navigateToTab(Screen.Settings.route) },
                            onNavigateToSupport = {
                                supportInitialCategory = null
                                navController.navigate(Screen.Support.route)
                            },
                            onNavigateToCoins = { navController.navigate(Screen.Coins.route) },
                            onOpenTour = { showTourDialog = true },
                            onPreviewWork = { work -> previewWork = work }
                        )
                    }

                    composable(Screen.Generator.route) {
                        GeneratorScreen(
                            viewModel = generatorViewModel,
                            coinBalance = coinBalance,
                            onNavigateToCoins = { navController.navigate(Screen.Coins.route) },
                            onPreviewWork = { work -> previewWork = work }
                        )
                    }

                    composable(Screen.History.route) {
                        HistoryScreen(
                            appContainer = appContainer,
                            onBack = { navigateToTab(Screen.Home.route) },
                            onPreviewWork = { work -> previewWork = work },
                            showBackButton = false
                        )
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            appContainer = appContainer,
                            onBack = { navController.popBackStack() },
                            onNavigateToSupport = {
                                supportInitialCategory = null
                                navController.navigate(Screen.Support.route)
                            },
                            onNavigateToCoins = { navController.navigate(Screen.Coins.route) },
                            onNavigateToSettings = { navigateToTab(Screen.Settings.route) },
                            onOpenTour = { showTourDialog = true },
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
                            onBack = { navController.popBackStack() },
                            initialCategory = supportInitialCategory
                        )
                    }

                    composable(Screen.Coins.route) {
                        CoinsScreen(
                            appContainer = appContainer,
                            onBack = { navController.popBackStack() },
                            onNavigateToSupportRefund = {
                                supportInitialCategory = SupportCategory.REFUND
                                navController.navigate(Screen.Support.route)
                            }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            appContainer = appContainer,
                            onBack = { navigateToTab(Screen.Home.route) },
                            onNavigateToCoins = { navController.navigate(Screen.Coins.route) },
                            onNavigateToSupport = {
                                supportInitialCategory = null
                                navController.navigate(Screen.Support.route)
                            },
                            onOpenTour = { showTourDialog = true },
                            onLogout = {
                                appContainer.sessionManager.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            showBackButton = false
                        )
                    }
                }
            }
        }

        // Интерактивный тур по приложению
        if (showTourDialog) {
            StudToolAppTourDialog(
                isEnglish = isEn,
                onDismiss = {
                    showTourDialog = false
                    scope.launch { appContainer.settingsRepository.setHasSeenTour(true) }
                },
                onNavigateToGenerator = {
                    showTourDialog = false
                    scope.launch { appContainer.settingsRepository.setHasSeenTour(true) }
                    navigateToTab(Screen.Generator.route)
                },
                onNavigateToDeposit = {
                    showTourDialog = false
                    scope.launch { appContainer.settingsRepository.setHasSeenTour(true) }
                    navController.navigate(Screen.Coins.route)
                },
                onNavigateToSettings = {
                    showTourDialog = false
                    scope.launch { appContainer.settingsRepository.setHasSeenTour(true) }
                    navigateToTab(Screen.Settings.route)
                }
            )
        }

        // Анимированная заставка при запуске
        StudToolSplashOverlay(
            visible = showSplash,
            isEnglish = isEn,
            onDismiss = {
                showSplash = false
                if (!appContainer.sessionManager.hasSeenTour()) {
                    showTourDialog = true
                }
            },
            onStartTour = {
                showSplash = false
                showTourDialog = true
            }
        )
    }
}
