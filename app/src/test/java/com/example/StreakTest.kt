package com.example

import com.example.domain.CompletionRecord
import com.example.domain.DayKey
import com.example.domain.HexagonCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StreakTest {

  private fun fixedDayKey(year: Int, month: Int, day: Int, hour: Int = 12): DayKey {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val fixedMillis = cal.timeInMillis
    return DayKey { fixedMillis }
  }

  @Test
  fun testEmptyCompletionsReturnsZero() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    assertEquals(0, HexagonCalculator.calculateStreak(emptyList(), dayKey))
  }

  @Test
  fun testStreakIncludesTodayAndConsecutiveDays() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 0, date = dayKey.today()),
      CompletionRecord(missionId = "m2", dimensionIndex = 1, date = dayKey.today()),
      CompletionRecord(missionId = "m3", dimensionIndex = 2, date = dayKey.yesterday()),
      CompletionRecord(missionId = "m4", dimensionIndex = 3, date = dayKey.daysAgo(2))
    )
    assertEquals(3, HexagonCalculator.calculateStreak(completions, dayKey))
    assertEquals(3, HexagonCalculator.calculateActiveDaysLast14(completions, dayKey))
  }

  @Test
  fun testStreakPreservedWhenTodayHasNoCompletionYetButYesterdayDoes() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 0, date = dayKey.yesterday()),
      CompletionRecord(missionId = "m2", dimensionIndex = 2, date = dayKey.daysAgo(2)),
      CompletionRecord(missionId = "m3", dimensionIndex = 4, date = dayKey.daysAgo(3))
    )
    assertEquals(3, HexagonCalculator.calculateStreak(completions, dayKey))
  }

  @Test
  fun testStreakBreaksAfterSkippingYesterdayAndToday() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 0, date = dayKey.daysAgo(2)),
      CompletionRecord(missionId = "m2", dimensionIndex = 2, date = dayKey.daysAgo(3))
    )
    assertEquals(0, HexagonCalculator.calculateStreak(completions, dayKey))
  }

  @Test
  fun testMidnightRolloverHandledByClock() {
    val beforeMidnight = fixedDayKey(2026, Calendar.NOVEMBER, 12, hour = 23)
    val afterMidnight = fixedDayKey(2026, Calendar.NOVEMBER, 13, hour = 0)

    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 2, date = "2026-11-12"),
      CompletionRecord(missionId = "m2", dimensionIndex = 1, date = "2026-11-11")
    )

    assertEquals(2, HexagonCalculator.calculateStreak(completions, beforeMidnight))
    // Al pasar las 00:00 del día 13, la constancia de ayer (12) y anteayer (11) sigue intacta
    assertEquals(2, HexagonCalculator.calculateStreak(completions, afterMidnight))
  }

  @Test
  fun testStreakIncludesFrozenDatesToBridgeGaps() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 0, date = dayKey.daysAgo(2)),
      CompletionRecord(missionId = "m2", dimensionIndex = 2, date = dayKey.daysAgo(3))
    )
    // Sin congelación, el día de ayer (daysAgo(1)) rompe la racha
    assertEquals(0, HexagonCalculator.calculateStreak(completions, dayKey))

    // Con congelación ayer, la racha de anteayer y día anterior se mantiene viva (3 días)
    val frozenDates = setOf(dayKey.yesterday())
    assertEquals(3, HexagonCalculator.calculateStreak(completions, dayKey, frozenDates))
  }

  @Test
  fun testStreakFrozenTodayPlusYesterdayCountsTwoDays() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 1, date = dayKey.yesterday())
    )
    val frozenDates = setOf(dayKey.today())

    assertEquals(2, HexagonCalculator.calculateStreak(completions, dayKey, frozenDates))
  }

  @Test
  fun testStreakWithMultipleFreezesAndCompletionsCalculatesContinuity() {
    val dayKey = fixedDayKey(2026, Calendar.NOVEMBER, 12)
    val completions = listOf(
      CompletionRecord(missionId = "m1", dimensionIndex = 0, date = dayKey.today()),
      CompletionRecord(missionId = "m2", dimensionIndex = 1, date = dayKey.daysAgo(2)),
      CompletionRecord(missionId = "m3", dimensionIndex = 4, date = dayKey.daysAgo(4))
    )
    val frozenDates = setOf(dayKey.yesterday(), dayKey.daysAgo(3))

    assertEquals(5, HexagonCalculator.calculateStreak(completions, dayKey, frozenDates))
  }
}
