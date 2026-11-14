package com.hisaab.diary.ui.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.ui.theme.HisaabTheme

@Composable
fun HomeRoute(
    onOpenParty: (Long) -> Unit,
    onAddParty: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenParty = onOpenParty,
        onAddParty = onAddParty,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onOpenParty: (Long) -> Unit,
    onAddParty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(state.bookName.ifEmpty { stringResource(R.string.app_name) }) })
        },
    ) { padding ->
        val totals = state.totals
        if (state.isLoading || totals == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "summary") {
                SummaryCard(totals, Modifier.padding(16.dp))
            }
            if (!state.hasAnyParty) {
                item(key = "empty") { EmptyParties(onAddParty = onAddParty) }
                return@LazyColumn
            }
            item(key = "search") {
                PartySearchField(
                    query = state.query,
                    onQueryChange = { onEvent(HomeEvent.QueryChanged(it)) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "filters") {
                PartyFilterBar(
                    filter = state.filter,
                    sort = state.sort,
                    onFilterChange = { onEvent(HomeEvent.FilterChanged(it)) },
                    onSortChange = { onEvent(HomeEvent.SortChanged(it)) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            if (state.parties.isEmpty()) {
                item(key = "no-match") {
                    Text(
                        text = stringResource(R.string.no_matching_parties),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            items(state.parties, key = { it.party.id }) { summary ->
                PartyRow(summary = summary, onClick = { onOpenParty(summary.party.id) })
                HorizontalDivider(Modifier.padding(start = 72.dp))
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    HisaabTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(isLoading = false, bookName = "Personal", totals = BookTotals(Money.ZERO, Money.ZERO)),
            onEvent = {},
            onOpenParty = {},
            onAddParty = {},
        )
    }
}
