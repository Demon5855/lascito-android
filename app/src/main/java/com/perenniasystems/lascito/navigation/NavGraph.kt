package com.perenniasystems.lascito.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.perenniasystems.lascito.feature.legacy.AltarScreen
import com.perenniasystems.lascito.feature.auth.LoginScreen
import com.perenniasystems.lascito.feature.auth.RegisterScreen
import com.perenniasystems.lascito.feature.custody.CapsuleVaultScreen
import com.perenniasystems.lascito.feature.custody.CapsuleDetailScreen
import com.perenniasystems.lascito.feature.legacy.DashboardScreen
import com.perenniasystems.lascito.feature.custody.GuardianScreen
import com.perenniasystems.lascito.feature.custody.InheritanceRoute
import com.perenniasystems.lascito.feature.memory.AddMemoryScreen
import com.perenniasystems.lascito.feature.legacy.AddProfileScreen
import com.perenniasystems.lascito.feature.settings.UserSettingsScreen
import com.perenniasystems.lascito.feature.memory.AddMemoryViewModel
import com.perenniasystems.lascito.feature.legacy.AddProfileViewModel
import com.perenniasystems.lascito.feature.auth.AuthViewModel

import com.perenniasystems.lascito.feature.auth.OnboardingScreen

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
    val authViewModel: AuthViewModel = hiltViewModel()

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
                onNavigateToClaimLegacy = { invitationId ->
                    navController.navigate(Screen.ClaimLegacy.createRoute(invitationId))
                },
                onNavigateToInheritance = { creatorUid ->
                    navController.navigate(Screen.Inheritance.createRoute(creatorUid))
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
            val addProfileViewModel: AddProfileViewModel = hiltViewModel()

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
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("readOnly") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            AltarScreen(
                profileId      = profileId,
                readOnly       = backStackEntry.arguments?.getBoolean("readOnly") ?: false,
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
            val addMemoryViewModel: AddMemoryViewModel = hiltViewModel()

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

        composable(
            route     = Screen.ClaimLegacy.route,
            arguments = listOf(navArgument("invitationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val invitationId = backStackEntry.arguments?.getString("invitationId") ?: return@composable
            com.perenniasystems.lascito.feature.custody.ClaimLegacyScreen(
                invitationId   = invitationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVault = { creatorUid ->
                    navController.navigate(Screen.Inheritance.createRoute(creatorUid)) {
                        popUpTo(Screen.ClaimLegacy.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Screen.Inheritance.route,
            arguments = listOf(navArgument("creatorUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val creatorUid = backStackEntry.arguments?.getString("creatorUid") ?: return@composable
            InheritanceRoute(
                creatorUid     = creatorUid,
                onNavigateBack = { navController.popBackStack() },
                onOpenLegacy   = { profileId ->
                    navController.navigate(Screen.Altar.createRoute(profileId, readOnly = true))
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


