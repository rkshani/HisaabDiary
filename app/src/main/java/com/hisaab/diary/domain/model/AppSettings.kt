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
    /** App lock: PBKDF2 hash + salt of the 4-digit PIN (hex). Null = lock off. */
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometricEnabled: Boolean = false,
    /** Lock again after the app was in the background this long. */
    val lockTimeoutSeconds: Int = DEFAULT_LOCK_TIMEOUT_SECONDS,
    /** Privacy mode: show **** instead of amounts. */
    val hideAmounts: Boolean = false,
    /** Block screenshots and the recent-apps preview (FLAG_SECURE). */
    val blockScreenshots: Boolean = false,
    /** When the last backup was made; null = never (counted from the first reminder check). */
    val lastBackupAt: Long? = null,
    /** Remind to back up after this many days; 0 = never remind. */
    val backupReminderDays: Int = DEFAULT_BACKUP_REMINDER_DAYS,
) {
    val isLockEnabled: Boolean get() = pinHash != null && pinSalt != null
}

const val DEFAULT_LOCK_TIMEOUT_SECONDS = 30
const val DEFAULT_BACKUP_REMINDER_DAYS = 7
