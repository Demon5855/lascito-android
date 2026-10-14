package com.perenniasystems.lascito.viewmodel

import com.perenniasystems.lascito.data.repository.PreferencesRepository
import com.perenniasystems.lascito.data.repository.SessionRepository
import com.perenniasystems.lascito.data.repository.SessionUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val session: SessionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val GRACE_PERIOD_MS = 30_000L
    }


    /** null = cargando, false = desactivado, true = activado */
    val isBiometricEnabled: StateFlow<Boolean?> = prefs.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isUnlocked = MutableStateFlow(
        savedStateHandle.get<Boolean>("isUnlocked") ?: false
    )
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _lastPauseTime = MutableStateFlow(
        savedStateHandle.get<Long>("lastPauseTime") ?: 0L
    )
    val lastPauseTime: StateFlow<Long> = _lastPauseTime.asStateFlow()


    /** Lectura puntual: el bloqueo solo aplica si hay sesión iniciada. */
    val currentUser: SessionUser?
        get() = session.currentUser

    fun signOut() = session.signOut()

    fun unlock() {
        _isUnlocked.value = true
        savedStateHandle["isUnlocked"] = true
    }
    
    fun lock() {
        _isUnlocked.value = false
        savedStateHandle["isUnlocked"] = false
    }
    
    fun onPause() {
        _lastPauseTime.value = SystemClock.elapsedRealtime()
        savedStateHandle["lastPauseTime"] = _lastPauseTime.value
    }
    
    fun onResume() {
        val lastPause = _lastPauseTime.value
        
        if (lastPause == 0L) {
            // Primera composición, no hacer nada
            return
        }
        
        val timeAway = SystemClock.elapsedRealtime() - lastPause
        if (timeAway > GRACE_PERIOD_MS) {
            lock()
        }
    }
}
