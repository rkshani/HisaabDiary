package com.hisaab.diary.ui.feature.party

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.BuildConfig
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.data.export.ShareFiles
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.feature.party.editor.PartyEditorSheet
import com.hisaab.diary.ui.theme.HisaabTheme
import kotlinx.coroutines.launch

@Composable
fun PartyDetailDestination(
    onBack: () -> Unit,
    onAddEntry: (partyId: Long, isGave: Boolean) -> Unit,
    onOpenEntry: (partyId: Long, entryId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var confirmSettle by rememberSaveable { mutableStateOf(false) }
    var showReminder by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val noAppMessage = stringResource(R.string.reminder_no_app)
    val context = LocalContext.current
    val exportFailedMessage = stringResource(R.string.pdf_failed)
    val shareTitle = stringResource(R.string.pdf_share_title)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PartyDetailEffect.SharePdf -> {
                    val uri = ShareFiles.uriFor(context, effect.file)
                    runCatching { context.startActivity(ShareFiles.shareIntent(uri, "application/pdf", shareTitle)) }
                }

                PartyDetailEffect.ExportFailed -> snackbarHostState.showSnackbar(exportFailedMessage)
            }
        }
    }
    val currentOnBack by rememberUpdatedState(onBack)
    val pendingUndo by viewModel.pendingUndo.collectAsStateWithLifecycle()
    val deletedMessage = stringResource(R.string.entry_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(pendingUndo) {
        val entry = pendingUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undo(entry) else viewModel.dismissUndo(entry)
    }

    // The party was deleted (here or elsewhere): leave the screen.
    LaunchedEffect(state.isLoading, state.summary) {
        if (!state.isLoading && state.summary == null) currentOnBack()
    }

    PartyDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = { showEditor = true },
        onSettleUp = { confirmSettle = true },
        onSendReminder = { showReminder = true },
        onSharePdf = viewModel::exportPdf,
        onAddEntry = { isGave -> onAddEntry(viewModel.partyId, isGave) },
        onOpenEntry = { entryId -> onOpenEntry(viewModel.partyId, entryId) },
        modifier = modifier,
    )

    val summary = state.summary
    if (showReminder && summary != null) {
        SendReminderDialog(
            summary = summary,
            onDismiss = { showReminder = false },
            onFailure = { scope.launch { snackbarHostState.showSnackbar(noAppMessage) } },
        )
    }
    if (confirmSettle && summary != null) {
        val note = stringResource(R.string.settle_up_note)
        SettleUpDialog(
            summary = summary,
            onConfirm = {
                confirmSettle = false
                viewModel.settleUp(note)
            },
            onDismiss = { confirmSettle = false },
        )
    }
    if (showEditor && summary != null) {
        PartyEditorSheet(
            bookId = summary.party.bookId,
            partyId = summary.party.id,
            onDismiss = { showEditor = false },
            onSave = { _, _ -> showEditor = false },
            onDelete = { showEditor = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(
    state: PartyDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSettleUp: () -> Unit,
    onSendReminder: () -> Unit,
    onSharePdf: () -> Unit,
    onAddEntry: (isGave: Boolean) -> Unit,
    onOpenEntry: (entryId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(state.summary?.party?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.summary != null && state.summary.status != BalanceStatus.SETTLED) {
                        TextButton(onClick = onSettleUp) { Text(stringResource(R.string.action_settle_up)) }
                    }
                    if (state.summary != null) {
                        IconButton(onClick = onSharePdf) {
                            Icon(painterResource(R.drawable.ic_share), stringResource(R.string.action_share_pdf))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_edit_person))
                        }
                    }
                },
            )
        },
        bottomBar = { EntryButtons(onAddEntry = onAddEntry) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val summary = state.summary
        if (summary == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val formatter = remember { DateFormatter() }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "balance") { PartyBalanceCard(summary, Modifier.padding(16.dp)) }
            if (BuildConfig.FEATURE_REMINDERS && summary.status == BalanceStatus.WILL_GET) {
                item(key = "reminder") {
                    FilledTonalButton(
                        onClick = onSendReminder,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_send), contentDescription = null)
                        Text(stringResource(R.string.action_send_reminder), Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (state.days.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.ledger_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
                return@LazyColumn
            }
            item(key = "header") { LedgerHeader() }
            state.days.forEach { day ->
                item(key = "day-${day.dayStart}") {
                    Text(
                        text = formatter.date(day.dayStart),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { heading() },
                    )
                }
                items(day.entries, key = { it.entry.id }) { item ->
                    LedgerRow(
                        item = item,
                        onClick = { onOpenEntry(item.entry.id) },
                        modifier = Modifier.animateItem(),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

/** The two main actions, always at the bottom. */
@Composable
private fun EntryButtons(onAddEntry: (isGave: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onAddEntry(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HisaabTheme.colors.willGive,
                    contentColor = HisaabTheme.colors.onWillGive,
                ),
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text(stringResource(R.string.action_you_gave)) }
            Button(
                onClick = { onAddEntry(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HisaabTheme.colors.willGet,
                    contentColor = HisaabTheme.colors.onWillGet,
                ),
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text(stringResource(R.string.action_you_got)) }
        }
    }
}

@Composable
private fun SettleUpDialog(
    summary: PartySummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amount = formatMoney(summary.balance)
    val body = if (summary.status == BalanceStatus.WILL_GET) {
        stringResource(R.string.settle_up_body_got, amount, summary.party.name)
    } else {
        stringResource(R.string.settle_up_body_gave, amount, summary.party.name)
    }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settle_up_title, summary.party.name)) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_settle_up)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
