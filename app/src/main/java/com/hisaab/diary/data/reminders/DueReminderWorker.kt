package com.hisaab.diary.data.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hisaab.diary.R
import com.hisaab.diary.core.date.DateUtils
import com.hisaab.diary.core.money.MoneyFormatter
import com.hisaab.diary.domain.model.BalanceStatus
import com.hisaab.diary.domain.model.Reminder
import com.hisaab.diary.domain.usecase.GetDueRemindersUseCase
import com.hisaab.diary.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/** Once a day: shows a notification for each party whose reminder is due. Fully local. */
class DueReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun getDueReminders(): GetDueRemindersUseCase

        fun dateUtils(): DateUtils
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val due = deps.getDueReminders().invoke()
        if (due.isNotEmpty() && canNotify(applicationContext)) {
            ReminderNotifications.show(applicationContext, due, deps.dateUtils())
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "due-reminders"
        private const val HOUR_TO_RUN = 9

        /** Schedules the daily check at about 9 AM. Safe to call on every app start. */
        fun schedule(context: Context, dateUtils: DateUtils) {
            val now = dateUtils.now()
            var next = dateUtils.startOfDay(now) + TimeUnit.HOURS.toMillis(HOUR_TO_RUN.toLong())
            if (next <= now) next = dateUtils.plusDays(next, 1)
            val request = PeriodicWorkRequestBuilder<DueReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(next - now, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun canNotify(context: Context): Boolean {
            val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}

/** Builds and posts the due-date notifications. */
object ReminderNotifications {
    private const val CHANNEL_ID = "dues"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_dues),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_dues_desc) }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    /** Text like "Collect Rs 2,000 from Ali — due today". */
    fun text(context: Context, reminder: Reminder, dateUtils: DateUtils): String {
        val amount = MoneyFormatter().format(reminder.balance)
        val days = dateUtils.daysFromToday(reminder.remindAt)
        val action = if (reminder.status == BalanceStatus.WILL_GET) {
            context.getString(R.string.notification_collect, amount, reminder.partyName)
        } else {
            context.getString(R.string.notification_pay, amount, reminder.partyName)
        }
        val whenText = if (days < 0) {
            context.resources.getQuantityString(R.plurals.notification_overdue_days, -days, -days)
        } else {
            context.getString(R.string.notification_due_today)
        }
        return context.getString(R.string.notification_line, action, whenText)
    }

    // Permission is checked by canNotify() just above.
    @SuppressLint("MissingPermission")
    fun show(context: Context, reminders: List<Reminder>, dateUtils: DateUtils) {
        if (!DueReminderWorker.canNotify(context)) return
        createChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val manager = NotificationManagerCompat.from(context)
        reminders.forEach { reminder ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text(context, reminder, dateUtils))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
            manager.notify(reminder.partyId.toInt(), notification)
        }
    }
}
