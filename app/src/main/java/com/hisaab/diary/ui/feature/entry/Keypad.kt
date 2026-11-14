package com.hisaab.diary.ui.feature.entry

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.core.money.AmountCalculator

/**
 * Number pad with a mini calculator. Long-press ⌫ clears the amount.
 * Layout is fixed left-to-right like every calculator, also in RTL.
 */
@Composable
fun Keypad(onKey: (Char) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf('7', '8', '9', AmountCalculator.DIVIDE),
        listOf('4', '5', '6', AmountCalculator.TIMES),
        listOf('1', '2', '3', AmountCalculator.MINUS),
        listOf('.', '0', BACKSPACE, AmountCalculator.PLUS),
    )
    val backspaceLabel = stringResource(R.string.key_backspace)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        val isOperator = key in AmountCalculator.operators
                        KeypadKey(
                            label = key.toString(),
                            description = if (key == BACKSPACE) backspaceLabel else key.toString(),
                            isOperator = isOperator,
                            onClick = { if (key == BACKSPACE) onBackspace() else onKey(key) },
                            onLongClick = if (key == BACKSPACE) onClear else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    description: String,
    isOperator: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isOperator) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private const val BACKSPACE = '⌫'
