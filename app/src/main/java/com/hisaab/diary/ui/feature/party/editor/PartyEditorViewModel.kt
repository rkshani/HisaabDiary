package com.hisaab.diary.ui.feature.party.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.core.result.Outcome
import com.hisaab.diary.data.contacts.ContactReader
import com.hisaab.diary.domain.usecase.AddPartyUseCase
import com.hisaab.diary.domain.usecase.DeletePartyUseCase
import com.hisaab.diary.domain.usecase.GetPartyUseCase
import com.hisaab.diary.domain.usecase.PartyError
import com.hisaab.diary.domain.usecase.SetPartyArchivedUseCase
import com.hisaab.diary.domain.usecase.UpdatePartyUseCase
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

data class PartyEditorUiState(
    val partyId: Long? = null,
    val name: String = "",
    val phone: String = "",
    val note: String = "",
    val isArchived: Boolean = false,
    val error: PartyError? = null,
    val isBusy: Boolean = false,
) {
    val isEdit: Boolean get() = partyId != null
}

sealed interface PartyEditorEvent {
    data class NameChanged(val value: String) : PartyEditorEvent

    data class PhoneChanged(val value: String) : PartyEditorEvent

    data class NoteChanged(val value: String) : PartyEditorEvent

    data class ContactPicked(val uri: Uri) : PartyEditorEvent

    data object Save : PartyEditorEvent

    data object ToggleArchive : PartyEditorEvent

    data object Delete : PartyEditorEvent
}

sealed interface PartyEditorEffect {
    data class Saved(val partyId: Long, val isNew: Boolean) : PartyEditorEffect

    data class ArchivedChanged(val archived: Boolean) : PartyEditorEffect

    data object Deleted : PartyEditorEffect
}

@HiltViewModel
class PartyEditorViewModel @Inject constructor(
    private val addParty: AddPartyUseCase,
    private val updateParty: UpdatePartyUseCase,
    private val getParty: GetPartyUseCase,
    private val setArchived: SetPartyArchivedUseCase,
    private val deleteParty: DeletePartyUseCase,
    private val contactReader: ContactReader,
) : ViewModel() {
    private val _state = MutableStateFlow(PartyEditorUiState())
    val state: StateFlow<PartyEditorUiState> = _state.asStateFlow()

    private val _effects = Channel<PartyEditorEffect>(Channel.BUFFERED)
    val effects: Flow<PartyEditorEffect> = _effects.receiveAsFlow()

    private var bookId: Long = 0
    private var started = false

    /** Call once. [partyId] = null to add a new party to [bookId]. */
    fun start(bookId: Long, partyId: Long?) {
        if (started) return
        started = true
        this.bookId = bookId
        if (partyId == null) return
        viewModelScope.launch {
            val party = getParty(partyId) ?: return@launch
            _state.value = PartyEditorUiState(
                partyId = party.id,
                name = party.name,
                phone = party.phone.orEmpty(),
                note = party.note.orEmpty(),
                isArchived = party.isArchived,
            )
        }
    }

    fun onEvent(event: PartyEditorEvent) {
        when (event) {
            is PartyEditorEvent.NameChanged -> _state.update { it.copy(name = event.value, error = null) }
            is PartyEditorEvent.PhoneChanged -> _state.update { it.copy(phone = event.value, error = null) }
            is PartyEditorEvent.NoteChanged -> _state.update { it.copy(note = event.value) }
            is PartyEditorEvent.ContactPicked -> fillFromContact(event.uri)
            PartyEditorEvent.Save -> save()
            PartyEditorEvent.ToggleArchive -> toggleArchive()
            PartyEditorEvent.Delete -> delete()
        }
    }

    private fun fillFromContact(uri: Uri) = viewModelScope.launch {
        val contact = contactReader.read(uri) ?: return@launch
        _state.update {
            it.copy(
                name = contact.name.ifEmpty { it.name },
                phone = contact.phone ?: it.phone,
                error = null,
            )
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val id = current.partyId
            val result = if (id == null) {
                addParty(bookId, current.name, current.phone, current.note)
            } else {
                when (val updated = updateParty(id, current.name, current.phone, current.note)) {
                    is Outcome.Success -> Outcome.Success(id)
                    is Outcome.Failure -> updated
                }
            }
            when (result) {
                is Outcome.Success -> _effects.send(PartyEditorEffect.Saved(result.value, isNew = id == null))
                is Outcome.Failure -> _state.update { it.copy(error = result.error) }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }

    private fun toggleArchive() {
        val id = _state.value.partyId ?: return
        val archived = !_state.value.isArchived
        viewModelScope.launch {
            setArchived(id, archived)
            _state.update { it.copy(isArchived = archived) }
            _effects.send(PartyEditorEffect.ArchivedChanged(archived))
        }
    }

    /** The UI asks the user to confirm before sending [PartyEditorEvent.Delete]. */
    private fun delete() {
        val id = _state.value.partyId ?: return
        viewModelScope.launch {
            deleteParty(id)
            _effects.send(PartyEditorEffect.Deleted)
        }
    }
}
