package com.perenniasystems.lascito.core.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.core.designsystem.theme.TextSecondary
import kotlinx.coroutines.launch

/** Una opción de la hoja: qué mostrar y qué hacer al elegirla. */
data class MediaSourceOption(
    val icon: ImageVector,
    val label: String,
    val onSelected: () -> Unit
)

/**
 * Hoja inferior para elegir el origen del medio (foto, vídeo, nota de voz...).
 * Al elegir una opción, la hoja se oculta, se cierra con [onDismiss] y después
 * se ejecuta la acción de la opción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSourceSheet(
    options: List<MediaSourceOption>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color(0xFF1A1A1A),
        shape            = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            options.forEach { option ->
                MediaSourceItem(icon = option.icon, label = option.label) {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        option.onSelected()
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSourceItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(28.dp)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
        )
    }
}
