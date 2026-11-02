package com.expenso.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expenso.app.MainActivity
import com.expenso.app.R
import com.expenso.app.data.DueItem
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.Money
import com.expenso.app.data.UserPreferences

/**
 * Local (on-device) due-date reminders. Expenso has no INTERNET permission, so these are
 * plain NotificationManager notifications posted by a WorkManager job - never push messages.
 */
object ReminderNotifications {

    const val CHANNEL_DUE = "due_reminders"
    private const val DUE_NOTIFICATION_ID = 4200
    private const val TEST_NOTIFICATION_ID = 4299

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_DUE) != null) return
        val channel = NotificationChannel(
            CHANNEL_DUE,
            "Due date reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you before bills, EMIs and credit card payments are due."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showDueReminders(context: Context, items: List<DueItem>, currencyCode: String) {
        if (items.isEmpty()) return
        ensureChannel(context)

        val single = items.singleOrNull()
        val title = if (single != null) {
            "${single.title} · ${FinanceMath.dueLabel(single)}"
        } else {
            "${items.size} payments due soon"
        }
        val text = if (single != null) {
            "${Money.format(single.amountCents, currencyCode)} · ${single.subtitle}"
        } else {
            items.take(3).joinToString(" · ") { "${it.title} ${FinanceMath.dueLabel(it)}" }
        }

        val builder = baseBuilder(context, title, text)
        if (items.size > 1) {
            val style = NotificationCompat.InboxStyle()
            items.take(6).forEach { item ->
                style.addLine(
                    "${item.title} — ${FinanceMath.dueLabel(item)} — " +
                        Money.format(item.amountCents, currencyCode)
                )
            }
            if (items.size > 6) style.setSummaryText("+${items.size - 6} more")
            builder.setStyle(style)
        }

        notifySafely(context, DUE_NOTIFICATION_ID, builder.build())
    }

    fun showTest(context: Context, prefs: UserPreferences) {
        ensureChannel(context)
        val lead = when (prefs.reminderLeadDays) {
            0 -> "on the due date"
            1 -> "1 day before"
            else -> "${prefs.reminderLeadDays} days before"
        }
        val builder = baseBuilder(
            context,
            "Reminders are working",
            "Expenso will remind you $lead at ${formatTime(prefs.reminderHour, prefs.reminderMinute)}."
        )
        notifySafely(context, TEST_NOTIFICATION_ID, builder.build())
    }

    fun cancelReminders(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        listOf(DUE_NOTIFICATION_ID, TEST_NOTIFICATION_ID).forEach { id ->
            runCatching { manager.cancel(id) }
        }
    }

    private fun baseBuilder(context: Context, title: String, text: String): NotificationCompat.Builder {
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_DUE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    private fun notifySafely(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(id, notification) }
    }

    private fun formatTime(hour: Int, minute: Int): String =
        java.time.LocalTime.of(hour, minute)
            .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault()))
}
