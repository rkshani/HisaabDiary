package com.hisaab.diary.ui.feature.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.AmountCalculator
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.AddTransactionUseCase
import com.hisaab.diary.domain.usecase.DeleteTransactionUseCase
import com.hisaab.diary.domain.usecase.EntryError
import com.hisaab.diary.domain.usecase.EntryInput
import com.hisaab.diary.domain.usecase.GetPartyUseCase
import com.hisaab.diary.domain.usecase.GetTransactionUseCase
import com.hisaab.diary.domain.usecase.RecentlyDeletedEntry
import com.hisaab.diary.domain.usecase.UpdateTransactionUseCase
import com.hisaab.diary.ui.navigation.AddEntryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEntryUiState(
    val partyName: String = "",
    val type: EntryType = EntryType.GAVE,
    /** What the user typed on the keypad, e.g. "1200+350". */
    val expression: String = "",
    val note: String = "",
    val date: Long = 0,
    val dueDate: Long? = null,
    val error: EntryError? = null,
    val isSaving: Boolean = false,
    /** True when editing an existing entry. */
    val isEdit: Boolean = false,
    val attachmentPath: String? = null,
) {
    /** Result of [expression], or null while it is empty or incomplete. */
    val amount: Money? get() = AmountCalculator.evaluate(expression)
    val showResult: Boolean get() = AmountCalculator.hasOperator(expression)
}

sealed interface AddEntryEvent {
    data class Key(val key: Char) : AddEntryEvent

    data object Backspace : AddEntryEvent

    data object ClearAmount : AddEntryEvent

    data class TypeChanged(val type: EntryType) : AddEntryEvent

    data class NoteChanged(val note: String) : AddEntryEvent

    /** From the date picker (UTC midnight of the chosen date). */
    data class DatePicked(val pickerMillis: Long) : AddEntryEvent

    /** From the date picker, or null to remove the due date. */
    data class DueDatePicked(val pickerMillis: Long?) : AddEntryEvent

    data object Save : AddEntryEvent

    /** Delete the entry being edited. The ledger then offers Undo. */
    data object Delete : AddEntryEvent
}

sealed interface AddEntryEffect {
    data object Saved : AddEntryEffect

    data object Deleted : AddEntryEffect
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val recentlyDeleted: RecentlyDeletedEntry,
    private val getParty: GetPartyUseCase,
    private val dateUtils: DateUtils,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AddEntryRoute>()

    private val _state = MutableStateFlow(
        AddEntryUiState(
            type = if (route.isGave) EntryType.GAVE else EntryType.GOT,
            date = dateUtils.now(),
        ),
    )
    val state: StateFlow<AddEntryUiState> = _state.asStateFlow()

    private val _effects = Channel<AddEntryEffect>(Channel.BUFFERED)
    val effects: Flow<AddEntryEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            getParty(route.partyId)?.let { party -> _state.update { it.copy(partyName = party.name) } }
        }
        route.entryId?.let(::loadEntry)
    }

    private fun loadEntry(id: Long) = viewModelScope.launch {
        val entry = getTransaction(id) ?: return@launch
        _state.update {
            it.copy(
                isEdit = true,
                type = entry.type,
                expression = MoneyFormatter().formatForInput(entry.amount),
                note = entry.note.orEmpty(),
                date = entry.date,
                dueDate = entry.dueDate,
                attachmentPath = entry.attachmentPath,
            )
        }
    }

    fun onEvent(event: AddEntryEvent) {
        when (event) {
            is AddEntryEvent.Key -> updateExpression { AmountCalculator.append(it, event.key) }

            AddEntryEvent.Backspace -> updateExpression { it.dropLast(1) }

            AddEntryEvent.ClearAmount -> updateExpression { "" }

            is AddEntryEvent.TypeChanged -> _state.update { it.copy(type = event.type) }

            is AddEntryEvent.NoteChanged -> _state.update { it.copy(note = event.note, error = null) }

            is AddEntryEvent.DatePicked -> _state.update {
                it.copy(date = dateUtils.atCurrentTime(dateUtils.fromPickerMillis(event.pickerMillis)), error = null)
            }

            is AddEntryEvent.DueDatePicked -> _state.update {
                // Due at the end of the chosen day.
                val due = event.pickerMillis?.let { picked -> dateUtils.endOfDay(dateUtils.fromPickerMillis(picked)) }
                it.copy(dueDate = due, error = null)
            }

            AddEntryEvent.Save -> save()

            AddEntryEvent.Delete -> delete()
        }
    }

    private fun updateExpression(change: (String) -> String) {
        _state.update { it.copy(expression = change(it.expression), error = null) }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val input = EntryInput(
                partyId = route.partyId,
                type = current.type,
                amount = current.amount ?: Money.ZERO,
                note = current.note,
                date = current.date,
                dueDate = current.dueDate,
                attachmentPath = current.attachmentPath,
            )
            val error = when (val id = route.entryId) {
                null -> (addTransaction(input) as? Outcome.Failure)?.error
                else -> (updateTransaction(id, input) as? Outcome.Failure)?.error
            }
            if (error == null) {
                _effects.send(AddEntryEffect.Saved)
            } else {
                _state.update { it.copy(error = error) }
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun delete() {
        val id = route.entryId ?: return
        viewModelScope.launch {
            deleteTransaction(id)?.let(recentlyDeleted::remember)
            _effects.send(AddEntryEffect.Deleted)
        }
    }
}
