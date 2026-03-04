package com.perenniasystems.lascito.data.repository

import com.perenniasystems.lascito.data.model.DeadManSwitch
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
}
