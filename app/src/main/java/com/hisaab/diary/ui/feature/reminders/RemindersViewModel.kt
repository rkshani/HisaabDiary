package com.hisaab.diary.ui.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.domain.usecase.MarkReminderDoneUseCase
import com.hisaab.diary.domain.usecase.ObserveRemindersUseCase
import com.hisaab.diary.domain.usecase.SnoozeReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersUiState(
    val isLoading: Boolean = true,
    val overdue: List<Reminder> = emptyList(),
    val today: List<Reminder> = emptyList(),
    val upcoming: List<Reminder> = emptyList(),
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && today.isEmpty() && upcoming.isEmpty()
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    observeReminders: ObserveRemindersUseCase,
    private val markDone: MarkReminderDoneUseCase,
    private val snooze: SnoozeReminderUseCase,
    private val dateUtils: DateUtils,
) : ViewModel() {
    val uiState: StateFlow<RemindersUiState> = observeReminders()
        .map { list -> split(list.filterNot { it.balance.isZero }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RemindersUiState())

    fun onDone(id: Long) {
        viewModelScope.launch { markDone(id) }
    }

    fun onSnooze(id: Long) {
        viewModelScope.launch { snooze(id, days = 1) }
    }

    private fun split(reminders: List<Reminder>): RemindersUiState {
        val startOfToday = dateUtils.startOfToday()
        val endOfToday = dateUtils.endOfDay(startOfToday)
        return RemindersUiState(
            isLoading = false,
            overdue = reminders.filter { it.remindAt < startOfToday },
            today = reminders.filter { it.remindAt in startOfToday..endOfToday },
            upcoming = reminders.filter { it.remindAt > endOfToday },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
