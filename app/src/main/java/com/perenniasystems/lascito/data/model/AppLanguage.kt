package com.perenniasystems.lascito.data.model

import java.util.Locale
import com.perenniasystems.lascito.R

enum class AppLanguage(val labelRes: Int, val tag: String) {
    SPANISH(R.string.lang_spanish, "es"),
    ENGLISH(R.string.lang_english, "en"),
    PORTUGUESE(R.string.lang_portuguese, "pt-BR");

    companion object {
        /** Idioma que la app está mostrando según la configuración actual. */
        fun fromLocale(locale: Locale): AppLanguage = when (locale.language) {
            "es" -> SPANISH
            "pt" -> PORTUGUESE
            else -> ENGLISH
        }
    }
}
