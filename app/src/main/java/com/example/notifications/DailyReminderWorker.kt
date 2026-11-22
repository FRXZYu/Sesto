package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.BalanceRepository
import com.example.domain.WeeklyChallengeProvider
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val repository = BalanceRepository(applicationContext)
    val prefs = repository.readNotificationPreferences()
    if (!prefs.dailyReminderEnabled || !NotificationHelper.hasNotificationPermission(applicationContext)) {
      return Result.success()
    }

    val perceived = repository.loadCurrentScores() ?: (0..5).associateWith { 5 }
    val dailyPlan = repository.ensureDailyPlanForToday(perceived)
    val completedIds = repository.todayCompletedMissionIds.first()

    val pendingDaily = dailyPlan.count { it.missionId !in completedIds }
    val acceptedWeeklyId = repository.assessmentRepository.getAcceptedWeeklyChallengeId()
    val currentWeekSeed = WeeklyChallengeProvider.currentIsoWeekSeed(repository.dayKey)
    val hasPendingAcceptedWeekly = acceptedWeeklyId != null &&
      acceptedWeeklyId.contains(currentWeekSeed.seedKey) &&
      acceptedWeeklyId !in completedIds

    val pendingCount = pendingDaily + if (hasPendingAcceptedWeekly) 1 else 0
    val message = NotificationHelper.buildDailyReminderMessage(pendingCount) ?: return Result.success()

    NotificationHelper.showNotification(
      context = applicationContext,
      notificationId = NotificationHelper.NOTIFICATION_ID_DAILY,
      channelId = NotificationHelper.CHANNEL_DAILY_REMINDER,
      title = "Equilibrio",
      body = message
    )
    return Result.success()
  }
}
