package com.perenniasystems.lascito.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.DeadManSwitch
import com.perenniasystems.lascito.data.model.LegacyGuardian
import com.perenniasystems.lascito.data.model.UnlockStatus
import com.perenniasystems.lascito.data.repository.FirebaseGuardianRepository
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.util.ErrorHandler
import com.perenniasystems.lascito.util.ValidationRules
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class GuardianUiState(
    val isLoading:       Boolean = false,
    val guardianName:    String  = "",
    val guardianEmail:   String  = "",
    /** 4-digit PIN de acceso para el heredero. Se persiste solo como hash bcrypt. */
    val guardianPin:     String  = "",
    /** Pista introducida por el usuario, sin encriptar, solo para estado de UI */
    val guardianPinHint: String  = "",
    /** Error detectado por el validation rules */
    @StringRes val pinHintError: Int? = null,
    val isSwitchActive:  Boolean = false,
    /** Umbral de inactividad en días. Opciones válidas: 30, 90, 180, 365. */
    val inactivityDays:  Int     = 90,
    /** Timestamp (ms) de la última Prueba de Vida registrada. */
    val lastProofOfLife: Long    = 0L,
    @StringRes val error: Int?   = null,
    val isSaved:         Boolean = false,
    /** Controla la visibilidad del diálogo de enfriamiento anti-spam (7 días). */
    val showCooldownDialog: Boolean = false,
    val cooldownDaysRemaining: Int = 0,
    /** Timestamp local guardado para validación de la próxima edición. */
    val lastGuardianUpdatedAt: Long? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class GuardianViewModel(
    private val repository: GuardianRepository = FirebaseGuardianRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuardianUiState())
    val uiState: StateFlow<GuardianUiState> = _uiState.asStateFlow()

    companion object {
        private const val GUARDIAN_UPDATE_COOLDOWN_DAYS = 7
        private const val MILLIS_PER_DAY = 86_400_000L
    }

    init {
        loadConfig()
    }

    // ── Carga ─────────────────────────────────────────────────────────────────

    /**
     * Lee la configuración actual del Guardián y del Dead Man's Switch desde
     * Firestore en una única consulta al documento `users/{uid}`.
     *
     * Reglas de mapeo:
     *  - [LegacyGuardian] existe  → rellena nombre y email. El PIN no se
     *    restaura por seguridad (el hash no es reversible); la UI lo deja vacío.
     *  - [DeadManSwitch] existe   → actualiza isSwitchActive, inactivityDays y
     *    lastProofOfLife.
     *  - lastProofOfLife == 0L   → fallback a System.currentTimeMillis() para
     *    que el footer nunca muestre "Nunca" en una cuenta que ya tiene actividad.
     *  - Campos null / doc vacío → estado "Sin configurar"; la UI muestra
     *    valores por defecto sin bloquear al usuario.
     */
    fun loadConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getGuardianConfig()
                .onSuccess { config ->
                    val g   = config.guardian
                    val dms = config.deadManSwitch

                    _uiState.update { state ->
                        state.copy(
                            isLoading    = false,
                            // ── Heredero ───────────────────────────────
                            guardianName  = g?.name?.orEmpty()  ?: "",
                            guardianEmail = g?.email?.orEmpty() ?: "",
                            // PIN nunca se rellena: el hash no es reversible.
                            guardianPin   = "",
                            // ── Dead Man's Switch ──────────────────────
                            isSwitchActive  = dms?.isActive               ?: false,
                            inactivityDays  = dms?.inactivityThresholdDays ?: 90,
                            lastProofOfLife = (dms?.lastProofOfLife ?: 0L)
                                .let { ts -> if (ts == 0L) System.currentTimeMillis() else ts },
                            lastGuardianUpdatedAt = g?.lastGuardianUpdatedAt
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Fallback visual: al menos muestra un timestamp real
                            lastProofOfLife = System.currentTimeMillis(),
                            error = ErrorHandler.mapExceptionToResId(e as Exception)
                        )
                    }
                }
        }
    }

    // ── Mutadores de estado local (UI-only, sin I/O) ──────────────────────────

    fun onNameChange(name: String)    = _uiState.update { it.copy(guardianName = name) }
    fun onEmailChange(email: String)  = _uiState.update { it.copy(guardianEmail = email) }
    fun onPinChange(pin: String)      = _uiState.update { 
        it.copy(
            guardianPin = pin.filter { c -> c.isDigit() }.take(4),
            // Re-evaluar pista cada que cambia el pin
            pinHintError = ValidationRules.validatePinHint(
                pin = pin.filter { c -> c.isDigit() }.take(4), 
                hint = it.guardianPinHint, 
                isOptional = false
            )
        ) 
    }
    fun onPinHintChange(hint: String) = _uiState.update { 
        it.copy(
            guardianPinHint = hint,
            pinHintError = ValidationRules.validatePinHint(
                pin = it.guardianPin, 
                hint = hint, 
                isOptional = false
            )
        )
    }
    fun onSwitchChange(active: Boolean) = _uiState.update { it.copy(isSwitchActive = active) }
    fun onDaysChange(days: Int)       = _uiState.update { it.copy(inactivityDays = days) }
    fun clearError()                  = _uiState.update { it.copy(error = null) }
    fun clearSaved()                  = _uiState.update { it.copy(isSaved = false) }
    fun dismissCooldownDialog()       = _uiState.update { it.copy(showCooldownDialog = false) }

    // ── Persistencia ──────────────────────────────────────────────────────────

    /**
     * Persiste la configuración del Guardián y del Dead Man's Switch en Firestore
     * usando actualizaciones parciales (merge). Las dos llamadas se hacen en
     * secuencia para evitar condiciones de carrera sobre el mismo documento.
     */
    fun saveGuardianConfig() {
        val state = _uiState.value
        
        // Validación del Cooldown de 7 Días
        val lastUpdate = state.lastGuardianUpdatedAt
        if (lastUpdate != null) {
            val daysSinceLastUpdate = (System.currentTimeMillis() - lastUpdate) / MILLIS_PER_DAY
            
            if (daysSinceLastUpdate < GUARDIAN_UPDATE_COOLDOWN_DAYS) {
                val daysRemaining = GUARDIAN_UPDATE_COOLDOWN_DAYS - daysSinceLastUpdate
                _uiState.update {
                    it.copy(
                        showCooldownDialog = true,
                        cooldownDaysRemaining = daysRemaining.toInt()
                    )
                }
                return
            }
        }

        // Validación de Pista Segura antes de guardar
        if (state.guardianPin.isNotBlank()) {
            val hintError = ValidationRules.validatePinHint(
                pin = state.guardianPin, 
                hint = state.guardianPinHint, 
                isOptional = false
            )
            if (hintError != null) {
                _uiState.update { it.copy(error = hintError) }
                return
            }
        }

        // Estado previo para posible Rollback
        val previousState = state.copy()

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null, isSaved = false) }
            try {
                // Actualizamos estado en UI interinamente (optimistic update)
                val newTimestamp = System.currentTimeMillis()
                _uiState.update { it.copy(lastGuardianUpdatedAt = newTimestamp) }

                // 1. Encriptar pista si hay un nuevo PIN
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("User not logged in")
                val encryptedHintData = if (state.guardianPin.isNotBlank() && state.guardianPinHint.isNotBlank()) {
                    encryptPinHint(state.guardianPinHint, uid)
                } else null

                // 2. Guardar el PIN solo si se escribió uno nuevo (no pisar el existente)
                if (state.guardianPin.isNotBlank()) {
                    repository.setInheritancePin(state.guardianPin).getOrThrow()
                }

                // 3. Guardar configuración del Heredero
                val guardian = LegacyGuardian(
                    name          = state.guardianName.trim().ifBlank { null },
                    email         = state.guardianEmail.trim().ifBlank { null },
                    encryptedPinHint = encryptedHintData?.data,
                    pinHintIv = encryptedHintData?.iv,
                    lastGuardianUpdatedAt = newTimestamp
                )
                repository.updateGuardian(guardian).getOrThrow()

                // 4. Guardar configuración del Dead Man's Switch
                val switch = DeadManSwitch(
                    isActive               = state.isSwitchActive,
                    inactivityThresholdDays = state.inactivityDays,
                    lastProofOfLife        = state.lastProofOfLife,
                    unlockStatus           = UnlockStatus.LOCKED
                )
                repository.updateDeadManSwitchConfig(switch).getOrThrow()

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                // Ejecutando ROLLBACK en caso de fallo
                _uiState.value = previousState.copy(
                    isLoading = false,
                    error = ErrorHandler.mapExceptionToResId(e)
                )
            }
        }
    }

    /**
     * Registra manualmente una Prueba de Vida (botón de pánico/refresh).
     * Actualiza [GuardianUiState.lastProofOfLife] en la UI inmediatamente
     * y persiste el timestamp en Firestore de forma asíncrona.
     */
    fun triggerManualCheck() {
        val now = System.currentTimeMillis()
        _uiState.update { it.copy(lastProofOfLife = now) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.registerProofOfLife().getOrThrow()
            } catch (e: Exception) {
                // Fallo silencioso — el timestamp local ya se actualizó en la UI.
                // El servidor se sincronizará en la próxima sesión.
            }
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    data class EncryptedData(val data: String, val iv: String)

    /**
     * Encripta la pista usando AES-256 CBC.
     * Utiliza el UID del creador hasheado como clave simétrica.
     */
    private fun encryptPinHint(hint: String, uid: String): EncryptedData {
        // Derivar clave desde UID
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray())
        val key = SecretKeySpec(keyBytes, "AES")
        
        // Generar IV aleatorio
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        
        // Encriptar
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(hint.toByteArray())
        
        return EncryptedData(
            data = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP),
            iv = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
        )
    }
}
