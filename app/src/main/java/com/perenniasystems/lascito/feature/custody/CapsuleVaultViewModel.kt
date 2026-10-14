package com.perenniasystems.lascito.feature.custody

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perenniasystems.lascito.data.model.TimeCapsule
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.core.common.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class VaultUiState(
    val isLoading: Boolean          = false,
    val capsules:  List<TimeCapsule> = emptyList(),
    val error:     Int?             = null
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CapsuleVaultViewModel @Inject constructor(
    private val repository: GuardianRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        loadCapsules()
    }

    // ── Carga ─────────────────────────────────────────────────────────────────

    /**
     * Recupera la lista completa de cápsulas del usuario desde Firestore,
     * ordenadas por [TimeCapsule.createdAt] descendente.
     */
    fun loadCapsules() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val capsules = repository.getTimeCapsules()
                _uiState.update { it.copy(isLoading = false, capsules = capsules) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = ErrorHandler.mapExceptionToResId(e)
                    )
                }
            }
        }
    }

    // ── Borrado ───────────────────────────────────────────────────────────────

    /**
     * Borra la cápsula y su archivo multimedia (si existe), luego recarga
     * la lista para reflejar el cambio en la UI.
     *
     * Optimización: elimina la cápsula de la lista local inmediatamente antes
     * de confirmar con el servidor (optimistic update), y revierte si falla.
     */
    fun deleteCapsule(capsule: TimeCapsule) {
        // Optimistic update: quitar de la lista local al instante
        val previousList = _uiState.value.capsules
        _uiState.update { it.copy(capsules = it.capsules.filter { c -> c.capsuleId != capsule.capsuleId }) }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTimeCapsule(capsule.capsuleId, capsule.mediaUrl)
                .onSuccess {
                    // Lista ya actualizada de forma optimista; recarga para consistencia
                    loadCapsules()
                }
                .onFailure { e ->
                    // Revertir: restaurar lista anterior
                    _uiState.update {
                        it.copy(
                            capsules = previousList,
                            error    = ErrorHandler.mapExceptionToResId(e as Exception)
                        )
                    }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
