package com.perenniasystems.lascito.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perenniasystems.lascito.R

@Composable
fun LascitoAlertDialog(
    @StringRes messageRes: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val messageText = context.getString(messageRes)
    val btnText = context.getString(R.string.btn_understood)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A), // Dark gray, premium look
        shape = RoundedCornerShape(16.dp),
        text = {
            Text(
                text = messageText,
                color = Color(0xFFE0E0E0), // Light gray/White for readability
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC9A96E), // Gold Accent
                    contentColor = Color(0xFF0D0D0D)  // Amoled Black text
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = btnText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    )
}
