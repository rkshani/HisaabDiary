package com.hisaab.diary.domain.repository

import com.hisaab.diary.core.money.CurrencySymbol
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setSelectedBook(bookId: Long)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setCurrencySymbol(symbol: CurrencySymbol)

    suspend fun setOnboardingDone(done: Boolean)

    /** Saves the PIN hash; null values turn the app lock off. */
    suspend fun setPin(hash: String?, salt: String?)

    suspend fun setBiometricEnabled(enabled: Boolean)

    suspend fun setLockTimeoutSeconds(seconds: Int)

    suspend fun setHideAmounts(hide: Boolean)

    suspend fun setBlockScreenshots(block: Boolean)
}
