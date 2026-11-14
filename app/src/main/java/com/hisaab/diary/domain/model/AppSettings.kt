package com.hisaab.diary.domain.model

import com.hisaab.diary.core.money.CurrencySymbol

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User settings saved on the phone (DataStore). */
data class AppSettings(
    /** The book shown on Home. Null until the first book is made. */
    val selectedBookId: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Wallpaper colors on Android 12+. */
    val dynamicColor: Boolean = true,
    val currencySymbol: CurrencySymbol = CurrencySymbol.RS,
    val onboardingDone: Boolean = false,
)
