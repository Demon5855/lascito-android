package com.perenniasystems.lascito.data.model

data class LegacyInvitation(
    val id: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val guardianEmail: String = "",
    val status: InvitationStatus = InvitationStatus.PENDING,
    val createdAt: Long = 0L,
    val expiresAt: Long? = null,
    val vaultContentRef: String = ""
)

enum class InvitationStatus {
    PENDING,
    CLAIMED,
    EXPIRED,
    BLOCKED
}

data class LegacyVault(
    val id: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val vaultContentRef: String = "",
    val claimedAt: Long = 0L,
    // Compatibilidad para UI temporal:
    val benefactorName: String = "",
    val capsules: List<TimeCapsule> = emptyList()
)

class InvalidPinException(val attemptsRemaining: Int) : Exception("Invalid PIN. Attempts remaining: $attemptsRemaining")
class LegacyNotFoundException(message: String = "Legacy not found") : Exception(message)
class LegacyExpiredException(message: String = "Legacy expired") : Exception(message)
class LegacyBlockedException(message: String = "Legacy blocked due to too many failed attempts") : Exception(message)
class LegacyAlreadyClaimedException(message: String = "Legacy already claimed") : Exception(message)
