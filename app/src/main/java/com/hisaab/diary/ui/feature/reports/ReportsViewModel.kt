package com.hisaab.diary.ui.feature.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.data.export.CsvExporter
import com.hisaab.diary.domain.model.Entry
import com.hisaab.diary.domain.usecase.ObserveEntriesBetweenUseCase
import com.hisaab.diary.domain.usecase.ObservePartiesUseCase
import com.hisaab.diary.domain.usecase.ObserveSelectedBookUseCase
import com.hisaab.diary.domain.usecase.Report
import com.hisaab.diary.domain.usecase.buildReport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReportRange { TODAY, WEEK, MONTH, CUSTOM }

/** An entry row for the report list. */
data class ReportEntry(val entry: Entry, val partyName: String)

sealed interface ReportsEffect {
    data class CsvSaved(val rows: Int) : ReportsEffect

    data object CsvFailed : ReportsEffect
}

data class ReportsUiState(
    val isLoading: Boolean = true,
    val bookId: Long = 0,
    val bookName: String = "",
    val range: ReportRange = ReportRange.MONTH,
    val from: Long = 0,
    val to: Long = 0,
    val report: Report? = null,
    val entries: List<ReportEntry> = emptyList(),
)

private data class Period(val range: ReportRange, val from: Long, val to: Long)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    observeSelectedBook: ObserveSelectedBookUseCase,
    observeParties: ObservePartiesUseCase,
    private val observeEntriesBetween: ObserveEntriesBetweenUseCase,
    private val dateUtils: DateUtils,
    private val csvExporter: CsvExporter,
) : ViewModel() {
    private val _effects = Channel<ReportsEffect>(Channel.BUFFERED)
    val effects: Flow<ReportsEffect> = _effects.receiveAsFlow()

    private val period = MutableStateFlow(periodFor(ReportRange.MONTH))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportsUiState> = combine(observeSelectedBook(), period) { book, p -> book to p }
        .flatMapLatest { (book, p) ->
            combine(
                observeEntriesBetween(book.id, p.from, p.to),
                observeParties(book.id),
                observeParties(book.id, archived = true),
            ) { list, active, archived ->
                val names = (active + archived).associate { it.party.id to it.party.name }
                ReportsUiState(
                    isLoading = false,
                    bookId = book.id,
                    bookName = book.name,
                    range = p.range,
                    from = p.from,
                    to = p.to,
                    report = buildReport(list, p.from, p.to, dateUtils),
                    entries = list.map { ReportEntry(it, names[it.partyId].orEmpty()) },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReportsUiState())

    /** Saves all entries of the current book as CSV to the file the user picked. */
    fun exportCsv(target: Uri) {
        val bookId = uiState.value.bookId
        viewModelScope.launch {
            val rows = csvExporter.export(bookId, target)
            _effects.send(if (rows != null) ReportsEffect.CsvSaved(rows) else ReportsEffect.CsvFailed)
        }
    }

    fun onRangeSelected(range: ReportRange) {
        if (range != ReportRange.CUSTOM) period.value = periodFor(range)
    }

    /** From the date range picker (UTC midnights). */
    fun onCustomRange(fromPicker: Long, toPicker: Long) {
        val from = dateUtils.fromPickerMillis(fromPicker)
        val to = dateUtils.endOfDay(dateUtils.fromPickerMillis(toPicker))
        period.value = Period(ReportRange.CUSTOM, from, to)
    }

    private fun periodFor(range: ReportRange): Period {
        val now = dateUtils.now()
        return when (range) {
            ReportRange.TODAY -> Period(range, dateUtils.startOfDay(now), dateUtils.endOfDay(now))

            ReportRange.WEEK -> Period(range, dateUtils.startOfWeek(now), dateUtils.endOfWeek(now))

            ReportRange.MONTH, ReportRange.CUSTOM ->
                Period(ReportRange.MONTH, dateUtils.startOfMonth(now), dateUtils.endOfMonth(now))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
