package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EquilibrioWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    if (appWidgetIds.isEmpty()) return
    updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    updateWidgetsAsync(context, appWidgetManager, intArrayOf(appWidgetId))
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    val action = intent.action
    if (
      action == Intent.ACTION_DATE_CHANGED ||
      action == Intent.ACTION_TIME_CHANGED ||
      action == Intent.ACTION_TIMEZONE_CHANGED ||
      action == ACTION_REFRESH_WIDGET
    ) {
      val manager = AppWidgetManager.getInstance(context)
      val component = ComponentName(context, EquilibrioWidgetProvider::class.java)
      val appWidgetIds = manager.getAppWidgetIds(component)
      if (appWidgetIds.isNotEmpty()) {
        updateWidgetsAsync(context, manager, appWidgetIds)
      }
    }
  }

  private fun updateWidgetsAsync(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val appContext = context.applicationContext
        val repository = BalanceRepository(appContext)
        repository.refreshDayIfNeeded()

        val todayPlan = repository.todayDailyPlan.first()
        val todayCompleted = repository.todayCompletedMissionIds.first()
        val realScoresFloat = repository.realScoresFloat.first()
        val userProfile = repository.userProfileFlow.first()
        val dailyGoalCount = repository.readAppPreferences().dailyGoalCount

        val snapshot = EquilibrioWidgetCalculator.calculateSnapshot(
          todayPlanMissionIds = todayPlan.map { it.missionId },
          todayCompletedMissionIds = todayCompleted,
          realScoresFloat = realScoresFloat,
          activeStreakDays = userProfile.activeStreakDays,
          dailyGoalCount = dailyGoalCount
        )

        for (widgetId in appWidgetIds) {
          val options = appWidgetManager.getAppWidgetOptions(widgetId)
          val layoutMode = EquilibrioWidgetCalculator.resolveLayoutMode(
            minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
          )
          val views = buildRemoteViews(appContext, snapshot, layoutMode)
          appWidgetManager.updateAppWidget(widgetId, views)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_EQUILIBRIO_WIDGET"
    private const val WIDGET_TAP_REQUEST_CODE = 4202

    fun buildRemoteViews(
      context: Context,
      snapshot: EquilibrioWidgetSnapshot,
      layoutMode: WidgetLayoutMode
    ): RemoteViews {
      val layoutRes = when (layoutMode) {
        WidgetLayoutMode.COMPACT_4X1 -> R.layout.widget_equilibrio_4x1
        WidgetLayoutMode.EXPANDED_4X2 -> R.layout.widget_equilibrio_4x2
      }
      val views = RemoteViews(context.packageName, layoutRes)

      val hexSizeDp = if (layoutMode == WidgetLayoutMode.COMPACT_4X1) 48f else 96f
      val hexSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        hexSizeDp,
        context.resources.displayMetrics
      ).toInt().coerceAtLeast(72)

      val hexBitmap = EquilibrioWidgetHexagonRenderer.renderForContext(
        context = context,
        realScores = snapshot.realScoresFloat,
        sizePx = hexSizePx
      )

      views.setImageViewBitmap(R.id.widget_hexagon_image, hexBitmap)
      views.setTextViewText(R.id.widget_missions_text, snapshot.missionsLabel)
      views.setTextViewText(R.id.widget_streak_text, snapshot.streakLabel)

      val tapIntent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_TARGET_TAB, 2)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
          Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        WIDGET_TAP_REQUEST_CODE,
        tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

      return views
    }
  }
}

object EquilibrioWidgetUpdater {
  fun requestUpdate(context: Context) {
    runCatching {
      val appContext = context.applicationContext
      val manager = AppWidgetManager.getInstance(appContext)
      val component = ComponentName(appContext, EquilibrioWidgetProvider::class.java)
      val widgetIds = manager.getAppWidgetIds(component)
      if (widgetIds.isEmpty()) return

      val intent = Intent(appContext, EquilibrioWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
      }
      appContext.sendBroadcast(intent)
    }
  }
}
