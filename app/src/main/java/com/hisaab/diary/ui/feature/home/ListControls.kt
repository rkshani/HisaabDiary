package com.hisaab.diary.ui.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.domain.usecase.PartyFilter
import com.hisaab.diary.domain.usecase.PartySort

@Composable
fun PartySearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_parties)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear_search))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/** Filter chips plus a sort menu button. */
@Composable
fun PartyFilterBar(
    filter: PartyFilter,
    sort: PartySort,
    onFilterChange: (PartyFilter) -> Unit,
    onSortChange: (PartySort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PartyFilter.entries.forEach { option ->
                FilterChip(
                    selected = option == filter,
                    onClick = { onFilterChange(option) },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
        SortMenu(sort = sort, onSortChange = onSortChange)
    }
}

@Composable
private fun SortMenu(sort: PartySort, onSortChange: (PartySort) -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_sort), stringResource(R.string.action_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PartySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    onClick = {
                        onSortChange(option)
                        open = false
                    },
                    trailingIcon = {
                        if (option == sort) Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                    },
                )
            }
        }
    }
}

private val PartyFilter.label: Int
    get() = when (this) {
        PartyFilter.ALL -> R.string.filter_all
        PartyFilter.WILL_GET -> R.string.filter_will_get
        PartyFilter.WILL_GIVE -> R.string.filter_will_give
        PartyFilter.SETTLED -> R.string.filter_settled
    }

private val PartySort.label: Int
    get() = when (this) {
        PartySort.RECENT -> R.string.sort_recent
        PartySort.NAME -> R.string.sort_name
        PartySort.HIGHEST_AMOUNT -> R.string.sort_highest
    }
