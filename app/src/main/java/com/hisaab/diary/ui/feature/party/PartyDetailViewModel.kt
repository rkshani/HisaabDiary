package com.hisaab.diary.ui.feature.party

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.usecase.ObserveLedgerUseCase
import com.hisaab.diary.domain.usecase.ObservePartyUseCase
import com.hisaab.diary.domain.usecase.RecentlyDeletedEntry
import com.hisaab.diary.domain.usecase.UndoDeleteTransactionUseCase
import com.hisaab.diary.ui.navigation.PartyDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Entries of one day, newest entry first. */
data class LedgerDay(val dayStart: Long, val entries: List<LedgerEntry>)

data class PartyDetailUiState(
    val isLoading: Boolean = true,
    /** Null after loading means the party was deleted. */
    val summary: PartySummary? = null,
    val days: List<LedgerDay> = emptyList(),
)

@HiltViewModel
class PartyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeParty: ObservePartyUseCase,
    observeLedger: ObserveLedgerUseCase,
    private val dateUtils: DateUtils,
    private val recentlyDeleted: RecentlyDeletedEntry,
    private val undoDelete: UndoDeleteTransactionUseCase,
) : ViewModel() {
    val partyId: Long = savedStateHandle.toRoute<PartyDetailRoute>().partyId

    val uiState: StateFlow<PartyDetailUiState> =
        combine(observeParty(partyId), observeLedger(partyId)) { summary, ledger ->
            PartyDetailUiState(
                isLoading = false,
                summary = summary,
                days = groupByDay(ledger, dateUtils),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PartyDetailUiState())

    /** An entry of this party that was just deleted; the screen offers Undo for it. */
    val pendingUndo: StateFlow<Entry?> = recentlyDeleted.entry
        .map { it?.takeIf { entry -> entry.partyId == partyId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun undo(entry: Entry) {
        recentlyDeleted.clear(entry)
        viewModelScope.launch { undoDelete(entry) }
    }

    fun dismissUndo(entry: Entry) = recentlyDeleted.clear(entry)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * Groups a ledger (oldest first) into days, newest day first and newest entry first,
 * so the latest activity is at the top of the screen.
 */
fun groupByDay(ledger: List<LedgerEntry>, dateUtils: DateUtils): List<LedgerDay> = ledger
    .asReversed()
    .groupBy { dateUtils.startOfDay(it.entry.date) }
    .map { (day, entries) -> LedgerDay(day, entries) }
