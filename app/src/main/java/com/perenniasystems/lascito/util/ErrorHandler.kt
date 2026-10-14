package com.perenniasystems.lascito.util

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.MediaTooLargeException
import java.io.IOException

/**
 * Maps raw Exceptions into localized @StringRes identifiers.
 */
object ErrorHandler {

    @StringRes
    fun mapExceptionToResId(e: Exception): Int {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> {
                // Email Enumeration Protection: Group login failures under one generic "Invalid Credentials" message
                R.string.error_auth_invalid
            }
            is MediaTooLargeException -> {
                if (e.isVideo) R.string.error_video_too_large else R.string.error_file_too_large
            }
            is FirebaseNetworkException,
            is IOException -> {
                R.string.error_network
            }
            else -> {
                R.string.error_generic
            }
        }
    }
}
