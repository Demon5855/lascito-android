package com.perenniasystems.lascito.util

import android.util.Patterns
import com.perenniasystems.lascito.R

/**
 * Centralized utility object for common validation rules across the application.
 * All functions return a String Resource ID (Int) if validation fails, or null if it passes.
 */
object ValidationRules {

    /**
     * Validates an email address against the official Android Patterns regex.
     */
    fun validateEmail(email: String): Int? {
        if (email.isBlank()) return R.string.error_invalid_email
        return if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else R.string.error_invalid_email
    }

    /**
     * Validates a password. Rule: At least 8 characters.
     * (Extended rules like 1 uppercase, 1 lowercase, 1 number, 1 symbol can be added here
     * matching the R.string.error_password_strength description).
     */
    fun validatePassword(password: String): Int? {
        if (password.isBlank() || password.length < 8) return R.string.error_password_strength
        // For strict validation matching the strength message:
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        
        return if (hasUppercase && hasLowercase && hasDigit && hasSymbol) {
            null
        } else {
            R.string.error_password_strength
        }
    }

    /**
     * Validates a guardian access PIN. Rule: Exactly 4 digits.
     */
    fun validatePin(pin: String): Int? {
        if (pin.isBlank() || pin.length != 4 || !pin.all { it.isDigit() }) {
            // Reusing a generic or specific error. Currently the UI handles length block, 
            // but we can return null if valid, or a generic invalid field if we had one.
            // As there isn't a specific R.string.error_invalid_pin, we check its validity 
            // solely by returning an Int? that can be mapped if needed, though for Guardian 
            // it usually enables/disables the save button.
            // We'll return a generic "error" via R.string.error_generic if invalid, though 
            // the ViewModel will mainly use this to check `== null` for button enablement.
            return R.string.error_generic
        }
        return null
    }

    /**
     * Valida que la pista del PIN no sea demasiado obvia.
     * 
     * REGLAS:
     * - No puede ser demasiado corta (min 15 caracteres)
     * - No puede contener el PIN exacto
     * - No puede contener dígitos del PIN en secuencia
     * - No puede contener palabras prohibidas
     */
    fun validatePinHint(pin: String, hint: String, isOptional: Boolean = false): Int? {
        if (isOptional && hint.isEmpty()) return null
        
        val normalizedHint = hint.lowercase().replace(Regex("[^a-z0-9]"), "")
        val normalizedPin = pin.lowercase()
        
        if (hint.isBlank()) {
            return R.string.error_pin_hint_required
        }
        
        if (hint.length < 15) {
            return R.string.error_pin_hint_too_short
        }
        
        if (normalizedHint.contains(normalizedPin)) {
            return R.string.error_pin_hint_contains_pin
        }
        
        if (pin.length >= 2) {
            for (i in 0..pin.length - 2) {
                val sequence = pin.substring(i, i + 2)
                if (normalizedHint.contains(sequence)) {
                    return R.string.error_pin_hint_contains_sequence
                }
            }
        }
        
        val forbiddenWords = listOf(
            "pin", "password", "clave", "codigo", "numero",
            "uno", "dos", "tres", "cuatro", "cinco", 
            "seis", "siete", "ocho", "nueve", "cero",
            "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "zero"
        )
        
        for (word in forbiddenWords) {
            if (normalizedHint.contains(word)) {
                return R.string.error_pin_hint_too_obvious
            }
        }
        
        return null
    }

    /**
     * Validates a name (User or Guardian). Rule: Not empty.
     */
    fun validateName(name: String): Int? {
        return if (name.trim().isBlank()) R.string.error_generic else null
    }
}
