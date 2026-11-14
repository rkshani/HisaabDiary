package com.hisaab.diary.ui.feature.settings

import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.security.PinHasher
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.ui.feature.entry.Keypad
import com.hisaab.diary.ui.feature.lock.PinDots

@Composable
fun SettingsDestination(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    SettingsScreen(
        settings = settings,
        biometricAvailable = biometricAvailable,
        actions = SettingsActions(
            onEnableLock = viewModel::enableLock,
            onDisableLock = viewModel::disableLock,
            onBiometricChange = viewModel::setBiometric,
            onLockTimeoutChange = viewModel::setLockTimeout,
            onHideAmountsChange = viewModel::setHideAmounts,
            onBlockScreenshotsChange = viewModel::setBlockScreenshots,
        ),
        modifier = modifier,
    )
}

/** All callbacks of the settings screen, grouped to keep the parameter list short. */
class SettingsActions(
    val onEnableLock: (pin: String) -> Unit,
    val onDisableLock: () -> Unit,
    val onBiometricChange: (Boolean) -> Unit,
    val onLockTimeoutChange: (Int) -> Unit,
    val onHideAmountsChange: (Boolean) -> Unit,
    val onBlockScreenshotsChange: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    biometricAvailable: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    var settingPin by rememberSaveable { mutableStateOf(false) }
    var pickTimeout by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.settings_security))
            SwitchRow(
                title = stringResource(R.string.settings_app_lock),
                subtitle = stringResource(R.string.settings_app_lock_desc),
                checked = settings.isLockEnabled,
                onCheckedChange = { on -> if (on) settingPin = true else actions.onDisableLock() },
            )
            if (settings.isLockEnabled) {
                if (biometricAvailable) {
                    SwitchRow(
                        title = stringResource(R.string.settings_fingerprint),
                        subtitle = stringResource(R.string.settings_fingerprint_desc),
                        checked = settings.biometricEnabled,
                        onCheckedChange = actions.onBiometricChange,
                    )
                }
                ClickRow(
                    title = stringResource(R.string.settings_lock_after),
                    subtitle = timeoutLabel(settings.lockTimeoutSeconds),
                    onClick = { pickTimeout = true },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_privacy))
            SwitchRow(
                title = stringResource(R.string.settings_hide_amounts),
                subtitle = stringResource(R.string.settings_hide_amounts_desc),
                checked = settings.hideAmounts,
                onCheckedChange = actions.onHideAmountsChange,
            )
            SwitchRow(
                title = stringResource(R.string.settings_block_screenshots),
                subtitle = stringResource(R.string.settings_block_screenshots_desc),
                checked = settings.blockScreenshots,
                onCheckedChange = actions.onBlockScreenshotsChange,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }
    }

    if (settingPin) {
        SetPinDialog(
            onDone = { pin ->
                settingPin = false
                actions.onEnableLock(pin)
            },
            onDismiss = { settingPin = false },
        )
    }
    if (pickTimeout) {
        TimeoutDialog(
            selected = settings.lockTimeoutSeconds,
            onSelect = {
                pickTimeout = false
                actions.onLockTimeoutChange(it)
            },
            onDismiss = { pickTimeout = false },
        )
    }
}

@Composable
private fun timeoutLabel(seconds: Int): String = when (seconds) {
    0 -> stringResource(R.string.lock_after_now)
    in 1 until SECONDS_PER_MINUTE -> stringResource(R.string.lock_after_seconds, seconds)
    else -> stringResource(R.string.lock_after_minutes, seconds / SECONDS_PER_MINUTE)
}

private const val SECONDS_PER_MINUTE = 60

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun ClickRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Asks for a new PIN twice; calls [onDone] only when both match. */
@Composable
private fun SetPinDialog(onDone: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var first by rememberSaveable { mutableStateOf<String?>(null) }
    var pin by rememberSaveable { mutableStateOf("") }
    var mismatch by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (first == null) R.string.pin_new else R.string.pin_confirm)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (mismatch) Text(stringResource(R.string.pin_mismatch), color = MaterialTheme.colorScheme.error)
                PinDots(filled = pin.length, modifier = Modifier.padding(vertical = 12.dp))
                Keypad(
                    onKey = { key ->
                        if (pin.length >= PinHasher.PIN_LENGTH) return@Keypad
                        pin += key
                        if (pin.length < PinHasher.PIN_LENGTH) return@Keypad
                        val firstPin = first
                        when {
                            firstPin == null -> {
                                first = pin
                                pin = ""
                                mismatch = false
                            }

                            firstPin == pin -> onDone(pin)

                            else -> {
                                first = null
                                pin = ""
                                mismatch = true
                            }
                        }
                    },
                    onBackspace = { pin = pin.dropLast(1) },
                    onClear = { pin = "" },
                    digitsOnly = true,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun TimeoutDialog(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_lock_after)) },
        text = {
            Column {
                SettingsViewModel.LOCK_TIMEOUTS.forEach { seconds ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(seconds) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = seconds == selected, onClick = null)
                        Text(timeoutLabel(seconds), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
