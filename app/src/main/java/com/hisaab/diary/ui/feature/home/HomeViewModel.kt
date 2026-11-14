package com.hisaab.diary.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.domain.usecase.ObserveBookTotalsUseCase
import com.hisaab.diary.domain.usecase.ObservePartiesUseCase
import com.hisaab.diary.domain.usecase.ObserveSelectedBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val bookId: Long = 0,
    val bookName: String = "",
    val totals: BookTotals? = null,
    val parties: List<PartySummary> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSelectedBook: ObserveSelectedBookUseCase,
    observeBookTotals: ObserveBookTotalsUseCase,
    observeParties: ObservePartiesUseCase,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = observeSelectedBook()
        .flatMapLatest { book ->
            combine(observeBookTotals(book.id), observeParties(book.id)) { totals, parties ->
                HomeUiState(
                    isLoading = false,
                    bookId = book.id,
                    bookName = book.name,
                    totals = totals,
                    parties = parties,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
