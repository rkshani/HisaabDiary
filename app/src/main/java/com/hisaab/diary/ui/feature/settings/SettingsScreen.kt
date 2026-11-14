package com.hisaab.diary.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateFormatter
import com.hisaab.diary.core.security.PinHasher
import com.hisaab.diary.data.backup.BackupManager
import com.hisaab.diary.domain.model.AppSettings
import com.hisaab.diary.ui.MainActivity
import com.hisaab.diary.ui.feature.entry.Keypad
import com.hisaab.diary.ui.feature.lock.PinDots

@Composable
fun SettingsDestination(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                SettingsMessage.BACKUP_DONE -> resources.getString(R.string.backup_done)
                SettingsMessage.BACKUP_FAILED -> resources.getString(R.string.backup_failed)
                SettingsMessage.NOT_A_BACKUP -> resources.getString(R.string.restore_not_backup)
                SettingsMessage.NEWER_VERSION -> resources.getString(R.string.restore_newer_version)
                SettingsMessage.RESTORE_FAILED -> resources.getString(R.string.restore_failed)
            }
            snackbarHostState.showSnackbar(text)
        }
    }
    val saveBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(viewModel::createBackup) }
    val today = remember { DateFormatter().fileDate(System.currentTimeMillis()) }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::onRestoreFilePicked)
    }
    val restoreStep by viewModel.restore.collectAsStateWithLifecycle()
    RestoreDialogs(
        step = restoreStep,
        onPassword = viewModel::onRestorePassword,
        onConfirm = viewModel::onRestoreConfirmed,
        onCancel = viewModel::onRestoreCancelled,
        onRestart = { restartApp(context) },
    )
    val biometricAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    SettingsScreen(
        settings = settings,
        biometricAvailable = biometricAvailable,
        snackbarHostState = snackbarHostState,
        actions = SettingsActions(
            onEnableLock = viewModel::enableLock,
            onDisableLock = viewModel::disableLock,
            onBiometricChange = viewModel::setBiometric,
            onLockTimeoutChange = viewModel::setLockTimeout,
            onHideAmountsChange = viewModel::setHideAmounts,
            onBlockScreenshotsChange = viewModel::setBlockScreenshots,
            onCreateBackup = { password ->
                viewModel.startBackup(password)
                runCatching { saveBackup.launch(BackupManager.fileName(today)) }
            },
            onRestoreBackup = { runCatching { openBackup.launch(arrayOf("*/*")) } },
            onBackupReminderChange = viewModel::setBackupReminderDays,
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
    val onCreateBackup: (password: String?) -> Unit,
    val onRestoreBackup: () -> Unit,
    val onBackupReminderChange: (Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    biometricAvailable: Boolean,
    snackbarHostState: SnackbarHostState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    var settingPin by rememberSaveable { mutableStateOf(false) }
    var pickTimeout by rememberSaveable { mutableStateOf(false) }
    var askBackupPassword by rememberSaveable { mutableStateOf(false) }
    var pickBackupDays by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            SectionTitle(stringResource(R.string.settings_backup))
            ClickRow(
                title = stringResource(R.string.backup_create),
                subtitle = stringResource(R.string.backup_create_desc),
                onClick = { askBackupPassword = true },
            )
            ClickRow(
                title = stringResource(R.string.restore_backup),
                subtitle = stringResource(R.string.restore_backup_desc),
                onClick = actions.onRestoreBackup,
            )
            ClickRow(
                title = stringResource(R.string.backup_reminder),
                subtitle = backupDaysLabel(settings.backupReminderDays),
                onClick = { pickBackupDays = true },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }
    }

    if (pickBackupDays) {
        ChoiceDialog(
            title = stringResource(R.string.backup_reminder),
            options = SettingsViewModel.BACKUP_REMINDER_DAYS,
            selected = settings.backupReminderDays,
            label = { backupDaysLabel(it) },
            onSelect = {
                pickBackupDays = false
                actions.onBackupReminderChange(it)
            },
            onDismiss = { pickBackupDays = false },
        )
    }
    if (askBackupPassword) {
        BackupPasswordDialog(
            onConfirm = { password ->
                askBackupPassword = false
                actions.onCreateBackup(password)
            },
            onDismiss = { askBackupPassword = false },
        )
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

/** Optional password for the backup file, typed twice. Empty = no password. */
@Composable
private fun BackupPasswordDialog(onConfirm: (String?) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var password by rememberSaveable { mutableStateOf("") }
    var again by rememberSaveable { mutableStateOf("") }
    val mismatch = password.isNotEmpty() && again.isNotEmpty() && password != again
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.backup_password_desc), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = again,
                    onValueChange = { again = it },
                    label = { Text(stringResource(R.string.backup_password_again)) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = mismatch,
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.ifEmpty { null }) }, enabled = password == again) {
                Text(stringResource(R.string.backup_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RestoreDialogs(
    step: RestoreStep,
    onPassword: (Uri, String) -> Unit,
    onConfirm: (Uri, String?) -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
) {
    when (step) {
        RestoreStep.Idle -> Unit

        is RestoreStep.NeedPassword -> {
            var password by rememberSaveable(step) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text(stringResource(R.string.restore_password_title)) },
                text = {
                    Column {
                        if (step.wrong) {
                            Text(
                                stringResource(R.string.restore_wrong_password),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.backup_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onPassword(step.source, password) }, enabled = password.isNotEmpty()) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
            )
        }

        is RestoreStep.Confirm -> AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(step.source, step.password) }) {
                    Text(stringResource(R.string.restore_confirm_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) } },
        )

        RestoreStep.Working -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.restore_working)) },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )

        RestoreStep.Restart -> {
            val currentOnRestart by rememberUpdatedState(onRestart)
            LaunchedEffect(Unit) { currentOnRestart() }
        }
    }
}

/** Starts the app fresh so the restored database is opened. */
private fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}

@Composable
private fun backupDaysLabel(days: Int): String = if (days == 0) {
    stringResource(R.string.backup_reminder_off)
} else {
    pluralStringResource(R.plurals.backup_reminder_every, days, days)
}

/** Simple list of radio choices. */
@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
