package com.perenniasystems.lascito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.IOException

import androidx.annotation.StringRes
import com.perenniasystems.lascito.R

data class UserSettingsUiState(
    @StringRes val displayNameRes: Int? = R.string.state_loading,
    val displayNameRaw: String? = null,
    
    @StringRes val emailRes: Int? = R.string.state_loading,
    val emailRaw: String? = null,
    
    @StringRes val storageUsedStrRes: Int = R.string.state_calculating,
    @StringRes val storageMaxStrRes: Int = R.string.state_storage_max
)

class UserSettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSettingsUiState())
    val uiState: StateFlow<UserSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                var name: String? = user.displayName
                val userEmail = user.email ?: ""

                // Fetch from Firestore if Auth Profile doesn't have the displayName
                if (name.isNullOrBlank()) {
                    try {
                        withTimeout(15_000L) {
                            val doc = db.collection("users").document(user.uid).get().await()
                            name = doc.getString("displayName")
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        _uiState.value = _uiState.value.copy(displayNameRes = R.string.state_offline, displayNameRaw = null)
                        return@launch
                    } catch (e: IOException) {
                        _uiState.value = _uiState.value.copy(displayNameRes = R.string.state_offline, displayNameRaw = null)
                        return@launch
                    } catch (e: Exception) {
                        // Keep as null if fetch fails, will fallback below
                    }
                }

                _uiState.value = _uiState.value.copy(
                    displayNameRes = if (name.isNullOrBlank()) R.string.state_default_user else null,
                    displayNameRaw = name,
                    emailRes = null,
                    emailRaw = userEmail,
                    storageUsedStrRes = R.string.state_calculating // Placeholder for future phase
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    displayNameRes = R.string.state_unauthenticated,
                    displayNameRaw = null,
                    emailRes = R.string.state_no_email,
                    emailRaw = null,
                    storageUsedStrRes = R.string.state_storage_empty
                )
            }
        }
    }
}
