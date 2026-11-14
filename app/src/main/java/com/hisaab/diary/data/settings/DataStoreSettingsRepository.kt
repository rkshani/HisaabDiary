package com.hisaab.diary.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hisaab.diary.core.money.CurrencySymbol
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.model.ThemeMode
import com.hisaab.diary.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun setSelectedBook(bookId: Long) = edit { it[Keys.SELECTED_BOOK] = bookId }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    override suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    override suspend fun setCurrencySymbol(symbol: CurrencySymbol) = edit { it[Keys.CURRENCY] = symbol.name }

    override suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDING_DONE] = done }

    override suspend fun setPin(hash: String?, salt: String?) = edit {
        if (hash == null || salt == null) {
            it.remove(Keys.PIN_HASH)
            it.remove(Keys.PIN_SALT)
            it.remove(Keys.BIOMETRIC)
        } else {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) = edit { it[Keys.BIOMETRIC] = enabled }

    override suspend fun setLockTimeoutSeconds(seconds: Int) = edit { it[Keys.LOCK_TIMEOUT] = seconds }

    override suspend fun setHideAmounts(hide: Boolean) = edit { it[Keys.HIDE_AMOUNTS] = hide }

    override suspend fun setBlockScreenshots(block: Boolean) = edit { it[Keys.BLOCK_SCREENSHOTS] = block }

    override suspend fun setLastBackupAt(time: Long) = edit { it[Keys.LAST_BACKUP] = time }

    override suspend fun setBackupReminderDays(days: Int) = edit { it[Keys.BACKUP_DAYS] = days }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            selectedBookId = this[Keys.SELECTED_BOOK],
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            currencySymbol = enumOrDefault(this[Keys.CURRENCY], defaults.currencySymbol),
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: defaults.onboardingDone,
            pinHash = this[Keys.PIN_HASH],
            pinSalt = this[Keys.PIN_SALT],
            biometricEnabled = this[Keys.BIOMETRIC] ?: defaults.biometricEnabled,
            lockTimeoutSeconds = this[Keys.LOCK_TIMEOUT] ?: defaults.lockTimeoutSeconds,
            hideAmounts = this[Keys.HIDE_AMOUNTS] ?: defaults.hideAmounts,
            blockScreenshots = this[Keys.BLOCK_SCREENSHOTS] ?: defaults.blockScreenshots,
            lastBackupAt = this[Keys.LAST_BACKUP],
            backupReminderDays = this[Keys.BACKUP_DAYS] ?: defaults.backupReminderDays,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val SELECTED_BOOK = longPreferencesKey("selected_book_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout_seconds")
        val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
        val BACKUP_DAYS = intPreferencesKey("backup_reminder_days")
    }
}
