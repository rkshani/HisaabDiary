package com.hisaab.diary.ui.feature.reminders

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.ui.components.InitialsAvatar
import com.hisaab.diary.ui.components.balanceColor
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.amount

@Composable
fun RemindersDestination(
    onBack: () -> Unit,
    onOpenParty: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RemindersScreen(
        state = state,
        onBack = onBack,
        onOpenParty = onOpenParty,
        onDone = viewModel::onDone,
        onSnooze = viewModel::onSnooze,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onBack: () -> Unit,
    onOpenParty: (Long) -> Unit,
    onDone: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.isEmpty -> Text(
                text = stringResource(R.string.reminders_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(padding).padding(32.dp),
            )

            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                val actions = RowActions(onOpenParty, onDone, onSnooze)
                section(R.string.reminders_overdue, state.overdue, actions)
                section(R.string.reminders_today, state.today, actions)
                section(R.string.reminders_upcoming, state.upcoming, actions)
            }
        }
    }
}

private class RowActions(val onOpenParty: (Long) -> Unit, val onDone: (Long) -> Unit, val onSnooze: (Long) -> Unit)

private fun LazyListScope.section(@StringRes title: Int, reminders: List<Reminder>, actions: RowActions) {
    if (reminders.isEmpty()) return
    item(key = "title-$title") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
    items(reminders, key = { it.id }) { reminder ->
        ReminderRow(
            reminder = reminder,
            onClick = { actions.onOpenParty(reminder.partyId) },
            onDone = { actions.onDone(reminder.id) },
            onSnooze = { actions.onSnooze(reminder.id) },
        )
        HorizontalDivider(Modifier.padding(start = 72.dp))
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateFormatter() }
    val statusLabel = if (reminder.status ==
        BalanceStatus.WILL_GET
    ) {
        R.string.status_will_get
    } else {
        R.string.status_will_give
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InitialsAvatar(name = reminder.partyName)
            Column(Modifier.weight(1f)) {
                Text(reminder.partyName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.reminder_due_on, formatter.date(reminder.remindAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(reminder.balance),
                    style = MaterialTheme.typography.titleMedium.amount(),
                    color = balanceColor(reminder.balance),
                )
                Text(
                    text = stringResource(statusLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSnooze) { Text(stringResource(R.string.action_snooze)) }
            TextButton(onClick = onDone) { Text(stringResource(R.string.action_mark_done)) }
        }
    }
}
