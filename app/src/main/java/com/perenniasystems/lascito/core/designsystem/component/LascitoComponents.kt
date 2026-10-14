package com.perenniasystems.lascito.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.perenniasystems.lascito.core.designsystem.theme.*

/**
 * Shared text field styled for the Lascito design system.
 * Accent colors come from [MaterialTheme.colorScheme] so they respond to
 * the accent metal selector automatically.
 */
@Composable
fun LascitoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
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
        keyboardActions      = keyboardActions,
        isError              = isError,
        trailingIcon         = trailingIcon,
        shape                = RoundedCornerShape(8.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = CardBorder,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor  = TextSecondary,
            cursorColor          = MaterialTheme.colorScheme.primary,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            errorBorderColor     = ErrorRed,
            errorLabelColor      = ErrorRed,
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Primary CTA button with accent fill.
 * When [enabled] is false the button turns gray so it's clear the user
 * must meet a prerequisite (e.g. accept terms) before proceeding.
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
            containerColor         = MaterialTheme.colorScheme.primary,
            contentColor           = AmoledBlack,
            disabledContainerColor = SurfaceVariant,   // opaque gray — clearly blocked
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
        border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        colors   = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
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


