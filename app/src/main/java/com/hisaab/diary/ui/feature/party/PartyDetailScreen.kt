package com.hisaab.diary.ui.feature.party

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.ui.components.PlaceholderScreen
import com.hisaab.diary.ui.theme.HisaabTheme

@Composable
fun PartyDetailScreen(
    partyId: Long,
    onBack: () -> Unit,
    onAddEntry: (isGave: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.party_title, partyId),
        modifier = modifier,
        onBack = onBack,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onAddEntry(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HisaabTheme.colors.willGive,
                    contentColor = HisaabTheme.colors.onWillGive,
                ),
            ) { Text(stringResource(R.string.action_you_gave)) }
            Button(
                onClick = { onAddEntry(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HisaabTheme.colors.willGet,
                    contentColor = HisaabTheme.colors.onWillGet,
                ),
            ) { Text(stringResource(R.string.action_you_got)) }
        }
    }
}
