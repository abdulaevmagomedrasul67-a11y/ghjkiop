package com.example.core.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Generator : Screen("generator")
    data object History : Screen("history")
    data object Profile : Screen("profile")
    data object Support : Screen("support")
    data object Settings : Screen("settings")
    data object Coins : Screen("coins")
}
