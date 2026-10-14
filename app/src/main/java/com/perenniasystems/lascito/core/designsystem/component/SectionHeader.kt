package com.perenniasystems.lascito.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.core.designsystem.theme.PlayfairDisplay
import com.perenniasystems.lascito.core.designsystem.theme.TextPrimary
import com.perenniasystems.lascito.core.designsystem.theme.TextSecondary

/**
 * Encabezado de sección.
 *
 * - Solo [title]: título de sección de una pantalla de ajustes.
 * - Con [icon]: encabezado con medallón e [subtitle] opcional (formularios como la cápsula).
 */
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    subtitle: String? = null
) {
    if (icon == null) {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = PlayfairDisplay,
                color      = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        return
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.padding(bottom = 18.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlayfairDisplay
                )
            )
            if (subtitle != null) {
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}
