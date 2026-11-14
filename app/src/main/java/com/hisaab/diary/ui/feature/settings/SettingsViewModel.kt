package com.hisaab.diary.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.repository.SettingsRepository
import com.hisaab.diary.domain.usecase.DisableAppLockUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.SetPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val setPin: SetPinUseCase,
    private val disableAppLock: DisableAppLockUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    /** Turns the lock on with [pin]; the screen already checked that both PIN entries match. */
    fun enableLock(pin: String) = launch { setPin(pin) }

    fun disableLock() = launch { disableAppLock() }

    fun setBiometric(enabled: Boolean) = launch { settingsRepository.setBiometricEnabled(enabled) }

    fun setLockTimeout(seconds: Int) = launch { settingsRepository.setLockTimeoutSeconds(seconds) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        /** Choices for "Lock after": now, 30 seconds, 1 minute, 5 minutes. */
        val LOCK_TIMEOUTS = listOf(0, 30, 60, 300)
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
