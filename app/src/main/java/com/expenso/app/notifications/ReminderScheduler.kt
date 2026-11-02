package com.expenso.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.expenso.app.data.UserPreferences
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Keeps a single pending one-time [DueReminderWorker] for the next chosen reminder time.
 *
 * A periodic WorkRequest has a 24 hour flex window, so it may run at any point during the day.
 * A one-time request with an exact initial delay fires at the user's chosen hour, and the worker
 * enqueues the next day's run when it finishes.
 */
class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext

    /** Called when reminder settings change: replace the pending run with a correctly timed one. */
    fun sync(prefs: UserPreferences) {
        if (!enabled(prefs)) {
            cancel()
            return
        }
        enqueue(prefs, ExistingWorkPolicy.REPLACE)
    }

    /** Called on app start: keep an already scheduled run, otherwise schedule the next one. */
    fun ensure(prefs: UserPreferences) {
        if (!enabled(prefs)) {
            cancel()
            return
        }
        enqueue(prefs, ExistingWorkPolicy.KEEP)
    }

    /** Called by the worker once it has finished a run, to chain tomorrow's run. */
    fun scheduleNext(prefs: UserPreferences) {
        if (!enabled(prefs)) return
        enqueue(prefs, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    fun cancel() {
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
        ReminderNotifications.cancelReminders(appContext)
    }

    fun sendTest(prefs: UserPreferences) {
        ReminderNotifications.showTest(appContext, prefs)
    }

    private fun enqueue(prefs: UserPreferences, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<DueReminderWorker>()
            .setInitialDelay(
                delayUntilNextRun(prefs.reminderHour, prefs.reminderMinute),
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private fun enabled(prefs: UserPreferences): Boolean =
        prefs.notificationsEnabled && prefs.dueReminders

    private fun delayUntilNextRun(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }

    private companion object {
        const val WORK_NAME = "expenso_due_reminders"
        const val TAG = "expenso_reminders"
    }
}
