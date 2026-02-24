package com.perenniasystems.lascito.navigation

/**
 * Sealed hierarchy of Navigation destinations.
 * Each object maps to a unique route string used by Compose Navigation.
 */
sealed class Screen(val route: String) {
    data object Login       : Screen("login")
    data object Register    : Screen("register")
    data object Dashboard   : Screen("dashboard")
    data object AddProfile  : Screen("add_profile")
    data object Altar       : Screen("altar/{profileId}") {
        fun createRoute(profileId: String) = "altar/$profileId"
    }
    data object AddMemory   : Screen("add_memory/{profileId}") {
        fun createRoute(profileId: String) = "add_memory/$profileId"
    }
    data object UserSettings : Screen("user_settings")
}
