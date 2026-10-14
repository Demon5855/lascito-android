package com.perenniasystems.lascito.navigation

/**
 * Sealed hierarchy of Navigation destinations.
 * Each object maps to a unique route string used by Compose Navigation.
 */
sealed class Screen(val route: String) {
    data object Onboarding  : Screen("onboarding")
    data object Login       : Screen("login")
    data object Register    : Screen("register")
    data object Dashboard   : Screen("dashboard")
    data object AddProfile  : Screen("add_profile?profileId={profileId}") {
        fun createRoute(profileId: String? = null): String {
            return if (profileId != null) "add_profile?profileId=$profileId" else "add_profile"
        }
    }
    data object Altar       : Screen("altar/{profileId}?readOnly={readOnly}") {
        fun createRoute(profileId: String, readOnly: Boolean = false) = "altar/$profileId?readOnly=$readOnly"
    }
    data object AddMemory   : Screen("add_memory/{profileId}?memoryId={memoryId}") {
        fun createRoute(profileId: String, memoryId: String? = null): String {
            return if (memoryId != null) "add_memory/$profileId?memoryId=$memoryId" else "add_memory/$profileId"
        }
    }
    data object UserSettings    : Screen("user_settings")
    data object Guardian         : Screen("guardian")
    data object ClaimLegacy      : Screen("claim_legacy/{invitationId}") {
        fun createRoute(invitationId: String) = "claim_legacy/$invitationId"
    }
    data object Inheritance      : Screen("inheritance/{creatorUid}") {
        fun createRoute(creatorUid: String) = "inheritance/$creatorUid"
    }
    data object AddTimeCapsule   : Screen("add_time_capsule")
    data object CapsuleDetail    : Screen("capsule_detail/{capsuleId}") {
        fun createRoute(capsuleId: String) = "capsule_detail/$capsuleId"
    }
    data object CapsuleVault     : Screen("capsule_vault")
}
