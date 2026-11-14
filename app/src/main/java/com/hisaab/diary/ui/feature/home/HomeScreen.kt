package com.hisaab.diary.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.ui.theme.HisaabTheme

@Composable
fun HomeRoute(onOpenParty: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onOpenParty = onOpenParty, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, onOpenParty: (Long) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(state.bookName.ifEmpty { stringResource(R.string.app_name) }) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val totals = state.totals
            if (totals == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SummaryCard(totals)
            }
            // Temporary: lets us test navigation until the party list exists (step 3.2).
            OutlinedButton(onClick = { onOpenParty(SAMPLE_PARTY_ID) }) {
                Text(stringResource(R.string.open_sample_party))
            }
        }
    }
}

private const val SAMPLE_PARTY_ID = 1L

@Preview
@Composable
private fun HomeScreenPreview() {
    HisaabTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                bookName = "Personal",
                totals = BookTotals(Money(1_250_000), Money(300_000)),
            ),
            onOpenParty = {},
        )
    }
}
