package com.hisaab.diary.ui.feature.entry

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.domain.model.EntryType
import com.hisaab.diary.domain.usecase.EntryError
import com.hisaab.diary.domain.usecase.EntryValidator
import com.hisaab.diary.ui.components.BillThumbnail
import com.hisaab.diary.ui.components.BillViewer
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.HisaabTheme
import com.hisaab.diary.ui.theme.amount

@Composable
fun AddEntryDestination(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEntryEffect.Saved -> currentOnBack()
                AddEntryEffect.Deleted -> currentOnBack()
            }
        }
    }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) viewModel.onEvent(AddEntryEvent.AttachmentPicked(uri))
    }
    // Ask for notifications only when the user first sets a due date (Android 13+).
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val context = LocalContext.current
    LaunchedEffect(state.dueDate != null) {
        if (state.dueDate != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onEvent(AddEntryEvent.AttachmentPicked(it)) }
    }
    AddEntryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onTakePhoto = {
            val uri = viewModel.newCameraUri()
            cameraUri = uri
            runCatching { takePhoto.launch(uri) }
        },
        onPickPhoto = {
            runCatching {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    state: AddEntryUiState,
    onEvent: (AddEntryEvent) -> Unit,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColor = if (state.type == EntryType.GAVE) HisaabTheme.colors.willGive else HisaabTheme.colors.willGet
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEdit) R.string.edit_entry_for else R.string.add_entry_for,
                            state.partyName,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.isEdit) {
                        IconButton(onClick = { onEvent(AddEntryEvent.Delete) }) {
                            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.action_delete_entry))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TypeSelector(type = state.type, onTypeChange = { onEvent(AddEntryEvent.TypeChanged(it)) })
                AmountDisplay(state = state, color = typeColor)
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { onEvent(AddEntryEvent.NoteChanged(it.take(EntryValidator.MAX_NOTE_LENGTH))) },
                    label = { Text(stringResource(R.string.field_entry_note)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                DateChips(state = state, onEvent = onEvent)
                AttachmentRow(
                    path = state.attachmentPath,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                    onRemove = { onEvent(AddEntryEvent.RemoveAttachment) },
                )
            }
            Keypad(
                onKey = { onEvent(AddEntryEvent.Key(it)) },
                onBackspace = { onEvent(AddEntryEvent.Backspace) },
                onClear = { onEvent(AddEntryEvent.ClearAmount) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Button(
                onClick = { onEvent(AddEntryEvent.Save) },
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@Composable
private fun TypeSelector(type: EntryType, onTypeChange: (EntryType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = type == EntryType.GAVE,
            onClick = { onTypeChange(EntryType.GAVE) },
            label = { Text(stringResource(R.string.action_you_gave)) },
        )
        FilterChip(
            selected = type == EntryType.GOT,
            onClick = { onTypeChange(EntryType.GOT) },
            label = { Text(stringResource(R.string.action_you_got)) },
        )
    }
}

@Composable
private fun AmountDisplay(state: AddEntryUiState, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = state.expression.ifEmpty { "0" },
            style = MaterialTheme.typography.displaySmall.amount(),
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        val amount = state.amount
        if (state.showResult && amount != null) {
            Text(
                text = stringResource(R.string.amount_result, formatMoney(amount)),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        entryErrorText(state.error)?.let {
            Text(
                text = stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun entryErrorText(error: EntryError?): Int? = when (error) {
    EntryError.AMOUNT_NOT_POSITIVE -> R.string.error_amount_zero
    EntryError.AMOUNT_TOO_BIG -> R.string.error_amount_too_big
    EntryError.DUE_BEFORE_DATE -> R.string.error_due_before_date
    EntryError.NOTE_TOO_LONG -> R.string.error_note_too_long
    EntryError.NOT_FOUND -> R.string.error_entry_not_found
    null -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChips(state: AddEntryUiState, onEvent: (AddEntryEvent) -> Unit, modifier: Modifier = Modifier) {
    val formatter = remember { DateFormatter() }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { picking = PICK_DATE },
            label = { Text(stringResource(R.string.entry_date, formatter.date(state.date))) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
        )
        val due = state.dueDate
        AssistChip(
            onClick = { picking = PICK_DUE },
            label = {
                Text(
                    if (due == null) {
                        stringResource(R.string.add_due_date)
                    } else {
                        stringResource(R.string.due_date, formatter.date(due))
                    },
                )
            },
        )
    }
    if (picking != null) {
        val isDue = picking == PICK_DUE
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.dueDate ?: state.date)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onEvent(if (isDue) AddEntryEvent.DueDatePicked(it) else AddEntryEvent.DatePicked(it))
                    }
                    picking = null
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                if (isDue && state.dueDate != null) {
                    TextButton(onClick = {
                        onEvent(AddEntryEvent.DueDatePicked(null))
                        picking = null
                    }) { Text(stringResource(R.string.action_remove)) }
                } else {
                    TextButton(onClick = { picking = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}

private const val PICK_DATE = "date"
private const val PICK_DUE = "due"

@Composable
private fun AttachmentRow(
    path: String?,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewing by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (path == null) {
            AssistChip(
                onClick = onTakePhoto,
                label = { Text(stringResource(R.string.action_take_photo)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_camera), contentDescription = null) },
            )
            AssistChip(
                onClick = onPickPhoto,
                label = { Text(stringResource(R.string.action_pick_photo)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_image), contentDescription = null) },
            )
        } else {
            BillThumbnail(
                path = path,
                contentDescription = stringResource(R.string.bill_photo),
                modifier = Modifier.clickable { viewing = true },
            )
            TextButton(onClick = onRemove) { Text(stringResource(R.string.action_remove_photo)) }
        }
    }
    if (viewing && path != null) {
        BillViewer(path = path, onDismiss = { viewing = false })
    }
}
