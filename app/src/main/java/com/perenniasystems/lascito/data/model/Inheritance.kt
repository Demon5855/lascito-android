package com.perenniasystems.lascito.data.model

data class InheritanceInvitation(
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

data class Inheritance(
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
class InheritanceNotFoundException(message: String = "Inheritance not found") : Exception(message)
class InheritanceExpiredException(message: String = "Inheritance expired") : Exception(message)
class InheritanceBlockedException(message: String = "Inheritance blocked due to too many failed attempts") : Exception(message)
class InheritanceNotConfiguredException(message: String = "Inheritance PIN not configured by its creator") : Exception(message)
class InheritanceAlreadyClaimedException(message: String = "Inheritance already claimed") : Exception(message)
