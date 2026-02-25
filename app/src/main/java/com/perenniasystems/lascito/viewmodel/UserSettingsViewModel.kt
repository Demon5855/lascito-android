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

data class UserSettingsUiState(
    val displayName: String = "Cargando...",
    val email: String = "Cargando...",
    val storageUsedStr: String = "Calculando...",
    val storageMaxStr: String = "10 GB"
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
                        name = "Sin conexión"
                    } catch (e: IOException) {
                        name = "Sin conexión"
                    } catch (e: Exception) {
                        // Keep as null if fetch fails, will fallback below
                    }
                }

                _uiState.value = _uiState.value.copy(
                    displayName = if (name.isNullOrBlank()) "Usuario" else name!!,
                    email = userEmail,
                    storageUsedStr = "Calculando..." // Placeholder for future phase
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    displayName = "No autenticado",
                    email = "Sin correo",
                    storageUsedStr = "0 GB"
                )
            }
        }
    }
}
