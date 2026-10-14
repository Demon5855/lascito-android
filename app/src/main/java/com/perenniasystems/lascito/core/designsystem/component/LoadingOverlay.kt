package com.perenniasystems.lascito.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.core.designsystem.theme.AmoledBlack
import com.perenniasystems.lascito.core.designsystem.theme.SurfaceDark
import com.perenniasystems.lascito.core.designsystem.theme.SurfaceVariant
import com.perenniasystems.lascito.core.designsystem.theme.TextPrimary
import com.perenniasystems.lascito.core.designsystem.theme.TextSecondary

/** Variantes visuales del overlay de carga. */
enum class LoadingOverlayStyle {
    /** Tarjeta con mensaje, barra y porcentaje (subida de legados y recuerdos). */
    Card,

    /** Indicador circular sobre fondo oscuro, con porcentaje si se conoce (sellado de cápsulas). */
    Minimal
}

/**
 * Overlay de carga a pantalla completa que bloquea la interacción.
 *
 * @param progress progreso de 0 a 1, o null si no se conoce (solo [LoadingOverlayStyle.Minimal]
 * lo muestra como indicador indeterminado).
 */
@Composable
fun LoadingOverlay(
    message: String,
    progress: Float?,
    style: LoadingOverlayStyle = LoadingOverlayStyle.Card
) {
    when (style) {
        LoadingOverlayStyle.Card -> CardLoadingOverlay(message = message, progress = progress ?: 0f)
        LoadingOverlayStyle.Minimal -> MinimalLoadingOverlay(message = message, progress = progress)
    }
}

@Composable
private fun CardLoadingOverlay(message: String, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) { /* Block clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                    textAlign = TextAlign.Center
                )

                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = AmoledBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MinimalLoadingOverlay(message: String, progress: Float?) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(AmoledBlack.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress          = { progress },
                    modifier          = Modifier.size(64.dp),
                    strokeWidth       = 5.dp,
                    color             = MaterialTheme.colorScheme.primary,
                    trackColor        = SurfaceVariant
                )
                Text(
                    text  = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                CircularProgressIndicator(
                    modifier    = Modifier.size(52.dp),
                    strokeWidth = 4.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        }
    }
}
