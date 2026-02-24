package com.perenniasystems.lascito.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.ui.theme.*

/**
 * Shared text field styled for the Lascito design system.
 * Outlined with a muted-gold accent on focus.
 */
@Composable
fun LascitoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        singleLine           = true,
        visualTransformation = visualTransformation,
        keyboardOptions      = keyboardOptions,
        isError              = isError,
        trailingIcon         = trailingIcon,
        shape                = RoundedCornerShape(8.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MutedGold,
            unfocusedBorderColor = CardBorder,
            focusedLabelColor    = MutedGold,
            unfocusedLabelColor  = TextSecondary,
            cursorColor          = MutedGold,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            errorBorderColor     = ErrorRed,
            errorLabelColor      = ErrorRed,
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Primary CTA button with gold fill and black text.
 */
@Composable
fun LascitoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = RoundedCornerShape(8.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = MutedGold,
            contentColor           = AmoledBlack,
            disabledContainerColor = MutedGoldDim,
            disabledContentColor   = TextDisabled
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Secondary (outline) button.
 */
@Composable
fun LascitoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick  = onClick,
        shape    = RoundedCornerShape(8.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, MutedGold),
        colors   = ButtonDefaults.outlinedButtonColors(
            contentColor = MutedGold
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
