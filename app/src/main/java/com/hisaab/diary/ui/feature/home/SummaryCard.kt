package com.hisaab.diary.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.core.money.Money
import com.hisaab.diary.domain.model.BookTotals
import com.hisaab.diary.ui.components.balanceColor
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.HisaabTheme
import com.hisaab.diary.ui.theme.amount

/** Top card on Home: total you will get, total you will give, and the net. */
@Composable
fun SummaryCard(totals: BookTotals, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            SummaryCell(
                label = stringResource(R.string.summary_will_get),
                amount = totals.willGet,
                color = HisaabTheme.colors.willGet,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 12.dp))
            SummaryCell(
                label = stringResource(R.string.summary_will_give),
                amount = totals.willGive,
                color = HisaabTheme.colors.willGive,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(netLabel(totals.net)),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = formatMoney(totals.net),
                style = MaterialTheme.typography.titleMedium.amount(),
                // Net > 0 means others owe you overall (same as "will get").
                color = balanceColor(-totals.net),
            )
        }
    }
}

/** Top card on the "You will get" / "You will give" tabs: that side's total and how many people. */
@Composable
fun TabTotalCard(label: String, amount: Money, color: Color, people: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = pluralStringResource(R.plurals.home_people_count, people, people),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatMoney(amount),
                style = MaterialTheme.typography.headlineSmall.amount(),
                color = color,
            )
        }
    }
}

private fun netLabel(net: Money): Int = when {
    net.isPositive -> R.string.summary_net_get
    net.isNegative -> R.string.summary_net_give
    else -> R.string.summary_net_settled
}

@Composable
private fun SummaryCell(label: String, amount: Money, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.headlineSmall.amount(),
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun SummaryCardPreview() {
    HisaabTheme(dynamicColor = false) {
        SummaryCard(BookTotals(willGet = Money(1_250_000), willGive = Money(300_050)))
    }
}
