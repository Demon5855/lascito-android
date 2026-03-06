package com.perenniasystems.lascito.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.perenniasystems.lascito.data.model.DeadManSwitch
import com.perenniasystems.lascito.data.model.GuardianConfig
import com.perenniasystems.lascito.data.model.LegacyGuardian
import com.perenniasystems.lascito.data.model.TimeCapsule
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Implementación Firestore de [GuardianRepository].
 *
 * Rutas:
 *   Guardián / Switch → campo embebido en `users/{uid}` (merge parcial)
 *   Cápsulas          → subcolección `users/{uid}/capsules/{capsuleId}`
 *
 * Todas las operaciones de red están protegidas por [withTimeout] de 10 s
 * para garantizar Fail Fast ante inestabilidad de conexión.
 */
class FirebaseGuardianRepository(
    private val db:      FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth:    FirebaseAuth      = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage   = FirebaseStorage.getInstance()
) : GuardianRepository {

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
                        accessPinHash = map["accessPinHash"] as? String
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
     */
    override suspend fun updateGuardian(guardian: LegacyGuardian): Result<Unit> =
        runCatching {
            val uid = requireUid()
            withTimeout(10_000L) {
                userDoc(uid)
                    .set(mapOf("legacyGuardian" to guardian), SetOptions.merge())
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
}
