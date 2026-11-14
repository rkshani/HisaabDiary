package com.hisaab.diary.ui.feature.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hisaab.diary.R
import com.hisaab.diary.ui.components.PlaceholderScreen

@Composable
fun AddEntryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.add_entry_title),
        modifier = modifier,
        onBack = onBack,
    )
}
