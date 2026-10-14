package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.DeadManSwitch
import com.perenniasystems.lascito.data.model.GuardianConfig
import com.perenniasystems.lascito.data.model.LegacyGuardian
import com.perenniasystems.lascito.data.model.TimeCapsule

/**
 * Contrato para todas las operaciones del "Fideicomiso Digital":
 * gestión del Guardián del Legado, configuración del Dead Man's Switch
 * y administración de las Cápsulas del Tiempo.
 *
 * Implementación prevista: [FirebaseGuardianRepository]
 * Rutas Firestore:
 *   - Guardián y Switch → campo embebido en `users/{uid}`
 *   - Cápsulas          → subcolección `users/{uid}/capsules`
 */
interface GuardianRepository {

    // ── Guardián del Legado ────────────────────────────────────────────────

    /**
     * Lee en una sola operación la configuración del Guardián y del
     * Dead Man's Switch desde el documento `users/{uid}`.
     *
     * Si el documento no existe o los campos son null, devuelve
     * [GuardianConfig] con ambos campos a null (estado "Sin configurar").
     */
    suspend fun getGuardianConfig(): Result<GuardianConfig>

    /**
     * Persiste (crea o sobreescribe) la configuración del Guardián en el
     * documento del usuario actual mediante un merge parcial.
     */
    suspend fun updateGuardian(guardian: LegacyGuardian): Result<Unit>

    // ── Dead Man's Switch ──────────────────────────────────────────────────

    /**
     * Persiste (crea o sobreescribe) la configuración del Dead Man's Switch
     * en el documento del usuario actual mediante un merge parcial.
     */
    suspend fun updateDeadManSwitchConfig(config: DeadManSwitch): Result<Unit>

    /**
     * Registra una Prueba de Vida: escribe el timestamp actual en el campo
     * `deadManSwitch.lastProofOfLife` sin modificar el resto de la config.
     *
     * Debe llamarse cada vez que el usuario abre la app activamente o
     * confirma manualmente desde la pantalla de Ajustes.
     */
    suspend fun registerProofOfLife(): Result<Unit>

    // ── Cápsulas del Tiempo ────────────────────────────────────────────────

    /**
     * Crea o sobreescribe una Cápsula del Tiempo en la subcolección
     * `users/{uid}/capsules/{capsuleId}`.
     *
     * El [TimeCapsule.capsuleId] debe ser generado en el ViewModel antes
     * de llamar a esta función (UUID.randomUUID().toString()).
     */
    suspend fun createTimeCapsule(capsule: TimeCapsule): Result<Unit>

    /**
     * Devuelve todas las Cápsulas del Tiempo del usuario actual,
     * ordenadas por fecha de creación descendente.
     *
     * @throws Exception si el usuario no está autenticado o falla la red.
     */
    suspend fun getTimeCapsules(): List<TimeCapsule>

    /**
     * Recupera una Cápsula del Tiempo específica por su ID.
     */
    suspend fun getTimeCapsule(capsuleId: String): Result<TimeCapsule>

    /**
     * Elimina una Cápsula del Tiempo y su archivo multimedia asociado (si existe).
     *
     * Orden de operaciones:
     *  1. Si [mediaUrl] no es null, borra primero el archivo de Firebase Storage.
     *     Fallos de Storage se toleran (el archivo puede ya no existir).
     *  2. Elimina el documento `users/{uid}/capsules/{capsuleId}` de Firestore.
     *
     * @param capsuleId ID del documento a borrar.
     * @param mediaUrl  URL de descarga del archivo en Storage, o null si la cápsula es solo texto.
     */
    suspend fun deleteTimeCapsule(capsuleId: String, mediaUrl: String?): Result<Unit>

    // ── Reclamación de Legados (Phase 4) ───────────────────────────────────

    /**
     * Verifica si el correo actual tiene legados pendientes por reclamar.
     */
    suspend fun checkPendingLegacies(email: String): Result<Boolean>
    
    /**
     * Recupera una invitación específica para reclamar un legado.
     */
    suspend fun getLegacyInvitation(email: String): Result<com.perenniasystems.lascito.data.model.LegacyInvitation?>

    /**
     * Herencias que el usuario actual ya reclamó (invitación CLAIMED con su UID).
     */
    suspend fun getClaimedInheritances(): Result<List<com.perenniasystems.lascito.data.model.LegacyVault>>

    /**
     * Guarda el PIN de herencia del usuario actual como hash bcrypt en un documento
     * que ningún cliente puede leer. El PIN en claro nunca se persiste.
     */
    suspend fun setInheritancePin(pin: String): Result<Unit>

    /**
     * Reclama la herencia pendiente del usuario actual. La verificación del PIN y el
     * conteo de intentos ocurren en la Cloud Function `claimInheritance`.
     * @throws InvalidPinException si el PIN es incorrecto
     * @throws LegacyBlockedException si excedió el límite de intentos
     */
    suspend fun claimInheritance(pin: String): Result<com.perenniasystems.lascito.data.model.LegacyVault>
}
