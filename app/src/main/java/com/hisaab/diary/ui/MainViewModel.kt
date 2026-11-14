package com.hisaab.diary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App-wide state for the activity: theme settings. */
@HiltViewModel
class MainViewModel @Inject constructor(observeSettings: ObserveSettingsUseCase) : ViewModel() {
    val settings: StateFlow<AppSettings> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
