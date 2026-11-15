package com.hisaab.diary.ui.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.BuildConfig
import com.hisaab.diary.R
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.domain.usecase.PartyFilter
import com.hisaab.diary.ui.feature.books.BookSwitcherSheet
import com.hisaab.diary.ui.feature.party.editor.PartyEditorSheet
import com.hisaab.diary.ui.theme.HisaabTheme

@Composable
fun HomeRoute(
    onOpenParty: (Long) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showBooks by rememberSaveable { mutableStateOf(false) }
    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenParty = onOpenParty,
        onAddParty = { showAddSheet = true },
        onOpenReminders = onOpenReminders,
        onOpenBooks = { showBooks = true },
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    )
    if (showBooks) {
        BookSwitcherSheet(onDismiss = { showBooks = false })
    }
    if (showAddSheet && !state.isLoading) {
        PartyEditorSheet(
            bookId = state.bookId,
            partyId = null,
            onDismiss = { showAddSheet = false },
            onSave = { partyId, _ ->
                showAddSheet = false
                onOpenParty(partyId)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onOpenParty: (Long) -> Unit,
    onAddParty: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeTopBar(
                bookName = state.bookName,
                onOpenDrawer = onOpenDrawer,
                onOpenBooks = onOpenBooks,
                onOpenReminders = onOpenReminders,
            )
        },
        floatingActionButton = {
            if (state.hasAnyParty) {
                ExtendedFloatingActionButton(
                    onClick = onAddParty,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_person)) },
                )
            }
        },
    ) { padding ->
        val totals = state.totals
        if (state.isLoading || totals == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.hasAnyParty) {
                HomeTabRow(selected = state.filter, onSelect = { onEvent(HomeEvent.FilterChanged(it)) })
            }
            // Bottom space so the "Add person" button never covers the last row.
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                item(key = "summary") {
                    TotalsHeader(state, totals, Modifier.padding(16.dp))
                }
                if (state.hasAnyParty) {
                    partyList(state, onEvent, onOpenParty)
                } else {
                    item(key = "empty") { EmptyParties(onAddParty = onAddParty) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    bookName: String,
    onOpenDrawer: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.drawer_open))
            }
        },
        title = {
            TextButton(onClick = onOpenBooks) {
                Text(
                    text = bookName.ifEmpty { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    painterResource(R.drawable.ic_expand),
                    contentDescription = stringResource(R.string.action_switch_book),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (BuildConfig.FEATURE_REMINDERS) {
                IconButton(onClick = onOpenReminders) {
                    Icon(painterResource(R.drawable.ic_notification), stringResource(R.string.reminders_title))
                }
            }
        },
    )
}

/** "All" shows both sides and the net; the other tabs show only their own total. */
@Composable
private fun TotalsHeader(state: HomeUiState, totals: BookTotals, modifier: Modifier = Modifier) {
    when (state.filter) {
        PartyFilter.WILL_GET -> TabTotalCard(
            label = stringResource(R.string.home_total_will_get),
            amount = totals.willGet,
            color = HisaabTheme.colors.willGet,
            people = state.willGetCount,
            modifier = modifier,
        )

        PartyFilter.WILL_GIVE -> TabTotalCard(
            label = stringResource(R.string.home_total_will_give),
            amount = totals.willGive,
            color = HisaabTheme.colors.willGive,
            people = state.willGiveCount,
            modifier = modifier,
        )

        else -> SummaryCard(totals, modifier)
    }
}

private fun LazyListScope.partyList(state: HomeUiState, onEvent: (HomeEvent) -> Unit, onOpenParty: (Long) -> Unit) {
    item(key = "search") {
        PartySearchRow(
            query = state.query,
            onQueryChange = { onEvent(HomeEvent.QueryChanged(it)) },
            sort = state.sort,
            onSortChange = { onEvent(HomeEvent.SortChanged(it)) },
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, bottom = 8.dp),
        )
    }
    if (state.parties.isEmpty()) {
        item(key = "no-match") {
            Text(
                text = stringResource(emptyListMessage(state)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        }
    }
    items(state.parties, key = { it.party.id }) { summary ->
        PartyRow(
            summary = summary,
            onClick = { onOpenParty(summary.party.id) },
            modifier = Modifier.animateItem(),
        )
        HorizontalDivider(Modifier.padding(start = 72.dp))
    }
}

private fun emptyListMessage(state: HomeUiState): Int = when {
    state.query.isNotBlank() -> R.string.no_matching_parties
    state.filter == PartyFilter.WILL_GET -> R.string.home_empty_will_get
    state.filter == PartyFilter.WILL_GIVE -> R.string.home_empty_will_give
    else -> R.string.no_matching_parties
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    HisaabTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(isLoading = false, bookName = "Personal", totals = BookTotals(Money.ZERO, Money.ZERO)),
            onEvent = {},
            onOpenParty = {},
            onAddParty = {},
            onOpenReminders = {},
            onOpenBooks = {},
            onOpenDrawer = {},
        )
    }
}

@Preview
@Composable
private fun HomeScreenWillGetPreview() {
    HisaabTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                bookName = "Personal",
                totals = BookTotals(Money(250_000), Money(80_000)),
                filter = PartyFilter.WILL_GET,
                hasAnyParty = true,
                willGetCount = 0,
            ),
            onEvent = {},
            onOpenParty = {},
            onAddParty = {},
            onOpenReminders = {},
            onOpenBooks = {},
            onOpenDrawer = {},
        )
    }
}
