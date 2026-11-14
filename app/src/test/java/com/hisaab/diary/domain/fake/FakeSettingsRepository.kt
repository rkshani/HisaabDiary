package com.hisaab.diary.domain.fake

import com.hisaab.diary.core.money.CurrencySymbol
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.model.ThemeMode
import com.hisaab.diary.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setSelectedBook(bookId: Long) = settings.update { it.copy(selectedBookId = bookId) }

    override suspend fun setThemeMode(mode: ThemeMode) = settings.update { it.copy(themeMode = mode) }

    override suspend fun setDynamicColor(enabled: Boolean) = settings.update { it.copy(dynamicColor = enabled) }

    override suspend fun setCurrencySymbol(symbol: CurrencySymbol) = settings.update {
        it.copy(currencySymbol = symbol)
    }

    override suspend fun setOnboardingDone(done: Boolean) = settings.update { it.copy(onboardingDone = done) }
}
