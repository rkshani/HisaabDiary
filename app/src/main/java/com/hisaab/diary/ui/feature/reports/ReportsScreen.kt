package com.hisaab.diary.ui.feature.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.Report
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.HisaabTheme
import com.hisaab.diary.ui.theme.amount

@Composable
fun ReportsDestination(modifier: Modifier = Modifier, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(
        state = state,
        onRangeSelect = viewModel::onRangeSelected,
        onCustomRange = viewModel::onCustomRange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onRangeSelect: (ReportRange) -> Unit,
    onCustomRange: (from: Long, to: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickRange by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateFormatter() }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_reports)) }) },
    ) { padding ->
        val report = state.report
        if (state.isLoading || report == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "ranges") {
                RangeChips(
                    selected = state.range,
                    onSelect = { if (it == ReportRange.CUSTOM) pickRange = true else onRangeSelect(it) },
                )
            }
            item(key = "period") {
                Text(
                    text = stringResource(R.string.report_period, formatter.date(state.from), formatter.date(state.to)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "totals") { TotalsCard(report, Modifier.padding(16.dp)) }
            item(key = "chart") { ReportChart(report, Modifier.padding(horizontal = 16.dp)) }
            item(key = "entries-title") {
                Text(
                    text = stringResource(R.string.report_entries, state.entries.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
                )
            }
            if (state.entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.report_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            items(state.entries, key = { it.entry.id }) { row ->
                ReportEntryRow(row, formatter)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (pickRange) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { pickRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val from = pickerState.selectedStartDateMillis
                        val to = pickerState.selectedEndDateMillis ?: from
                        if (from != null && to != null) onCustomRange(from, to)
                        pickRange = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickRange = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) { DateRangePicker(state = pickerState, modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun RangeChips(selected: ReportRange, onSelect: (ReportRange) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReportRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(stringResource(range.label)) },
            )
        }
    }
}

private val ReportRange.label: Int
    get() = when (this) {
        ReportRange.TODAY -> R.string.range_today
        ReportRange.WEEK -> R.string.range_week
        ReportRange.MONTH -> R.string.range_month
        ReportRange.CUSTOM -> R.string.range_custom
    }

@Composable
private fun TotalsCard(report: Report, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Total(
                stringResource(R.string.report_total_gave),
                report.totalGave,
                HisaabTheme.colors.willGive,
                Modifier.weight(1f),
            )
            Total(
                stringResource(R.string.report_total_got),
                report.totalGot,
                HisaabTheme.colors.willGet,
                Modifier.weight(1f),
            )
            Total(
                stringResource(R.string.report_net),
                report.net,
                MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Total(label: String, amount: Money, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = (if (amount.isNegative) "-" else "") + formatMoney(amount),
            style = MaterialTheme.typography.titleMedium.amount(),
            color = color,
        )
    }
}

@Composable
private fun ReportEntryRow(row: ReportEntry, formatter: DateFormatter, modifier: Modifier = Modifier) {
    val entry = row.entry
    val isGave = entry.type == EntryType.GAVE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.partyName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(formatter.date(entry.date), entry.note).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(
                if (isGave) R.string.report_row_gave else R.string.report_row_got,
                formatMoney(entry.amount),
            ),
            style = MaterialTheme.typography.titleSmall.amount(),
            color = if (isGave) HisaabTheme.colors.willGive else HisaabTheme.colors.willGet,
        )
    }
}
