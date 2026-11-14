package com.hisaab.diary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.core.date.Clock
import com.hisaab.diary.core.security.LockPolicy
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.usecase.CompleteOnboardingUseCase
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import com.hisaab.diary.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LockState { LOADING, LOCKED, UNLOCKED }

/** App-wide state for the activity: theme settings and the app lock. */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val verifyPin: VerifyPinUseCase,
    private val clock: Clock,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {
    private val loaded = observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<AppSettings> = loaded.map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Locked from the start (cold start) until the PIN or fingerprint is given. */
    private val locked = MutableStateFlow(true)
    private val _wrongPin = MutableStateFlow(false)
    val wrongPin: StateFlow<Boolean> = _wrongPin

    val lockState: StateFlow<LockState> = combine(loaded, locked) { s, isLocked ->
        when {
            s == null -> LockState.LOADING
            s.isLockEnabled && isLocked -> LockState.LOCKED
            else -> LockState.UNLOCKED
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LockState.LOADING)

    private var backgroundAt: Long? = null

    fun onAppBackground() {
        backgroundAt = clock.nowMillis()
    }

    fun onAppForeground() {
        val current = loaded.value ?: return
        if (LockPolicy.shouldLock(current.isLockEnabled, backgroundAt, clock.nowMillis(), current.lockTimeoutSeconds)) {
            locked.value = true
        }
        backgroundAt = null
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            if (verifyPin(pin)) {
                _wrongPin.value = false
                locked.value = false
            } else {
                _wrongPin.value = true
            }
        }
    }

    fun clearWrongPin() {
        _wrongPin.value = false
    }

    fun finishOnboarding() {
        viewModelScope.launch { completeOnboarding() }
    }

    /** Called after a successful fingerprint/face check. */
    fun unlockWithBiometric() {
        locked.value = false
    }
}
