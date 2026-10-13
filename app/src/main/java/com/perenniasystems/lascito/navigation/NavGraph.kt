package com.perenniasystems.lascito.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.perenniasystems.lascito.ui.screens.altar.AltarScreen
import com.perenniasystems.lascito.ui.screens.auth.LoginScreen
import com.perenniasystems.lascito.ui.screens.auth.RegisterScreen
import com.perenniasystems.lascito.ui.screens.capsule.CapsuleVaultScreen
import com.perenniasystems.lascito.ui.screens.capsule.CapsuleDetailScreen
import com.perenniasystems.lascito.ui.screens.dashboard.DashboardScreen
import com.perenniasystems.lascito.ui.screens.guardian.GuardianScreen
import com.perenniasystems.lascito.ui.screens.memory.AddMemoryScreen
import com.perenniasystems.lascito.ui.screens.profile.AddProfileScreen
import com.perenniasystems.lascito.ui.screens.settings.UserSettingsScreen
import com.perenniasystems.lascito.viewmodel.AddMemoryViewModel
import com.perenniasystems.lascito.viewmodel.AddProfileViewModel
import com.perenniasystems.lascito.viewmodel.AuthViewModel

import com.perenniasystems.lascito.ui.screens.onboarding.OnboardingScreen

/**
 * NavGraph wires up all application destinations.
 *
 * Phase 1 flow:
 *   Login → Dashboard → AddProfile    (new altar)
 *                      → Altar         → AddMemory
 *                      → UserSettings  (Mi Cuenta)
 * Phase 2: Replace mocks with Firebase/Firestore.
 */
@Composable
fun LascitoNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    onFinishOnboarding: () -> Unit = {},
    onShowSnackbar: (String, String?, () -> Unit) -> Unit = { _, _, _ -> }
) {
    // Shared AuthViewModel — survives across Login ↔ Register navigation
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = onFinishOnboarding
            )
        }

        composable(Screen.Login.route) {
            // Reset auth state when entering Login (clears errors from Register)
            LaunchedEffect(Unit) { authViewModel.resetState() }
            LoginScreen(
                authViewModel         = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onShowSnackbar = onShowSnackbar
            )
        }

        composable(Screen.Register.route) {
            // Reset auth state when entering Register (clears errors from Login)
            LaunchedEffect(Unit) { authViewModel.resetState() }
            RegisterScreen(
                authViewModel  = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAltar        = { profileId ->
                    navController.navigate(Screen.Altar.createRoute(profileId))
                },
                onNavigateToAddProfile   = { profileId ->
                    navController.navigate(Screen.AddProfile.createRoute(profileId))
                },
                onNavigateToUserSettings = {
                    navController.navigate(Screen.UserSettings.route)
                },
                onNavigateToClaimLegacy = {
                    navController.navigate(Screen.ClaimLegacy.route)
                }
            )
        }

        composable(
            route = Screen.AddProfile.route,
            arguments = listOf(navArgument("profileId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")
            val addProfileViewModel: AddProfileViewModel = viewModel()
            
            LaunchedEffect(profileId) {
                if (profileId != null) {
                    addProfileViewModel.loadProfile(profileId)
                }
            }

            AddProfileScreen(
                viewModel      = addProfileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }

        composable(
            route     = Screen.Altar.route,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            AltarScreen(
                profileId      = profileId,
                onNavigateBack = { navController.popBackStack() },
                onAddMemory    = { navController.navigate(Screen.AddMemory.createRoute(profileId)) },
                onEditMemory   = { memoryId -> navController.navigate(Screen.AddMemory.createRoute(profileId, memoryId)) },
                onEditProfileClick = { navController.navigate(Screen.AddProfile.createRoute(profileId)) },
                onDeleteProfileClick = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.AddMemory.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("memoryId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            val memoryId  = backStackEntry.arguments?.getString("memoryId")
            val addMemoryViewModel: AddMemoryViewModel = viewModel()

            LaunchedEffect(memoryId) {
                if (memoryId != null) {
                    addMemoryViewModel.loadMemory(memoryId, profileId)
                }
            }

            AddMemoryScreen(
                profileId      = profileId,
                viewModel      = addMemoryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }

        composable(Screen.UserSettings.route) {
            UserSettingsScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToGuardian = {
                    navController.navigate(Screen.Guardian.route)
                },
                onLogOut           = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onShowSnackbar = onShowSnackbar
            )
        }

        composable(Screen.Guardian.route) {
            GuardianScreen(
                onNavigateBack      = { navController.popBackStack() },
                onNavigateToCapsule = { navController.navigate(Screen.CapsuleVault.route) },
                onShowSnackbar      = onShowSnackbar
            )
        }

        composable(Screen.ClaimLegacy.route) {
            com.perenniasystems.lascito.ui.screens.guardian.ClaimLegacyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVault = { 
                    // FASE 4.2 Navegar a la cápsula o bóveda
                }
            )
        }

        composable(Screen.CapsuleVault.route) {
            CapsuleVaultScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(Screen.AddTimeCapsule.route) },
                onNavigateToDetail = { capsuleId -> navController.navigate(Screen.CapsuleDetail.createRoute(capsuleId)) }
            )
        }

        composable(Screen.AddTimeCapsule.route) {
            CapsuleDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }

        composable(
            route = Screen.CapsuleDetail.route,
            arguments = listOf(
                navArgument("capsuleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val capsuleId = backStackEntry.arguments?.getString("capsuleId") ?: return@composable
            CapsuleDetailScreen(
                capsuleId = capsuleId,
                onNavigateBack = { navController.popBackStack() },
                onShowSnackbar = onShowSnackbar
            )
        }
    }
}


