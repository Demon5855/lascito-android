package com.perenniasystems.lascito.feature.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perenniasystems.lascito.MainActivity
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.core.designsystem.theme.LocalAccentMetal
import com.perenniasystems.lascito.feature.auth.BiometricLockViewModel
import kotlinx.coroutines.launch

/**
 * Bloqueo biométrico sobre el contenido de la app.
 *
 * El estado de desbloqueo vive en [BiometricLockViewModel] (SavedStateHandle +
 * periodo de gracia), así que recrear la Activity por un cambio de idioma o de
 * configuración no vuelve a pedir el desbloqueo.
 */
@Composable
fun BiometricGate(
    activity: FragmentActivity,
    viewModel: BiometricLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val currentUser = viewModel.currentUser
    if (currentUser == null) {
        content()
        return
    }

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    // Si el usuario no activó el bloqueo biométrico, skip directo al contenido.
    // Firebase Auth sigue activa — la biometría es solo un overlay visual.
    if (isBiometricEnabled == false) {
        content()
        return
    }

    // Mientras se carga la preferencia (null), mostrar pantalla negra para evitar flash
    if (isBiometricEnabled == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.perenniasystems.lascito.core.designsystem.theme.AmoledBlack)
        )
        return
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    
    // CRÍTICO: DisposableEffect con key del lifecycleOwner
    // Esto previene múltiples registros del observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    viewModel.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onResume()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val authenticate = {
        val executor = ContextCompat.getMainExecutor(activity)
            
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlock()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Cancelación o error del sensor: la app sigue bloqueada y el
                    // usuario puede reintentar; no hay nada más que hacer aquí.
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setDescription(activity.getString(R.string.biometric_description))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
            
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Sin prompt disponible la app permanece bloqueada.
        }
    }
    
    // CRÍTICO: LaunchedEffect con key de isUnlocked
    // Solo se ejecuta cuando isUnlocked cambia, no en cada recomposición
    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            authenticate()
        }
    }
    
    if (isUnlocked) {
        content()
    } else {
        val guardianName = currentUser.displayName.takeIf { !it.isNullOrBlank() }
            ?: currentUser.email?.substringBefore('@')
            ?: stringResource(R.string.biometric_default_name)
            
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.perenniasystems.lascito.core.designsystem.theme.AmoledBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = LocalAccentMetal.current.dimColor,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Text(
                    text = stringResource(R.string.msg_welcome_back, guardianName),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(
                    onClick = { authenticate() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalAccentMetal.current.color,
                        contentColor = com.perenniasystems.lascito.core.designsystem.theme.AmoledBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_unlock_sanctuary),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val scope = rememberCoroutineScope()
                androidx.compose.material3.TextButton(
                    onClick = { 
                        scope.launch {
                            kotlinx.coroutines.delay(150L)
                            viewModel.signOut()
                            val intent = android.content.Intent(activity, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            activity.startActivity(intent)
                            activity.finish()
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_sign_out_safe),
                        color = com.perenniasystems.lascito.core.designsystem.theme.TextSecondary
                    )
                }
            }
        }
    }
}
