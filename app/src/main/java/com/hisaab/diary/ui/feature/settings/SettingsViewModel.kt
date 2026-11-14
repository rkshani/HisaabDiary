package com.hisaab.diary.ui.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.data.backup.BackupService
import com.hisaab.diary.data.backup.RestoreResult
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.repository.SettingsRepository
import com.hisaab.diary.domain.usecase.DisableAppLockUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.SetPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsMessage { BACKUP_DONE, BACKUP_FAILED, NOT_A_BACKUP, NEWER_VERSION, RESTORE_FAILED }

/** Steps of a restore, driven by the screen's dialogs. */
sealed interface RestoreStep {
    data object Idle : RestoreStep

    /** Ask for the password (again when [wrong] is true). */
    data class NeedPassword(val source: Uri, val wrong: Boolean) : RestoreStep

    /** Last chance: "this will replace current data". */
    data class Confirm(val source: Uri, val password: String?) : RestoreStep

    data object Working : RestoreStep

    /** Done: the app restarts now. */
    data object Restart : RestoreStep
}

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

    private val _restore = MutableStateFlow<RestoreStep>(RestoreStep.Idle)
    val restore: StateFlow<RestoreStep> = _restore

    /** The user picked a file to restore. */
    fun onRestoreFilePicked(source: Uri) = launch {
        when (backup.needsPassword(source)) {
            null -> _messages.send(SettingsMessage.NOT_A_BACKUP)
            true -> _restore.value = RestoreStep.NeedPassword(source, wrong = false)
            false -> _restore.value = RestoreStep.Confirm(source, null)
        }
    }

    fun onRestorePassword(source: Uri, password: String) {
        _restore.value = RestoreStep.Confirm(source, password)
    }

    fun onRestoreConfirmed(source: Uri, password: String?) = launch {
        _restore.value = RestoreStep.Working
        _restore.value = when (backup.restore(source, password)) {
            RestoreResult.OK -> RestoreStep.Restart
            RestoreResult.WRONG_PASSWORD -> RestoreStep.NeedPassword(source, wrong = true)
            RestoreResult.NOT_A_BACKUP -> RestoreStep.Idle.also { _messages.send(SettingsMessage.NOT_A_BACKUP) }
            RestoreResult.NEWER_VERSION -> RestoreStep.Idle.also { _messages.send(SettingsMessage.NEWER_VERSION) }
            RestoreResult.FAILED -> RestoreStep.Idle.also { _messages.send(SettingsMessage.RESTORE_FAILED) }
        }
    }

    fun onRestoreCancelled() {
        _restore.value = RestoreStep.Idle
    }

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
