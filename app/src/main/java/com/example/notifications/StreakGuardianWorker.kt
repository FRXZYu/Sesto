package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.BalanceRepository
import com.example.data.toDomain
import com.example.domain.HexagonCalculator
import kotlinx.coroutines.flow.first
import java.util.Calendar

object StreakGuardianRules {
  const val GUARDIAN_MIN_HOUR = 21
  const val MIN_STREAK_DAYS = 2

  fun shouldNotify(
    hourOfDay: Int,
    todayCompletionsCount: Int,
    activeStreakDays: Int,
    isFrozenToday: Boolean
  ): Boolean {
    return hourOfDay >= GUARDIAN_MIN_HOUR &&
      todayCompletionsCount == 0 &&
      !isFrozenToday &&
      activeStreakDays >= MIN_STREAK_DAYS
  }
}

class StreakGuardianWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val repository = BalanceRepository(applicationContext)
    val prefs = repository.readNotificationPreferences()
    if (!prefs.streakGuardianEnabled || !NotificationHelper.hasNotificationPermission(applicationContext)) {
      return Result.success()
    }

    val nowCal = Calendar.getInstance().apply {
      timeInMillis = repository.dayKey.currentMillis()
    }
    val hourOfDay = nowCal.get(Calendar.HOUR_OF_DAY)
    val today = repository.dayKey.today()
    val allCompletions = repository.completionDao.getAllCompletions().first().map { it.toDomain() }
    val todayCount = allCompletions.count { it.date == today }
    val freezes = repository.streakFreezeDao.getAllFreezesOnce()
    val frozenSet = freezes.map { it.date }.toSet()
    val streakDays = HexagonCalculator.calculateStreak(allCompletions, repository.dayKey, frozenSet)

    if (!StreakGuardianRules.shouldNotify(
        hourOfDay = hourOfDay,
        todayCompletionsCount = todayCount,
        activeStreakDays = streakDays,
        isFrozenToday = today in frozenSet
      )
    ) {
      return Result.success()
    }

    NotificationHelper.showNotification(
      context = applicationContext,
      notificationId = NotificationHelper.NOTIFICATION_ID_STREAK_GUARDIAN,
      channelId = NotificationHelper.CHANNEL_STREAK_GUARDIAN,
      title = "Guardián de racha",
      body = NotificationHelper.buildStreakGuardianMessage(streakDays)
    )
    return Result.success()
  }
}
