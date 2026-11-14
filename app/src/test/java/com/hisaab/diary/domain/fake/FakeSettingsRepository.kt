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

    override suspend fun setPin(hash: String?, salt: String?) = settings.update {
        it.copy(pinHash = hash, pinSalt = salt, biometricEnabled = it.biometricEnabled && hash != null)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) = settings.update { it.copy(biometricEnabled = enabled) }

    override suspend fun setLockTimeoutSeconds(seconds: Int) = settings.update { it.copy(lockTimeoutSeconds = seconds) }

    override suspend fun setHideAmounts(hide: Boolean) = settings.update { it.copy(hideAmounts = hide) }

    override suspend fun setBlockScreenshots(block: Boolean) = settings.update { it.copy(blockScreenshots = block) }

    override suspend fun setLastBackupAt(time: Long) = settings.update { it.copy(lastBackupAt = time) }

    override suspend fun setBackupReminderDays(days: Int) = settings.update { it.copy(backupReminderDays = days) }
}
