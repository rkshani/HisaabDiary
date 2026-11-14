package com.hisaab.diary.ui.feature.home

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hisaab.diary.R
import com.hisaab.diary.ui.components.PlaceholderScreen

@Composable
fun HomeScreen(onOpenParty: (Long) -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.nav_home), modifier = modifier) {
        // Temporary: lets us test navigation until the party list exists (Phase 3).
        OutlinedButton(onClick = { onOpenParty(SAMPLE_PARTY_ID) }) {
            Text(stringResource(R.string.open_sample_party))
        }
    }
}

private const val SAMPLE_PARTY_ID = 1L
