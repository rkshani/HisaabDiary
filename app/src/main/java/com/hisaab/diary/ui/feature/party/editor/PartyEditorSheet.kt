package com.hisaab.diary.ui.feature.party.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.domain.usecase.PartyError
import com.hisaab.diary.domain.usecase.PartyValidator
import kotlinx.coroutines.launch

/**
 * Bottom sheet to add a party ([partyId] = null) or edit one.
 * [onSave] gets the saved party id. [onDelete] is called after a confirmed delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyEditorSheet(
    bookId: Long,
    partyId: Long?,
    onDismiss: () -> Unit,
    onSave: (partyId: Long, isNew: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    viewModel: PartyEditorViewModel = hiltViewModel(key = "party-editor-$partyId"),
) {
    LaunchedEffect(bookId, partyId) { viewModel.start(bookId, partyId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { after ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    val currentOnSave by rememberUpdatedState(onSave)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnDelete by rememberUpdatedState(onDelete)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PartyEditorEffect.Saved -> close { currentOnSave(effect.partyId, effect.isNew) }
                is PartyEditorEffect.ArchivedChanged -> close { currentOnDismiss() }
                PartyEditorEffect.Deleted -> close { currentOnDelete() }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        PartyEditorContent(state = state, onEvent = viewModel::onEvent)
    }
}

@Composable
private fun PartyEditorContent(
    state: PartyEditorUiState,
    onEvent: (PartyEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickerMissing by rememberSaveable { mutableStateOf(false) }
    val pickContact = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        uri?.let { onEvent(PartyEditorEvent.ContactPicked(it)) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (state.isEdit) R.string.edit_party_title else R.string.add_party_title),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(
            onClick = {
                runCatching { pickContact.launch(Unit) }
                    .onFailure { if (it is ActivityNotFoundException) pickerMissing = true }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(painterResource(R.drawable.ic_contacts), contentDescription = null)
            Text(stringResource(R.string.pick_from_contacts), Modifier.padding(start = 8.dp))
        }
        if (pickerMissing) {
            Text(stringResource(R.string.error_no_contacts_app), color = MaterialTheme.colorScheme.error)
        }
        val nameErrorRes = nameError(state.error)
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(PartyEditorEvent.NameChanged(it.take(PartyValidator.MAX_NAME_LENGTH))) },
            label = { Text(stringResource(R.string.field_name_required)) },
            isError = state.error == PartyError.NAME_EMPTY || state.error == PartyError.NAME_TOO_LONG,
            supportingText = if (nameErrorRes != null) {
                { Text(stringResource(nameErrorRes)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = { onEvent(PartyEditorEvent.PhoneChanged(it)) },
            label = { Text(stringResource(R.string.field_phone_optional)) },
            isError = state.error == PartyError.PHONE_INVALID,
            supportingText = if (state.error == PartyError.PHONE_INVALID) {
                { Text(stringResource(R.string.error_phone_invalid)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.note,
            onValueChange = { onEvent(PartyEditorEvent.NoteChanged(it)) },
            label = { Text(stringResource(R.string.field_note_optional)) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onEvent(PartyEditorEvent.Save) },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.action_save)) }

        if (state.isEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onEvent(PartyEditorEvent.ToggleArchive) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.isArchived) R.string.action_unarchive else R.string.action_archive))
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_party_title)) },
            text = { Text(stringResource(R.string.delete_party_body, state.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onEvent(PartyEditorEvent.Delete)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun nameError(error: PartyError?): Int? = when (error) {
    PartyError.NAME_EMPTY -> R.string.error_name_empty
    PartyError.NAME_TOO_LONG -> R.string.error_name_too_long
    else -> null
}

/** Opens the system contact picker for phone numbers. No contacts permission needed. */
private class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}
