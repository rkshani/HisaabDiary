package com.hisaab.diary.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.usecase.ObserveBookTotalsUseCase
import com.hisaab.diary.domain.usecase.ObservePartiesUseCase
import com.hisaab.diary.domain.usecase.ObserveSelectedBookUseCase
import com.hisaab.diary.domain.usecase.PartyFilter
import com.hisaab.diary.domain.usecase.PartySort
import com.hisaab.diary.domain.usecase.applyOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val isLoading: Boolean = true,
    val bookId: Long = 0,
    val bookName: String = "",
    val totals: BookTotals? = null,
    val parties: List<PartySummary> = emptyList(),
    val query: String = "",
    val sort: PartySort = PartySort.RECENT,
    val filter: PartyFilter = PartyFilter.ALL,
    /** True when the book has parties at all (so an empty list means "no match", not "no parties"). */
    val hasAnyParty: Boolean = false,
)

sealed interface HomeEvent {
    data class QueryChanged(val query: String) : HomeEvent

    data class SortChanged(val sort: PartySort) : HomeEvent

    data class FilterChanged(val filter: PartyFilter) : HomeEvent
}

private data class ListOptions(
    val query: String = "",
    val sort: PartySort = PartySort.RECENT,
    val filter: PartyFilter = PartyFilter.ALL,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSelectedBook: ObserveSelectedBookUseCase,
    observeBookTotals: ObserveBookTotalsUseCase,
    observeParties: ObservePartiesUseCase,
) : ViewModel() {
    private val options = MutableStateFlow(ListOptions())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(observeSelectedBook(), options) { book, opts -> book to opts }
        .flatMapLatest { (book, opts) ->
            combine(
                observeBookTotals(book.id),
                observeParties(book.id, opts.query),
                observeParties(book.id),
            ) { totals, matches, all ->
                HomeUiState(
                    isLoading = false,
                    bookId = book.id,
                    bookName = book.name,
                    totals = totals,
                    parties = matches.applyOptions(opts.sort, opts.filter),
                    query = opts.query,
                    sort = opts.sort,
                    filter = opts.filter,
                    hasAnyParty = all.isNotEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.QueryChanged -> options.update { it.copy(query = event.query) }
            is HomeEvent.SortChanged -> options.update { it.copy(sort = event.sort) }
            is HomeEvent.FilterChanged -> options.update { it.copy(filter = event.filter) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
