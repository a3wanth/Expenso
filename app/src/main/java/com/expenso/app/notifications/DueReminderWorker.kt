package com.expenso.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Daily job that looks for bills, loan installments and credit card payments due inside the
 * user's lead-time window and posts a local notification for the ones not announced yet.
 */
class DueReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        ReminderNotifications.ensureChannel(context)

        val settings = SettingsRepository(context)
        val prefs = settings.preferences.first()
        if (!prefs.notificationsEnabled || !prefs.dueReminders) return Result.success()

        val today = LocalDate.now()
        val todayKey = today.toString()
        // A time change or retry can run the worker twice in one day; never repeat a reminder.
        if (prefs.lastReminderDay == todayKey) {
            ReminderScheduler(context).scheduleNext(prefs)
            return Result.success()
        }

        val snapshot = runCatching {
            FinanceRepository(AppDatabase.getInstance(context)).snapshot.first()
        }.getOrElse { return Result.retry() }

        val leadDays = prefs.reminderLeadDays.coerceIn(0, 30).toLong()
        val candidates = FinanceMath.upcoming(snapshot, leadDays)
            .filter { it.status != DueStatus.PAID }
        val newItems = candidates.filter { it.key !in prefs.remindedKeys }

        // Keep keys for everything still outstanding in the next month so a pay/unpay or a
        // lead-time change does not re-announce the same cycle.
        val keep = FinanceMath.upcoming(snapshot, 31).map { it.key }.toSet()
        val notifiedKeys = prefs.remindedKeys.intersect(keep) + newItems.map { it.key }

        if (newItems.isNotEmpty()) {
            ReminderNotifications.showDueReminders(context, newItems, prefs.currencyCode)
        }
        settings.markReminderRun(todayKey, notifiedKeys)

        // Chain the next run only after a successful pass, so a retry cannot double-schedule.
        ReminderScheduler(context).scheduleNext(settings.preferences.first())
        return Result.success()
    }
}
