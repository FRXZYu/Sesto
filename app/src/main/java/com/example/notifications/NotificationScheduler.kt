package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.NotificationPreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
  const val WORK_DAILY_REMINDER = "equilibrio_daily_reminder_work"
  const val WORK_STREAK_GUARDIAN = "equilibrio_streak_guardian_work"
  private const val GUARDIAN_TRIGGER_HOUR = 21
  private const val GUARDIAN_TRIGGER_MINUTE = 5

  fun defaultConstraints(): Constraints {
    return Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .build()
  }

  /**
   * Calcula los milisegundos hasta la próxima ocurrencia local de [targetHour]:[targetMinute].
   */
  fun computeInitialDelayMillis(
    targetHour: Int,
    targetMinute: Int,
    nowMillis: Long = System.currentTimeMillis()
  ): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
      set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= nowMillis) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }
    return (cal.timeInMillis - nowMillis).coerceAtLeast(1_000L)
  }

  fun syncSchedules(context: Context, prefs: NotificationPreferences) {
    val workManager = WorkManager.getInstance(context.applicationContext)
    val constraints = defaultConstraints()

    if (prefs.dailyReminderEnabled || prefs.streakGuardianEnabled) {
      NotificationHelper.ensureChannels(context.applicationContext)
    }

    if (prefs.dailyReminderEnabled) {
      val delay = computeInitialDelayMillis(
        targetHour = prefs.dailyReminderHour,
        targetMinute = prefs.dailyReminderMinute
      )
      val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setConstraints(constraints)
        .build()
      workManager.enqueueUniquePeriodicWork(
        WORK_DAILY_REMINDER,
        ExistingPeriodicWorkPolicy.UPDATE,
        dailyRequest
      )
    } else {
      workManager.cancelUniqueWork(WORK_DAILY_REMINDER)
    }

    if (prefs.streakGuardianEnabled) {
      val delay = computeInitialDelayMillis(
        targetHour = GUARDIAN_TRIGGER_HOUR,
        targetMinute = GUARDIAN_TRIGGER_MINUTE
      )
      val guardianRequest = PeriodicWorkRequestBuilder<StreakGuardianWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setConstraints(constraints)
        .build()
      workManager.enqueueUniquePeriodicWork(
        WORK_STREAK_GUARDIAN,
        ExistingPeriodicWorkPolicy.UPDATE,
        guardianRequest
      )
    } else {
      workManager.cancelUniqueWork(WORK_STREAK_GUARDIAN)
    }
  }
}
