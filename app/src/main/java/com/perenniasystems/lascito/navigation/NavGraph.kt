package com.perenniasystems.lascito.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.perenniasystems.lascito.ui.screens.altar.AltarScreen
import com.perenniasystems.lascito.ui.screens.auth.LoginScreen
import com.perenniasystems.lascito.ui.screens.auth.RegisterScreen
import com.perenniasystems.lascito.ui.screens.dashboard.DashboardScreen
import com.perenniasystems.lascito.ui.screens.memory.AddMemoryScreen

/**
 * NavGraph wires up all application destinations.
 *
 * Phase 1: Login → Dashboard → Altar → AddMemory (mock data, no auth gate).
 * Phase 2: Login will call Firebase Auth; write ops will persist to Firestore.
 */
@Composable
fun LascitoNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAltar = { profileId ->
                    navController.navigate(Screen.Altar.createRoute(profileId))
                }
            )
        }

        composable(
            route     = Screen.Altar.route,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            AltarScreen(
                profileId        = profileId,
                onNavigateBack   = { navController.popBackStack() },
                onAddMemory      = { navController.navigate(Screen.AddMemory.createRoute(profileId)) }
            )
        }

        composable(
            route     = Screen.AddMemory.route,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            AddMemoryScreen(
                profileId      = profileId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

