package com.perenniasystems.lascito.util

/**
 * Shared input validation helpers used across auth screens and Settings.
 * Centralised here so the rules are never duplicated.
 */
object Validators {

    /** Standard email format — same check used on Login, Register, and Guardian fields. */
    val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Returns true if [password] meets all strength requirements:
     *  - At least 8 characters
     *  - At least one lowercase letter   [a-z]
     *  - At least one uppercase letter   [A-Z]
     *  - At least one digit              [0-9]
     *  - At least one symbol             (any char that is not a letter or digit)
     *
     * Using individual Kotlin checks instead of a single complex regex keeps
     * this easy to read, test, and extend.
     */
    fun isPasswordStrong(password: String): Boolean =
        password.length >= 8
            && password.any { it.isLowerCase() }
            && password.any { it.isUpperCase() }
            && password.any { it.isDigit() }
            && password.any { !it.isLetterOrDigit() }
}

