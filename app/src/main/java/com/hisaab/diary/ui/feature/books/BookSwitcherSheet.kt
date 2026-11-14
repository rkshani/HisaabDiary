package com.hisaab.diary.ui.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.domain.model.Book
import com.hisaab.diary.domain.usecase.BookError
import com.hisaab.diary.domain.usecase.BookValidator

/** Which dialog is open in the book sheet. */
private sealed interface BookDialog {
    data object Add : BookDialog

    data class Rename(val book: Book) : BookDialog

    data class Delete(val book: Book) : BookDialog
}

/** Bottom sheet to switch, add, rename and delete books. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSwitcherSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BooksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // In memory only: a Book is not saveable, so an open dialog closes on rotation.
    var dialog by remember { mutableStateOf<BookDialog?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.books_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            state.books.forEach { book ->
                BookRow(
                    book = book,
                    selected = book.id == state.selectedId,
                    onSelect = {
                        viewModel.onSelect(book.id)
                        onDismiss()
                    },
                    onRename = { dialog = BookDialog.Rename(book) },
                    onDelete = { dialog = BookDialog.Delete(book) },
                )
            }
            TextButton(onClick = { dialog = BookDialog.Add }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text(stringResource(R.string.action_add_book), Modifier.padding(start = 8.dp))
            }
        }
    }

    when (val open = dialog) {
        BookDialog.Add -> BookNameDialog(
            title = stringResource(R.string.add_book_title),
            initial = "",
            error = state.error,
            onConfirm = { name -> viewModel.onAdd(name) { dialog = null } },
            onDismiss = {
                viewModel.clearError()
                dialog = null
            },
        )

        is BookDialog.Rename -> BookNameDialog(
            title = stringResource(R.string.rename_book_title),
            initial = open.book.name,
            error = state.error,
            onConfirm = { name -> viewModel.onRename(open.book.id, name) { dialog = null } },
            onDismiss = {
                viewModel.clearError()
                dialog = null
            },
        )

        is BookDialog.Delete -> AlertDialog(
            onDismissRequest = {
                viewModel.clearError()
                dialog = null
            },
            title = { Text(stringResource(R.string.delete_book_title, open.book.name)) },
            text = {
                Text(
                    stringResource(
                        if (state.error == BookError.LAST_BOOK) R.string.error_last_book else R.string.delete_book_body,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onDelete(open.book.id) { dialog = null } }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearError()
                    dialog = null
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )

        null -> Unit
    }
}

@Composable
private fun BookRow(
    book: Book,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            book.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = onRename) {
            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_rename_book, book.name))
        }
        IconButton(onClick = onDelete) {
            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.action_delete_book, book.name))
        }
    }
}

@Composable
private fun BookNameDialog(
    title: String,
    initial: String,
    error: BookError?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val errorText = when (error) {
        BookError.NAME_EMPTY -> stringResource(R.string.error_book_name_empty)
        BookError.NAME_TOO_LONG -> stringResource(R.string.error_book_name_long)
        else -> null
    }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(BookValidator.MAX_NAME_LENGTH) },
                label = { Text(stringResource(R.string.field_book_name)) },
                isError = errorText != null,
                supportingText = if (errorText != null) {
                    { Text(errorText) }
                } else {
                    null
                },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
