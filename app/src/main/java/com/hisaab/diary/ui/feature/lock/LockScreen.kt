package com.hisaab.diary.ui.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.core.security.PinHasher
import com.hisaab.diary.ui.feature.entry.Keypad

/** Full-screen PIN pad shown while the app is locked. */
@Composable
fun LockScreen(
    wrongPin: Boolean,
    showBiometric: Boolean,
    onPinEnter: (String) -> Unit,
    onPinEdit: () -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    val currentOnPinEnter by rememberUpdatedState(onPinEnter)
    LaunchedEffect(pin) {
        if (pin.length == PinHasher.PIN_LENGTH) currentOnPinEnter(pin)
    }
    LaunchedEffect(wrongPin) {
        if (wrongPin) pin = ""
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(if (wrongPin) R.string.lock_wrong_pin else R.string.lock_enter_pin),
                color = if (wrongPin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            PinDots(filled = pin.length)
            Keypad(
                onKey = { key ->
                    if (key.isDigit() && pin.length < PinHasher.PIN_LENGTH) {
                        onPinEdit()
                        pin += key
                    }
                },
                onBackspace = { pin = pin.dropLast(1) },
                onClear = { pin = "" },
                digitsOnly = true,
            )
            if (showBiometric) {
                TextButton(onClick = onBiometricClick) { Text(stringResource(R.string.lock_use_fingerprint)) }
            }
        }
    }
}

@Composable
fun PinDots(filled: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.lock_digits_entered, filled, PinHasher.PIN_LENGTH)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(PinHasher.PIN_LENGTH) { index ->
            val shape = CircleShape
            Box(
                Modifier
                    .size(16.dp)
                    .clip(shape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    .background(
                        if (index <
                            filled
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
            )
        }
    }
}
