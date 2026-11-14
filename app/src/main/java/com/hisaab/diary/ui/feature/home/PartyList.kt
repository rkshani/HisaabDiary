package com.hisaab.diary.ui.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.Party
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.ui.components.InitialsAvatar
import com.hisaab.diary.ui.components.balanceColor
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.HisaabTheme
import com.hisaab.diary.ui.theme.amount

/** One row in the party list: avatar, name, last entry date, balance. */
@Composable
fun PartyRow(summary: PartySummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = remember { DateFormatter() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(name = summary.party.name)
        Column(Modifier.weight(1f)) {
            Text(
                text = summary.party.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary.lastEntryDate?.let { formatter.date(it) }
                    ?: stringResource(R.string.party_no_entries),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(summary.balance),
                style = MaterialTheme.typography.titleMedium.amount(),
                color = balanceColor(summary.balance),
            )
            Text(
                text = stringResource(statusLabel(summary.status)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusLabel(status: BalanceStatus): Int = when (status) {
    BalanceStatus.WILL_GET -> R.string.status_will_get
    BalanceStatus.WILL_GIVE -> R.string.status_will_give
    BalanceStatus.SETTLED -> R.string.status_settled
}

/** Shown when the book has no parties yet. */
@Composable
fun EmptyParties(onAddParty: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.empty_parties_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_parties_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddParty) { Text(stringResource(R.string.add_first_person)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PartyRowPreview() {
    HisaabTheme(dynamicColor = false) {
        Column {
            PartyRow(previewSummary("Ali Raza", gave = 250_000, got = 50_000), onClick = {})
            PartyRow(previewSummary("Bilal Shop", gave = 0, got = 120_000), onClick = {})
            PartyRow(previewSummary("Sana", gave = 0, got = 0), onClick = {})
            EmptyParties(onAddParty = {})
        }
    }
}

private fun previewSummary(name: String, gave: Long, got: Long) = PartySummary(
    party = Party(1, 1, name, null, null, 0, false),
    totalGave = Money(gave),
    totalGot = Money(got),
    lastEntryDate = if (gave + got > 0) 1_763_100_000_000 else null,
    entryCount = 1,
)
