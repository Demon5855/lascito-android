package com.perenniasystems.lascito.core.common

import androidx.annotation.StringRes
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.error.DataErrorKind
import com.perenniasystems.lascito.data.error.DataErrors
import com.perenniasystems.lascito.data.model.MediaTooLargeException
import java.io.IOException

/**
 * Maps raw Exceptions into localized @StringRes identifiers.
 */
object ErrorHandler {

    @StringRes
    fun mapExceptionToResId(e: Exception): Int {
        if (e is MediaTooLargeException) {
            return if (e.isVideo) R.string.error_video_too_large else R.string.error_file_too_large
        }
        if (e is IOException) return R.string.error_network

        return when (DataErrors.classify(e)) {
            // Email Enumeration Protection: Group login failures under one generic "Invalid Credentials" message
            DataErrorKind.INVALID_CREDENTIALS,
            DataErrorKind.INVALID_USER,
            DataErrorKind.WEAK_PASSWORD -> R.string.error_auth_invalid
            DataErrorKind.NETWORK -> R.string.error_network
            else -> R.string.error_generic
        }
    }
}
