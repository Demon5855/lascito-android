package com.perenniasystems.lascito.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.perenniasystems.lascito.data.model.DeadManSwitch
import com.perenniasystems.lascito.data.model.GuardianConfig
import com.perenniasystems.lascito.data.model.LegacyGuardian
import com.perenniasystems.lascito.data.model.LegacyInvitation
import com.perenniasystems.lascito.data.model.InvitationStatus
import com.perenniasystems.lascito.data.model.InvalidPinException
import com.perenniasystems.lascito.data.model.LegacyAlreadyClaimedException
import com.perenniasystems.lascito.data.model.LegacyBlockedException
import com.perenniasystems.lascito.data.model.LegacyExpiredException
import com.perenniasystems.lascito.data.model.LegacyNotConfiguredException
import com.perenniasystems.lascito.data.model.LegacyNotFoundException
import com.perenniasystems.lascito.data.model.TimeCapsule
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Implementación Firestore de [GuardianRepository].
 *
 * Rutas:
 *   Guardián / Switch → campo embebido en `users/{uid}` (merge parcial)
 *   Cápsulas          → subcolección `users/{uid}/capsules/{capsuleId}`
 *   Hash del PIN      → `users/{uid}/private/inheritance` (solo escritura)
 *
 * Todas las operaciones de red están protegidas por [withTimeout] de 10 s
 * para garantizar Fail Fast ante inestabilidad de conexión.
 */
@Singleton
class FirebaseGuardianRepository @Inject constructor(
    private val db:      FirebaseFirestore,
    private val auth:    FirebaseAuth,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions
) : GuardianRepository {

    private companion object {
        const val PIN_HASH_COST = 10
    }

    private val invitationsCollection = db.collection("legacyInvitations")

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Devuelve el UID del usuario autenticado o lanza [IllegalStateException].
     * Centraliza la guarda de autenticación para todas las funciones.
     */
    private fun requireUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("No hay un usuario autenticado.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun capsulesCol(uid: String) =
        userDoc(uid).collection("capsules")

    private fun inheritanceSecretDoc(uid: String) =
        userDoc(uid).collection("private").document("inheritance")

    // ── Guardián del Legado ───────────────────────────────────────────────────

    /**
     * Lee en una única operación el documento `users/{uid}` y extrae los
     * objetos embebidos [LegacyGuardian] y [DeadManSwitch].
     *
     * Usa [DocumentSnapshot.get] con nombre de campo en lugar de [toObject]
     * para evitar problemas si el esquema del documento User cambia.
     * Si el documento no existe o un campo es null, devuelve null en ese slot
     * (estado "Sin configurar") sin lanzar excepción.
     */
    override suspend fun getGuardianConfig(): Result<GuardianConfig> =
        runCatching {
            val uid = requireUid()
            val snapshot = withTimeout(10_000L) { userDoc(uid).get().await() }

            val guardian = runCatching {
                snapshot.get("legacyGuardian")?.let { raw ->
                    @Suppress("UNCHECKED_CAST")
                    val map = raw as? Map<String, Any?> ?: return@let null
                    LegacyGuardian(
                        email         = map["email"] as? String,
                        name          = map["name"] as? String,
                        status        = runCatching {
                            com.perenniasystems.lascito.data.model.GuardianStatus
                                .valueOf((map["status"] as? String) ?: "PENDING")
                        }.getOrDefault(com.perenniasystems.lascito.data.model.GuardianStatus.PENDING),
                        lastGuardianUpdatedAt = map["lastGuardianUpdatedAt"] as? Long
                    )
                }
            }.getOrNull()

            val deadManSwitch = runCatching {
                snapshot.get("deadManSwitch")?.let { raw ->
                    @Suppress("UNCHECKED_CAST")
                    val map = raw as? Map<String, Any?> ?: return@let null
                    DeadManSwitch(
                        isActive                = (map["isActive"] as? Boolean) ?: (map["active"] as? Boolean) ?: false,
                        inactivityThresholdDays = ((map["inactivityThresholdDays"] as? Long)?.toInt()) ?: 90,
                        lastProofOfLife         = (map["lastProofOfLife"] as? Long) ?: 0L,
                        unlockStatus            = runCatching {
                            com.perenniasystems.lascito.data.model.UnlockStatus
                                .valueOf((map["unlockStatus"] as? String) ?: "LOCKED")
                        }.getOrDefault(com.perenniasystems.lascito.data.model.UnlockStatus.LOCKED)
                    )
                }
            }.getOrNull()

            GuardianConfig(guardian = guardian, deadManSwitch = deadManSwitch)
        }

    /**
     * Persiste el [LegacyGuardian] del usuario mediante un merge parcial.
     * Solo sobreescribe el campo `legacyGuardian`; el resto del documento
     * permanece intacto.
     *
     * La pista del PIN solo se escribe cuando llega una nueva: guardar el
     * guardián sin cambiar el PIN conserva la pista existente.
     */
    override suspend fun updateGuardian(guardian: LegacyGuardian): Result<Unit> =
        runCatching {
            val uid = requireUid()
            val fields = buildMap<String, Any?> {
                put("email", guardian.email)
                put("name", guardian.name)
                put("status", guardian.status.name)
                put("lastGuardianUpdatedAt", guardian.lastGuardianUpdatedAt)
                if (guardian.encryptedPinHint != null && guardian.pinHintIv != null) {
                    put("encryptedPinHint", guardian.encryptedPinHint)
                    put("pinHintIv", guardian.pinHintIv)
                }
            }
            withTimeout(10_000L) {
                userDoc(uid)
                    .set(mapOf("legacyGuardian" to fields), SetOptions.merge())
                    .await()
            }
        }

    // ── Dead Man's Switch ─────────────────────────────────────────────────────

    /**
     * Persiste la configuración del [DeadManSwitch] mediante un merge parcial.
     * Solo sobreescribe el campo `deadManSwitch`; el resto permanece intacto.
     */
    override suspend fun updateDeadManSwitchConfig(config: DeadManSwitch): Result<Unit> =
        runCatching {
            val uid = requireUid()
            withTimeout(10_000L) {
                userDoc(uid)
                    .set(mapOf("deadManSwitch" to config), SetOptions.merge())
                    .await()
            }
        }

    /**
     * Registra una Prueba de Vida actualizando SOLO el campo
     * `deadManSwitch.lastProofOfLife` de forma atómica.
     *
     * CRÍTICO: usa [update] en lugar de [get] + write para evitar descargar
     * el documento completo. Si el campo aun no existe (usuario nuevo que
     * nunca configuró el switch), [update] lanzará una excepción → fallback
     * con [set] + merge para crear la estructura mínima inicial.
     */
    override suspend fun registerProofOfLife(): Result<Unit> =
        runCatching {
            val uid = requireUid()
            val now = System.currentTimeMillis()
            withTimeout(10_000L) {
                try {
                    // Camino óptimo: actualización atómica de campo único (O(1))
                    userDoc(uid)
                        .update("deadManSwitch.lastProofOfLife", now)
                        .await()
                } catch (e: Exception) {
                    // Fallback: el mapa deadManSwitch aún no existe en el documento.
                    // Creamos la estructura mínima vía merge para no pisar otros campos.
                    userDoc(uid)
                        .set(
                            mapOf(
                                "deadManSwitch" to mapOf("lastProofOfLife" to now)
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }
            }
        }

    // ── Cápsulas del Tiempo ───────────────────────────────────────────────────

    /**
     * Crea o sobreescribe una Cápsula del Tiempo en la subcolección
     * `users/{uid}/capsules/{capsuleId}`.
     *
     * El [TimeCapsule.capsuleId] debe ser generado en el ViewModel
     * antes de llamar a esta función.
     */
    override suspend fun createTimeCapsule(capsule: TimeCapsule): Result<Unit> =
        runCatching {
            val uid = requireUid()
            withTimeout(10_000L) {
                capsulesCol(uid)
                    .document(capsule.capsuleId)
                    .set(capsule)
                    .await()
            }
        }

    /**
     * Devuelve todas las Cápsulas del Tiempo del usuario, ordenadas por
     * [TimeCapsule.createdAt] descendente (más reciente primero).
     */
    override suspend fun getTimeCapsules(): List<TimeCapsule> =
        withTimeout(10_000L) {
            val uid = requireUid()
            capsulesCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(TimeCapsule::class.java)
        }

    override suspend fun getTimeCapsule(capsuleId: String): Result<TimeCapsule> =
        runCatching {
            val uid = requireUid()
            withTimeout(10_000L) {
                val snapshot = capsulesCol(uid).document(capsuleId).get().await()
                snapshot.toObject(TimeCapsule::class.java)
                    ?: throw Exception("Capsule not found")
            }
        }

    /**
     * Elimina la cápsula y su archivo en Storage (si existe).
     *
     * Storage se borra primero usando tolerancia a fallo (el archivo puede ya no
     * existir). Firestore se borra solo si el paso de Storage no lanzó una
     * excepción no tolerable. Ambas operaciones tienen su propio timeout.
     */
    override suspend fun deleteTimeCapsule(capsuleId: String, mediaUrl: String?): Result<Unit> =
        runCatching {
            val uid = requireUid()

            // 1. Borrar archivo de Storage (tolerante a "object not found")
            if (!mediaUrl.isNullOrBlank()) {
                withTimeout(10_000L) {
                    try {
                        val ref = storage.getReferenceFromUrl(mediaUrl)
                        ref.delete().await()
                    } catch (e: com.google.firebase.storage.StorageException) {
                        if (e.errorCode != com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                            throw e   // relanza solo si no es "ya borrado"
                        }
                    }
                }
            }

            // 2. Borrar documento Firestore
            withTimeout(10_000L) {
                capsulesCol(uid).document(capsuleId).delete().await()
            }
        }

    // ── Reclamación de Legados (Phase 4) ───────────────────────────────────

    override suspend fun getPendingInvitations(): Result<List<LegacyInvitation>> = runCatching {
        val email = auth.currentUser?.email ?: throw IllegalStateException("User email not found")
        withTimeout(10_000L) {
            invitationsCollection
                .whereEqualTo("guardianEmail", email.lowercase())
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
                .documents
                .map { it.toInvitation() }
                .sortedBy { it.createdAt }
        }
    }

    override suspend fun getPendingInvitation(invitationId: String): Result<LegacyInvitation?> = runCatching {
        withTimeout(10_000L) {
            val doc = invitationsCollection.document(invitationId).get().await()
            if (!doc.exists()) return@withTimeout null
            doc.toInvitation().takeIf { it.status == InvitationStatus.PENDING }
        }
    }

    override suspend fun getClaimedInheritances(): Result<List<com.perenniasystems.lascito.data.model.LegacyVault>> = runCatching {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User not authenticated")
        val email = currentUser.email ?: throw IllegalStateException("User email not found")
        withTimeout(10_000L) {
            invitationsCollection
                .whereEqualTo("guardianEmail", email.lowercase())
                .whereEqualTo("status", "CLAIMED")
                .get()
                .await()
                .documents
                .filter { it.getString("claimedByUid") == currentUser.uid }
                .map { doc ->
                    com.perenniasystems.lascito.data.model.LegacyVault(
                        id = doc.id,
                        creatorUid = doc.getString("creatorUid") ?: "",
                        creatorName = doc.getString("creatorName") ?: "",
                        vaultContentRef = doc.getString("vaultContentRef") ?: "",
                        claimedAt = doc.getLong("claimedAt") ?: 0L
                    )
                }
        }
    }

    private fun DocumentSnapshot.toInvitation() = LegacyInvitation(
        id = id,
        creatorUid = getString("creatorUid") ?: "",
        creatorName = getString("creatorName") ?: "",
        guardianEmail = getString("guardianEmail") ?: "",
        status = runCatching { InvitationStatus.valueOf(getString("status") ?: "PENDING") }
            .getOrDefault(InvitationStatus.PENDING),
        // El Cron Job escribe createdAt como serverTimestamp
        createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        expiresAt = getLong("expiresAt"),
        vaultContentRef = getString("vaultContentRef") ?: ""
    )

    override suspend fun setInheritancePin(pin: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val accessPinHash = BCrypt.withDefaults().hashToString(PIN_HASH_COST, pin.toCharArray())
        withTimeout(10_000L) {
            inheritanceSecretDoc(uid)
                .set(
                    mapOf(
                        "accessPinHash" to accessPinHash,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        }
    }

    override suspend fun claimInheritance(invitationId: String, pin: String): Result<com.perenniasystems.lascito.data.model.LegacyVault> = runCatching {
        auth.currentUser ?: throw IllegalStateException("User not authenticated")

        val response = try {
            withTimeout(15_000L) {
                functions.getHttpsCallable("claimInheritance")
                    .call(mapOf("invitationId" to invitationId, "pin" to pin))
                    .await()
            }
        } catch (e: FirebaseFunctionsException) {
            throw e.toClaimException()
        }

        val data = response.getData() as? Map<*, *>
        com.perenniasystems.lascito.data.model.LegacyVault(
            id = data?.get("invitationId") as? String ?: invitationId,
            creatorUid = data?.get("creatorUid") as? String ?: "",
            creatorName = data?.get("creatorName") as? String ?: "",
            vaultContentRef = data?.get("vaultContentRef") as? String ?: "",
            claimedAt = (data?.get("claimedAt") as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    /** Traduce el `reason` devuelto por `claimInheritance` a las excepciones de dominio. */
    private fun FirebaseFunctionsException.toClaimException(): Exception {
        val details = details as? Map<*, *>
        return when (details?.get("reason") as? String) {
            "INVALID_PIN" -> InvalidPinException((details["attemptsRemaining"] as? Number)?.toInt() ?: 0)
            "BLOCKED" -> LegacyBlockedException()
            "EXPIRED" -> LegacyExpiredException()
            "NOT_FOUND" -> LegacyNotFoundException()
            "ALREADY_CLAIMED" -> LegacyAlreadyClaimedException()
            "NOT_CONFIGURED" -> LegacyNotConfiguredException()
            else -> this
        }
    }
}
