package com.example.widget

enum class WidgetLayoutMode {
  COMPACT_4X1,
  EXPANDED_4X2
}

data class EquilibrioWidgetSnapshot(
  val realScoresFloat: Map<Int, Float>,
  val completedTodayOfThree: Int,
  val missionsLabel: String,
  val streakDays: Int,
  val streakLabel: String
)

object EquilibrioWidgetCalculator {
  const val DAILY_GOAL_COUNT = 3
  const val COMPACT_HEIGHT_THRESHOLD_DP = 100

  fun resolveLayoutMode(minWidthDp: Int = 0, minHeightDp: Int = 0): WidgetLayoutMode {
    return if (minHeightDp in 1 until COMPACT_HEIGHT_THRESHOLD_DP) {
      WidgetLayoutMode.COMPACT_4X1
    } else {
      WidgetLayoutMode.EXPANDED_4X2
    }
  }

  fun calculateSnapshot(
    todayPlanMissionIds: List<String>,
    todayCompletedMissionIds: Set<String>,
    realScoresFloat: Map<Int, Float>,
    activeStreakDays: Int,
    dailyGoalCount: Int = DAILY_GOAL_COUNT
  ): EquilibrioWidgetSnapshot {
    val safeDailyGoal = dailyGoalCount.coerceIn(1, 6)
    val rawCompletedCount = if (todayPlanMissionIds.isNotEmpty()) {
      val planCompleted = todayPlanMissionIds.distinct().count { it in todayCompletedMissionIds }
      maxOf(planCompleted, todayCompletedMissionIds.size)
    } else {
      todayCompletedMissionIds.size
    }
    val completedTodayOfTarget = rawCompletedCount.coerceIn(0, safeDailyGoal)
    val streakDays = activeStreakDays.coerceAtLeast(0)

    val normalizedScores = (0..5).associateWith { idx ->
      (realScoresFloat[idx] ?: 0f).coerceIn(0f, 10f)
    }

    return EquilibrioWidgetSnapshot(
      realScoresFloat = normalizedScores,
      completedTodayOfThree = completedTodayOfTarget,
      missionsLabel = "$completedTodayOfTarget de $safeDailyGoal misiones hoy",
      streakDays = streakDays,
      streakLabel = if (streakDays == 1) "1 día" else "$streakDays días"
    )
  }
}
