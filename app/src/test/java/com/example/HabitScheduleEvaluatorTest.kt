package com.example

import com.example.data.db.MissionEntity
import com.example.domain.DayKey
import com.example.domain.HabitScheduleEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HabitScheduleEvaluatorTest {

  private fun fixedClock(year: Int, month: Int, day: Int): Pair<DayKey, Long> {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val millis = cal.timeInMillis
    return DayKey { millis } to millis
  }

  @Test
  fun testHabitActiveOnScheduledDaysAndShowsNextDayWhenOff() {
    // 2026-11-09 = Lunes, 2026-11-10 = Martes, 2026-11-11 = Miércoles
    val (mondayClock, createdAt) = fixedClock(2026, Calendar.NOVEMBER, 9)
    val (tuesdayClock, _) = fixedClock(2026, Calendar.NOVEMBER, 10)
    val (wednesdayClock, _) = fixedClock(2026, Calendar.NOVEMBER, 11)
    val (thursdayClock, _) = fixedClock(2026, Calendar.NOVEMBER, 12)
    val (saturdayClock, _) = fixedClock(2026, Calendar.NOVEMBER, 14)

    val days = listOf("Lun", "Mié", "Vie")

    val onMonday = HabitScheduleEvaluator.evaluate(true, days, "Nunca", createdAt, mondayClock)
    assertTrue(onMonday.isScheduledToday)
    assertFalse(onMonday.isExpired)
    assertNull(onMonday.nextAvailableDayLabel)

    val onTuesday = HabitScheduleEvaluator.evaluate(true, days, "Nunca", createdAt, tuesdayClock)
    assertFalse(onTuesday.isScheduledToday)
    assertFalse(onTuesday.isExpired)
    assertEquals("Mié", onTuesday.nextAvailableDayLabel)

    val onWednesday = HabitScheduleEvaluator.evaluate(true, days, "Nunca", createdAt, wednesdayClock)
    assertTrue(onWednesday.isScheduledToday)

    val onThursday = HabitScheduleEvaluator.evaluate(true, days, "Nunca", createdAt, thursdayClock)
    assertFalse(onThursday.isScheduledToday)
    assertEquals("Vie", onThursday.nextAvailableDayLabel)

    // Sábado envuelve hacia la siguiente semana -> Lun
    val onSaturday = HabitScheduleEvaluator.evaluate(true, days, "Nunca", createdAt, saturdayClock)
    assertFalse(onSaturday.isScheduledToday)
    assertEquals("Lun", onSaturday.nextAvailableDayLabel)
  }

  @Test
  fun testParseAndSerializeHabitDaysSupportsShortAndLegacyCodes() {
    val parsed = HabitScheduleEvaluator.parseHabitDays("L, x, Vie, dom")
    assertEquals(listOf("Lun", "Mié", "Vie", "Dom"), parsed)

    val serialized = HabitScheduleEvaluator.serializeHabitDays(listOf("Vie", "Lun", "Mié", "Lun"))
    assertEquals("Lun,Mié,Vie", serialized)
  }

  @Test
  fun testHabitEndRuleIn30DaysExpiresOnDay30() {
    val (day0Clock, createdAt) = fixedClock(2026, Calendar.NOVEMBER, 1)
    val (day29Clock, _) = fixedClock(2026, Calendar.NOVEMBER, 30)
    val (day30Clock, _) = fixedClock(2026, Calendar.DECEMBER, 1)

    val statusDay0 = HabitScheduleEvaluator.evaluate(
      isHabit = true,
      habitDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"),
      habitEndRule = "En 30 días",
      createdAtMillis = createdAt,
      dayKey = day0Clock
    )
    assertFalse(statusDay0.isExpired)
    assertTrue(statusDay0.isScheduledToday)

    val statusDay29 = HabitScheduleEvaluator.evaluate(
      isHabit = true,
      habitDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"),
      habitEndRule = "En 30 días",
      createdAtMillis = createdAt,
      dayKey = day29Clock
    )
    assertFalse(statusDay29.isExpired)
    assertTrue(statusDay29.isScheduledToday)

    val statusDay30 = HabitScheduleEvaluator.evaluate(
      isHabit = true,
      habitDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"),
      habitEndRule = "En 30 días",
      createdAtMillis = createdAt,
      dayKey = day30Clock
    )
    assertTrue(statusDay30.isExpired)
    assertFalse(statusDay30.isScheduledToday)
  }

  @Test
  fun testHabitEndRuleOnDay30OfMonth() {
    val (day10Clock, createdAt) = fixedClock(2026, Calendar.NOVEMBER, 10)
    val (day29Clock, _) = fixedClock(2026, Calendar.NOVEMBER, 29)
    val (day30Clock, _) = fixedClock(2026, Calendar.NOVEMBER, 30)

    val allDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    assertFalse(
      HabitScheduleEvaluator.evaluate(true, allDays, "El día 30", createdAt, day10Clock).isExpired
    )
    assertFalse(
      HabitScheduleEvaluator.evaluate(true, allDays, "El día 30", createdAt, day29Clock).isExpired
    )
    assertTrue(
      HabitScheduleEvaluator.evaluate(true, allDays, "El día 30", createdAt, day30Clock).isExpired
    )
  }

  @Test
  fun testMissionEntityWithHabitScheduleAppliesDaysAndEndRule() {
    // 2026-11-12 = Jueves
    val (thursdayClock, createdAt) = fixedClock(2026, Calendar.NOVEMBER, 12)
    val entity = MissionEntity(
      id = "habit_meditar",
      dimensionIndex = 2,
      title = "Meditar 5 min",
      description = "Respirar tranquilo",
      benefit = "Calma mental",
      durationLabel = "5 min",
      durationMinutes = 5,
      frequency = "Semanalmente",
      isHabit = true,
      expReward = 20,
      difficulty = "Fácil",
      habitDays = "Lun,Mié,Vie",
      habitEndRule = "Nunca",
      createdAtMillis = createdAt
    )

    val scheduled = entity.toBalanceMission().withHabitSchedule(thursdayClock)
    assertEquals(listOf("Lun", "Mié", "Vie"), scheduled.habitDays)
    assertFalse(scheduled.isHabitScheduledToday)
    assertFalse(scheduled.isHabitExpired)
    assertEquals("Vie", scheduled.nextHabitDayLabel)
  }
}
