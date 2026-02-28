package com.perenniasystems.lascito.viewmodel

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiometricLockViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _isUnlocked = MutableStateFlow(
        savedStateHandle.get<Boolean>("isUnlocked") ?: false
    )
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()
    
    private val _lastPauseTime = MutableStateFlow(
        savedStateHandle.get<Long>("lastPauseTime") ?: 0L
    )
    val lastPauseTime: StateFlow<Long> = _lastPauseTime.asStateFlow()
    
    private val GRACE_PERIOD_MS = 30_000L
    
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
