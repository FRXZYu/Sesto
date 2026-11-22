package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.data.AppThemeMode
import com.example.data.AssessmentRepository
import com.example.data.BalanceRepository
import com.example.data.FirstDayOfWeek
import com.example.data.db.AppDatabase
import com.example.domain.CompletionRecord
import com.example.domain.DayKey
import com.example.ui.screens.SettingsSection
import com.example.ui.screens.WeeklyCalendarStripBuilder
import com.example.widget.EquilibrioWidgetCalculator
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsPreferencesTest {

  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var assessmentRepo: AssessmentRepository
  private lateinit var balanceRepository: BalanceRepository
  private lateinit var fixedDayKey: DayKey

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.NOVEMBER)
      set(Calendar.DAY_OF_MONTH, 12) // Thursday
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val millis = cal.timeInMillis
    fixedDayKey = DayKey { millis }

    database = Room.inMemoryDatabaseBuilder(
      context,
      AppDatabase::class.java
    ).allowMainThreadQueries().build()

    assessmentRepo = AssessmentRepository(context)
    assessmentRepo.resetAssessment()

    balanceRepository = BalanceRepository(
      context = context,
      database = database,
      assessmentRepository = assessmentRepo,
      dayKey = fixedDayKey
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `default app preferences enable habit confirmation and set daily goal to 3`() {
    val defaults = assessmentRepo.readAppPreferences()
    assertEquals(AppThemeMode.SYSTEM, defaults.themeMode)
    assertEquals(FirstDayOfWeek.MONDAY, defaults.firstDayOfWeek)
    assertEquals(3, defaults.dailyGoalCount)
    assertTrue(defaults.confirmHabitCompletion)
    assertEquals("", defaults.accountEmail)
    assertEquals("", defaults.lastGoalChangeReason)
  }

  @Test
  fun `settings preferences sync across repository instances and persist reason and email`() {
    val observerRepo = AssessmentRepository(context)
    assertEquals(AppThemeMode.SYSTEM, observerRepo.appPrefsFlow.value.themeMode)

    balanceRepository.updateAppPreferences(
      AppPreferences(
        themeMode = AppThemeMode.DARK,
        firstDayOfWeek = FirstDayOfWeek.SUNDAY,
        dailyGoalCount = 5,
        confirmHabitCompletion = false,
        accountEmail = "kirenia@equilibrio.app"
      )
    )
    balanceRepository.updateMonthlyGoal(
      goal = "Dormir mejor y disfrutar el proceso",
      reason = "Hacerla más realista"
    )
    balanceRepository.updateProfileIdentity(
      name = "Kirenia",
      zodiacSign = "Leo",
      zodiacSymbol = "♌"
    )

    val syncedPrefs = observerRepo.appPrefsFlow.value
    assertEquals(AppThemeMode.DARK, syncedPrefs.themeMode)
    assertEquals(FirstDayOfWeek.SUNDAY, syncedPrefs.firstDayOfWeek)
    assertEquals(5, syncedPrefs.dailyGoalCount)
    assertFalse(syncedPrefs.confirmHabitCompletion)
    assertEquals("kirenia@equilibrio.app", syncedPrefs.accountEmail)
    assertEquals("Hacerla más realista", syncedPrefs.lastGoalChangeReason)

    val syncedProfile = observerRepo.baseProfileFlow.value
    assertEquals("Kirenia", syncedProfile.name)
    assertEquals("Leo", syncedProfile.zodiacSign)
    assertEquals("♌", syncedProfile.zodiacSymbol)
    assertEquals("Dormir mejor y disfrutar el proceso", syncedProfile.monthlyGoal)
  }

  @Test
  fun `weekly calendar strip orders Domingo vs Lunes according to firstDayOfWeek`() {
    val today = fixedDayKey.today()
    val completions = listOf(
      CompletionRecord(
        id = 1L,
        missionId = "desc_1",
        dimensionIndex = 0,
        date = today,
        completedAtMillis = fixedDayKey.currentMillis()
      )
    )

    val sundayWeek = WeeklyCalendarStripBuilder.buildWeekDays(
      firstDayOfWeek = FirstDayOfWeek.SUNDAY,
      completions = completions,
      dayKey = fixedDayKey
    )
    assertEquals(listOf("D", "L", "M", "X", "J", "V", "S"), sundayWeek.map { it.dayLetter })
    assertEquals(1, sundayWeek.count { it.isToday })
    assertEquals(1, sundayWeek.first { it.isToday }.completedCount)
    assertTrue(sundayWeek[5].isFuture)
    assertTrue(sundayWeek[6].isFuture)

    val mondayWeek = WeeklyCalendarStripBuilder.buildWeekDays(
      firstDayOfWeek = FirstDayOfWeek.MONDAY,
      completions = completions,
      dayKey = fixedDayKey
    )
    assertEquals(listOf("L", "M", "X", "J", "V", "S", "D"), mondayWeek.map { it.dayLetter })
    assertEquals(1, mondayWeek.count { it.isToday })
    assertFalse(mondayWeek[0].isFuture)
    assertTrue(mondayWeek[6].isFuture)
  }

  @Test
  fun `ensureDailyPlanForToday and widget snapshot honor custom dailyGoalCount`() = runBlocking {
    val perceivedScores = mapOf(0 to 4, 1 to 6, 2 to 5, 3 to 7, 4 to 8, 5 to 6)

    assessmentRepo.setDailyGoalCount(5)
    val fivePlan = balanceRepository.ensureDailyPlanForToday(perceivedScores)
    assertEquals(5, fivePlan.size)

    val completedId = fivePlan.first().missionId
    balanceRepository.completeMissionWithReward(
      missionId = completedId,
      dimensionIndex = fivePlan.first().dimensionIndex,
      expAwarded = 20,
      countsTowardDailyCap = true
    )

    val widgetSnapshot = EquilibrioWidgetCalculator.calculateSnapshot(
      todayPlanMissionIds = fivePlan.map { it.missionId },
      todayCompletedMissionIds = setOf(completedId),
      realScoresFloat = mapOf(0 to 2f),
      activeStreakDays = 2,
      dailyGoalCount = 5
    )
    assertEquals("1 de 5 misiones hoy", widgetSnapshot.missionsLabel)

    assessmentRepo.setDailyGoalCount(2)
    val twoPlan = balanceRepository.ensureDailyPlanForToday(perceivedScores)
    assertEquals(2, twoPlan.size)
    assertTrue("Already completed action should be kept when daily goal shrinks", twoPlan.any { it.missionId == completedId })
  }

  @Test
  fun `settings screen exposes exactly 6 distinct sections`() {
    assertEquals(6, SettingsSection.entries.size)
  }
}
