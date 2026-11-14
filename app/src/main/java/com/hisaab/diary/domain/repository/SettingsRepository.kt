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
}
