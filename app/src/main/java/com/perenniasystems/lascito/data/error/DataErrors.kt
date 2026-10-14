package com.perenniasystems.lascito.data.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Tipos de error que la UI necesita distinguir, sin exponer clases de Firebase. */
enum class DataErrorKind {
    INVALID_CREDENTIALS,
    INVALID_USER,
    WEAK_PASSWORD,
    RECENT_LOGIN_REQUIRED,
    TOO_MANY_REQUESTS,
    NETWORK,
    OTHER
}

/** Traduce las excepciones de Firebase a [DataErrorKind]. Es el único punto que las conoce. */
object DataErrors {

    fun classify(t: Throwable): DataErrorKind = when (t) {
        // WeakPassword hereda de InvalidCredentials: va primero.
        is FirebaseAuthWeakPasswordException -> DataErrorKind.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException -> DataErrorKind.INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException -> DataErrorKind.INVALID_USER
        is FirebaseAuthRecentLoginRequiredException -> DataErrorKind.RECENT_LOGIN_REQUIRED
        is FirebaseTooManyRequestsException -> DataErrorKind.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> DataErrorKind.NETWORK
        else -> DataErrorKind.OTHER
    }
}
