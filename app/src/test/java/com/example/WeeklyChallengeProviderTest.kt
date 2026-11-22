package com.example

import com.example.domain.DayKey
import com.example.domain.WeeklyChallengeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WeeklyChallengeProviderTest {

  private fun clockFor(year: Int, month: Int, day: Int, hour: Int = 10): DayKey {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val millis = cal.timeInMillis
    return DayKey { millis }
  }

  @Test
  fun testChallengeStaysSameAcrossWeekAndRotatesOnMonday() {
    // 2026-11-09 = Lunes, 2026-11-12 = Jueves, 2026-11-15 = Domingo, 2026-11-16 = Siguiente Lunes
    val mondayClock = clockFor(2026, Calendar.NOVEMBER, 9)
    val thursdayClock = clockFor(2026, Calendar.NOVEMBER, 12)
    val sundayClock = clockFor(2026, Calendar.NOVEMBER, 15, hour = 23)
    val nextMondayClock = clockFor(2026, Calendar.NOVEMBER, 16, hour = 8)

    val realScores = mapOf(0 to 6, 1 to 5, 2 to 2, 3 to 7, 4 to 8, 5 to 6)
    val perceivedScores = (0..5).associateWith { 5 }

    val mondayChallenge = WeeklyChallengeProvider.getChallengeForWeek(realScores, perceivedScores, mondayClock)
    val thursdayChallenge = WeeklyChallengeProvider.getChallengeForWeek(realScores, perceivedScores, thursdayClock)
    val sundayChallenge = WeeklyChallengeProvider.getChallengeForWeek(realScores, perceivedScores, sundayClock)
    val nextMondayChallenge = WeeklyChallengeProvider.getChallengeForWeek(realScores, perceivedScores, nextMondayClock)

    assertEquals(mondayChallenge.id, thursdayChallenge.id)
    assertEquals(mondayChallenge.id, sundayChallenge.id)
    assertEquals(mondayChallenge.title, sundayChallenge.title)

    assertNotEquals(
      "El lunes siguiente debe rotar de forma determinista el reto semanal",
      sundayChallenge.id,
      nextMondayChallenge.id
    )
    assertNotEquals(sundayChallenge.title, nextMondayChallenge.title)

    // Ambos apuntan a la dimensión más baja real (Descanso = 2)
    assertEquals(2, mondayChallenge.dimensionIndex)
    assertEquals(2, nextMondayChallenge.dimensionIndex)
  }

  @Test
  fun testChallengeTargetsLowestDimensionWith80ExpNoDailyCapAnd3To4Steps() {
    val clock = clockFor(2026, Calendar.NOVEMBER, 12)
    val realScores = mapOf(0 to 9, 1 to 7, 2 to 8, 3 to 6, 4 to 1, 5 to 8)
    val perceivedScores = (0..5).associateWith { 6 }

    val challenge = WeeklyChallengeProvider.getChallengeForWeek(realScores, perceivedScores, clock)

    assertEquals(4, challenge.dimensionIndex)
    assertEquals(80, challenge.expReward)
    assertFalse(challenge.countsTowardDailyCap)
    assertTrue(challenge.isWeeklyChallenge)
    assertTrue(challenge.durationMinutes in 45..60)
    assertTrue(challenge.steps.size in 3..4)

    // Round-trip mediante findById
    val restored = WeeklyChallengeProvider.findById(challenge.id)
    assertNotNull(restored)
    assertEquals(challenge.id, restored?.id)
    assertEquals(challenge.title, restored?.title)
    assertEquals(80, restored?.expReward)
    assertFalse(restored?.countsTowardDailyCap ?: true)
  }

  @Test
  fun testYearBoundaryIsoWeekRemainsConsistentBetweenDec29AndJan3() {
    // 2025-12-29 es Lunes y 2026-01-03 es Sábado: misma semana ISO (2026-W01)
    val dec29Clock = clockFor(2025, Calendar.DECEMBER, 29)
    val jan3Clock = clockFor(2026, Calendar.JANUARY, 3)

    val seedDec = WeeklyChallengeProvider.currentIsoWeekSeed(dec29Clock)
    val seedJan = WeeklyChallengeProvider.currentIsoWeekSeed(jan3Clock)

    assertEquals(seedDec, seedJan)
  }
}
