package com.hisaab.diary.ui.feature.party

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hisaab.diary.R
import com.hisaab.diary.core.phone.PhoneNumbers
import com.hisaab.diary.domain.model.PartySummary
import com.hisaab.diary.ui.components.formatMoney

/** How the reminder is sent. Only intents: the app itself never uses the internet. */
enum class ReminderChannel { WHATSAPP, SMS, OTHER }

/** The approved reminder text, e.g. "Assalam o Alaikum Ali, ... Rs 2,000 is pending ...". */
fun reminderMessage(context: Context, name: String, amount: String): String =
    context.getString(R.string.reminder_message, name, amount, context.getString(R.string.app_name))

/**
 * Builds the intent for [channel].
 * WhatsApp opens the chat of [phone] directly when it is known; SMS needs a phone.
 */
fun reminderIntent(channel: ReminderChannel, message: String, phone: String?): Intent {
    val international = phone?.let(PhoneNumbers::toInternational)
    return when {
        channel == ReminderChannel.WHATSAPP && international != null ->
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$international?text=${Uri.encode(message)}"))
                .setPackage(WHATSAPP_PACKAGE)

        channel == ReminderChannel.WHATSAPP ->
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
                .setPackage(WHATSAPP_PACKAGE)

        channel == ReminderChannel.SMS && phone != null ->
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${PhoneNumbers.forSms(phone)}"))
                .putExtra("sms_body", message)

        else -> Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message),
            null,
        )
    }
}

/** Starts the intent; falls back to the share sheet if WhatsApp/SMS is missing. @return false if nothing could open. */
fun sendReminder(context: Context, channel: ReminderChannel, message: String, phone: String?): Boolean =
    runCatching { context.startActivity(reminderIntent(channel, message, phone)) }
        .recoverCatching { error ->
            if (error !is ActivityNotFoundException || channel == ReminderChannel.OTHER) throw error
            context.startActivity(reminderIntent(ReminderChannel.OTHER, message, phone))
        }
        .isSuccess

private const val WHATSAPP_PACKAGE = "com.whatsapp"

/** Lets the user pick WhatsApp, SMS (when a phone is saved) or other apps. */
@Composable
fun SendReminderDialog(
    summary: PartySummary,
    onDismiss: () -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val message = reminderMessage(context, summary.party.name, formatMoney(summary.balance))
    val phone = summary.party.phone
    val send: (ReminderChannel) -> Unit = { channel ->
        onDismiss()
        if (!sendReminder(context, channel, message, phone)) onFailure()
    }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_via_title, summary.party.name)) },
        text = { Text(message) },
        confirmButton = {
            Column {
                TextButton(onClick = { send(ReminderChannel.WHATSAPP) }) {
                    Text(stringResource(R.string.reminder_via_whatsapp))
                }
                if (phone != null) {
                    TextButton(onClick = {
                        send(ReminderChannel.SMS)
                    }) { Text(stringResource(R.string.reminder_via_sms)) }
                }
                TextButton(onClick = {
                    send(ReminderChannel.OTHER)
                }) { Text(stringResource(R.string.reminder_via_other)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
