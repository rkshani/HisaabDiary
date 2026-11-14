package com.hisaab.diary.ui.feature.party

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.model.LedgerEntry
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.ui.components.balanceColor
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.HisaabTheme
import com.hisaab.diary.ui.theme.amount

/** Big balance card at the top of a khata. */
@Composable
fun PartyBalanceCard(summary: PartySummary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(balanceLabel(summary.status)),
                    style = MaterialTheme.typography.titleSmall,
                )
                summary.party.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatMoney(summary.balance),
                style = MaterialTheme.typography.headlineSmall.amount(),
                color = balanceColor(summary.balance),
            )
        }
    }
}

private fun balanceLabel(status: BalanceStatus): Int = when (status) {
    BalanceStatus.WILL_GET -> R.string.balance_will_get
    BalanceStatus.WILL_GIVE -> R.string.balance_will_give
    BalanceStatus.SETTLED -> R.string.balance_settled
}

/** Column titles above the entry list. */
@Composable
fun LedgerHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        HeaderText(stringResource(R.string.ledger_column_entry), TextAlign.Start, Modifier.weight(1f))
        HeaderText(stringResource(R.string.action_you_gave), TextAlign.End, Modifier.widthIn(min = AMOUNT_WIDTH))
        HeaderText(stringResource(R.string.action_you_got), TextAlign.End, Modifier.widthIn(min = AMOUNT_WIDTH))
    }
}

@Composable
private fun HeaderText(text: String, align: TextAlign, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
    )
}

/** One entry: note + running balance on the left, amount in the Gave or Got column. */
@Composable
fun LedgerRow(item: LedgerEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val entry = item.entry
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.note ?: stringResource(
                    if (entry.type == EntryType.GAVE) R.string.action_you_gave else R.string.action_you_got,
                ),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.ledger_balance_after, formatMoney(item.runningBalance)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountCell(text = if (entry.type == EntryType.GAVE) formatMoney(entry.amount) else "", isGave = true)
        AmountCell(text = if (entry.type == EntryType.GOT) formatMoney(entry.amount) else "", isGave = false)
    }
}

@Composable
private fun AmountCell(text: String, isGave: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.widthIn(min = AMOUNT_WIDTH),
        style = MaterialTheme.typography.titleSmall.amount(),
        color = if (isGave) HisaabTheme.colors.willGive else HisaabTheme.colors.willGet,
        textAlign = TextAlign.End,
    )
}

private val AMOUNT_WIDTH = 96.dp
