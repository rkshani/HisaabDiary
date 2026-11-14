package com.hisaab.diary.ui.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.data.backup.BackupService
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.repository.SettingsRepository
import com.hisaab.diary.domain.usecase.DisableAppLockUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.SetPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsMessage { BACKUP_DONE, BACKUP_FAILED }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val setPin: SetPinUseCase,
    private val disableAppLock: DisableAppLockUseCase,
    private val settingsRepository: SettingsRepository,
    private val backup: BackupService,
) : ViewModel() {
    private val _messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages: Flow<SettingsMessage> = _messages.receiveAsFlow()

    /** Password chosen before the file picker opened; null = no password. */
    private var pendingPassword: String? = null

    fun startBackup(password: String?) {
        pendingPassword = password?.takeIf { it.isNotEmpty() }
    }

    fun createBackup(target: Uri) = launch {
        val ok = backup.create(target, pendingPassword)
        pendingPassword = null
        _messages.send(if (ok) SettingsMessage.BACKUP_DONE else SettingsMessage.BACKUP_FAILED)
    }

    val settings: StateFlow<AppSettings> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    /** Turns the lock on with [pin]; the screen already checked that both PIN entries match. */
    fun enableLock(pin: String) = launch { setPin(pin) }

    fun disableLock() = launch { disableAppLock() }

    fun setBiometric(enabled: Boolean) = launch { settingsRepository.setBiometricEnabled(enabled) }

    fun setLockTimeout(seconds: Int) = launch { settingsRepository.setLockTimeoutSeconds(seconds) }

    fun setHideAmounts(hide: Boolean) = launch { settingsRepository.setHideAmounts(hide) }

    fun setBlockScreenshots(block: Boolean) = launch { settingsRepository.setBlockScreenshots(block) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        /** Choices for "Lock after": now, 30 seconds, 1 minute, 5 minutes. */
        val LOCK_TIMEOUTS = listOf(0, 30, 60, 300)
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
