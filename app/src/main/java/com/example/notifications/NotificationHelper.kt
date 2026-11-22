package com.example.notifications

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
import com.example.MainActivity
import com.example.R

object NotificationHelper {
  const val CHANNEL_DAILY_REMINDER = "equilibrio_daily_reminder"
  const val CHANNEL_STREAK_GUARDIAN = "equilibrio_streak_guardian"
  const val NOTIFICATION_ID_DAILY = 1001
  const val NOTIFICATION_ID_STREAK_GUARDIAN = 1002
  const val TARGET_TAB_HOY = 2

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val dailyChannel = NotificationChannel(
        CHANNEL_DAILY_REMINDER,
        "Recordatorio diario",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Un aviso amable al día sobre tus misiones de equilibrio pendientes."
      }
      val guardianChannel = NotificationChannel(
        CHANNEL_STREAK_GUARDIAN,
        "Guardián de racha",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Aviso nocturno opcional cuando tu racha está activa y aún no registraste acciones."
      }
      manager.createNotificationChannel(dailyChannel)
      manager.createNotificationChannel(guardianChannel)
    }
  }

  fun buildDailyReminderMessage(pendingCount: Int): String? {
    if (pendingCount <= 0) return null
    val suffix = if (pendingCount == 1) "misión pendiente" else "misiones pendientes"
    return "Tu momento de equilibrio: hoy tienes $pendingCount $suffix"
  }

  fun buildStreakGuardianMessage(streakDays: Int): String {
    return "Tu racha de $streakDays días vive hasta medianoche \uD83D\uDD25"
  }

  fun buildHoyPendingIntent(context: Context, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
      putExtra(MainActivity.EXTRA_TARGET_TAB, TARGET_TAB_HOY)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  @SuppressLint("MissingPermission")
  fun showNotification(
    context: Context,
    notificationId: Int,
    channelId: String,
    title: String,
    body: String
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannels(context)

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification_equilibrio)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(buildHoyPendingIntent(context, notificationId))
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
    return true
  }
}
