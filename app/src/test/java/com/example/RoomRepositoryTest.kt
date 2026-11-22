package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssessmentRepository
import com.example.data.BalanceRepository
import com.example.data.db.AppDatabase
import com.example.data.db.MissionEntity
import com.example.domain.DayKey
import com.example.domain.HelpfulnessRating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomRepositoryTest {

  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var clockMillis: AtomicLong
  private lateinit var dayKey: DayKey
  private lateinit var repository: BalanceRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val startCal = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.NOVEMBER)
      set(Calendar.DAY_OF_MONTH, 12)
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 55)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    clockMillis = AtomicLong(startCal.timeInMillis)
    dayKey = DayKey { clockMillis.get() }

    database = Room.inMemoryDatabaseBuilder(
      context,
      AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val assessmentRepo = AssessmentRepository(context)
    assessmentRepo.resetAssessment()

    repository = BalanceRepository(
      context = context,
      database = database,
      assessmentRepository = assessmentRepo,
      dayKey = dayKey
    )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testInsertarActualizarReflexionYBorrarCompletacion() = runBlocking {
    val missionId = "desc_1"
    val date = dayKey.today()

    assertNull(database.completionDao().findCompletion(missionId, date))

    val insertId = repository.insertCompletion(
      missionId = missionId,
      dimensionIndex = 2,
      date = date,
      rating = HelpfulnessRating.HELPFUL,
      reflectionNote = "Me costó soltar el móvil al inicio pero dormí mejor."
    )
    assertTrue(insertId > 0)

    val saved = database.completionDao().findCompletion(missionId, date)
    assertNotNull(saved)
    assertEquals(HelpfulnessRating.HELPFUL.name, saved?.helpfulRating)
    assertEquals("Me costó soltar el móvil al inicio pero dormí mejor.", saved?.reflectionNote)

    // Actualizar reflexión posterior sin perder el registro
    repository.saveReflectionFeedback(
      missionId = missionId,
      dimensionIndex = 2,
      rating = HelpfulnessRating.NEUTRAL,
      reflectionNote = "Nota editada",
      date = date
    )

    val updated = database.completionDao().findCompletion(missionId, date)
    assertEquals(HelpfulnessRating.NEUTRAL.name, updated?.helpfulRating)
    assertEquals("Nota editada", updated?.reflectionNote)

    val deletedCount = repository.deleteCompletion(missionId, date)
    assertEquals(1, deletedCount)
    assertNull(database.completionDao().findCompletion(missionId, date))
  }

  @Test
  fun testToggleCompletacionInsertarYDesmarcar() = runBlocking {
    val missionId = "custom_ejer_99"
    val date = dayKey.today()

    val completed = repository.toggleMissionCompletion(
      missionId = missionId,
      dimensionIndex = 1,
      date = date
    )
    assertTrue(completed)
    assertNotNull(database.completionDao().findCompletion(missionId, date))

    val unmarked = repository.toggleMissionCompletion(
      missionId = missionId,
      dimensionIndex = 1,
      date = date
    )
    assertFalse(unmarked)
    assertNull(database.completionDao().findCompletion(missionId, date))
  }

  @Test
  fun testInsertarYReleerMisionPersonalizada() = runBlocking {
    val customMission = MissionEntity(
      id = "custom_yoga_1",
      dimensionIndex = 1,
      title = "Estiramiento tranquilo",
      description = "10 minutos suaves al despertar.",
      benefit = "Puede ayudarte a soltar tensión matutina.",
      durationLabel = "10 min",
      durationMinutes = 10,
      frequency = "Diariamente",
      isHabit = true
    )

    repository.insertCustomMission(customMission)

    val missions = repository.customMissions.first()
    assertEquals(1, missions.size)
    assertEquals("custom_yoga_1", missions[0].id)
    assertEquals(10, missions[0].durationMinutes)
    assertTrue(missions[0].isHabit)

    repository.deleteCustomMission("custom_yoga_1")
    assertTrue(repository.customMissions.first().isEmpty())
  }

  @Test
  fun testMidnightRolloverRefreshesDailyPlanWithoutLosingHistory() = runBlocking {
    val perceivedScores = (0..5).associateWith { 5 }
    repository.saveFocusDimensions(setOf(2)) // Descanso

    val day1Key = dayKey.today()
    val day1Plan = repository.ensureDailyPlanForToday(perceivedScores)
    assertEquals(3, day1Plan.size)
    assertEquals(day1Key, day1Plan.first().date)

    // Completar y valorar la primera acción del día 1
    val firstAction = day1Plan.first()
    repository.saveReflectionFeedback(
      missionId = firstAction.missionId,
      dimensionIndex = firstAction.dimensionIndex,
      rating = HelpfulnessRating.HELPFUL,
      reflectionNote = "Ayudó bastante",
      date = day1Key
    )
    assertTrue(repository.todayCompletedMissionIds.first().contains(firstAction.missionId))

    // Avanzar el reloj local 10 minutos para cruzar las 00:00 (2026-11-12 23:55 -> 2026-11-13 00:05)
    clockMillis.addAndGet(10 * 60 * 1000L)
    val changed = repository.refreshDayIfNeeded()
    assertTrue("Cruzar las 00:00 locales debe actualizar currentDayKey", changed)

    val day2Key = dayKey.today()
    assertNotEquals(day1Key, day2Key)
    assertEquals(day2Key, repository.currentDayKey.value)

    // El nuevo día empieza limpio en hoy, pero el historial completo conserva lo de ayer
    assertTrue(repository.todayCompletedMissionIds.first().isEmpty())
    val history = repository.allCompletions.first()
    assertEquals(1, history.size)
    assertEquals("Ayudó bastante", history.first().reflectionNote)

    // El nuevo plan diario a las 00:00 evita repetir las acciones ofrecidas/completadas ayer
    val day2Plan = repository.ensureDailyPlanForToday(perceivedScores)
    assertEquals(3, day2Plan.size)
    assertEquals(day2Key, day2Plan.first().date)
    val day1Ids = day1Plan.map { it.missionId }.toSet()
    val day2Ids = day2Plan.map { it.missionId }.toSet()
    assertTrue("La acción completada ayer no debe repetirse hoy", firstAction.missionId !in day2Ids)
    assertFalse("El plan de hoy debe traer acciones nuevas frente al de ayer", day1Ids == day2Ids)
  }

  @Test
  fun testSwapDailyMissionAndReplanForFocusChange() = runBlocking {
    val perceivedScores = (0..5).associateWith { 5 }
    repository.saveFocusDimensions(setOf(0))

    val initialPlan = repository.ensureDailyPlanForToday(perceivedScores)
    assertEquals(3, initialPlan.size)

    val swapped = repository.swapDailyMission(slotIndex = 0, perceivedScores = perceivedScores)
    assertNotNull(swapped)
    assertNotEquals(initialPlan[0].missionId, swapped?.missionId)
    assertTrue(swapped?.wasSwapped == true)

    // Cambiar enfoque a Hobbies (4) actualiza las ranuras pendientes
    repository.saveFocusDimensions(setOf(4))
    val replanned = repository.replanTodayForFocusChange(perceivedScores)
    assertEquals(3, replanned.size)
    assertTrue(replanned.any { it.dimensionIndex == 4 })
  }

  @Test
  fun testDailyExpCapAndHexagonExpSync() = runBlocking {
    val date = dayKey.today()
    // 5 completions at 35 EXP = 175 -> should cap at 150 EXP today
    for (i in 1..5) {
      repository.toggleMissionCompletion(
        missionId = "exp_test_$i",
        dimensionIndex = 0,
        expAwarded = 35,
        date = date
      )
    }

    val profile = repository.userProfileFlow.first()
    assertEquals(150, profile.expToday)
    assertEquals(150, profile.exp)
    assertEquals(1, profile.level)
    assertEquals(5, profile.completedMissions)

    val expByDim = repository.expByDimension14d.first()
    assertEquals(150, expByDim[0])
  }

  @Test
  fun testCompleteMissionWithRewardDetectsLevelUpAt200Exp() = runBlocking {
    // Acumular 180 EXP en días previos para que la misión de hoy cruce los 200 EXP
    repository.insertCompletion(
      missionId = "prev_1",
      dimensionIndex = 0,
      date = dayKey.daysAgo(2),
      expAwarded = 90
    )
    repository.insertCompletion(
      missionId = "prev_2",
      dimensionIndex = 1,
      date = dayKey.daysAgo(1),
      expAwarded = 90
    )

    val sameLevelReward = repository.completeMissionWithReward(
      missionId = "today_small",
      dimensionIndex = 2,
      expAwarded = 15,
      date = dayKey.today()
    )
    assertNotNull(sameLevelReward)
    assertFalse(sameLevelReward!!.leveledUp)
    assertEquals(1, sameLevelReward.newLevel)
    assertEquals(195, sameLevelReward.newTotalExp)

    val levelUpReward = repository.completeMissionWithReward(
      missionId = "today_levelup",
      dimensionIndex = 3,
      expAwarded = 20,
      date = dayKey.today()
    )
    assertNotNull(levelUpReward)
    assertTrue("Superar 200 EXP debe disparar leveledUp = true", levelUpReward!!.leveledUp)
    assertEquals(2, levelUpReward.newLevel)
    assertEquals(215, levelUpReward.newTotalExp)
  }

  @Test
  fun testDailyPlanExcludesOffDayAndExpiredHabits() = runBlocking {
    // 2026-11-12 es Jueves
    val createdMillis = clockMillis.get() - (2 * 24 * 60 * 60 * 1000L)
    val offDayHabit = MissionEntity(
      id = "habit_offday_only",
      dimensionIndex = 2,
      title = "Solo Lunes",
      description = "Hábito fuera de día",
      benefit = "Descanso",
      durationLabel = "5 min",
      durationMinutes = 5,
      isHabit = true,
      habitDays = "Lun",
      habitEndRule = "Nunca",
      createdAtMillis = createdMillis
    )
    val expiredHabit = MissionEntity(
      id = "habit_expired_only",
      dimensionIndex = 2,
      title = "Expirado",
      description = "Ya terminó su periodo",
      benefit = "Descanso",
      durationLabel = "5 min",
      durationMinutes = 5,
      isHabit = true,
      habitDays = "Jue",
      habitEndRule = "En 1 días",
      createdAtMillis = createdMillis
    )
    repository.insertCustomMission(offDayHabit)
    repository.insertCustomMission(expiredHabit)

    val plan = repository.ensureDailyPlanForToday((0..5).associateWith { 5 })
    val planIds = plan.map { it.missionId }.toSet()
    assertFalse("Hábito fuera de su día no debe entrar en Tus 3 para hoy", "habit_offday_only" in planIds)
    assertFalse("Hábito expirado no debe entrar en Tus 3 para hoy", "habit_expired_only" in planIds)
  }

  @Test
  fun testMigration3To4PreservesExistingCustomMissionsWithDefaults() {
    val dbName = "migration_3_4_test.db"
    context.deleteDatabase(dbName)

    val config = SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(dbName)
      .callback(object : SupportSQLiteOpenHelper.Callback(3) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_missions (
              id TEXT NOT NULL PRIMARY KEY,
              dimensionIndex INTEGER NOT NULL,
              title TEXT NOT NULL,
              description TEXT NOT NULL,
              benefit TEXT NOT NULL,
              durationLabel TEXT NOT NULL,
              durationMinutes INTEGER NOT NULL,
              frequency TEXT NOT NULL,
              isHabit INTEGER NOT NULL,
              expReward INTEGER NOT NULL,
              difficulty TEXT NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO custom_missions (id, dimensionIndex, title, description, benefit, durationLabel, durationMinutes, frequency, isHabit, expReward, difficulty)
            VALUES ('legacy_1', 1, 'Caminar', '10 min fuera', 'Salud', '10 min', 10, 'Diariamente', 1, 30, 'Media')
            """.trimIndent()
          )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
      })
      .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(config)
    val sqliteDb = helper.writableDatabase
    AppDatabase.MIGRATION_3_4.migrate(sqliteDb)

    sqliteDb.query("SELECT id, habitDays, habitEndRule, createdAtMillis FROM custom_missions WHERE id = 'legacy_1'").use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals("legacy_1", cursor.getString(0))
      assertEquals("", cursor.getString(1))
      assertEquals("Nunca", cursor.getString(2))
      assertEquals(0L, cursor.getLong(3))
    }

    helper.close()
    context.deleteDatabase(dbName)
  }

  @Test
  fun testFreezeStreakDeducts100ExpOncePerDayAndPreservesStreak() = runBlocking {
    // Caso 1: racha insuficiente (< 2) no permite congelar
    repository.insertCompletion(
      missionId = "m_exp_only",
      dimensionIndex = 0,
      date = dayKey.yesterday(),
      expAwarded = 120
    )
    val deniedLowStreak = repository.freezeStreakToday()
    assertFalse(deniedLowStreak.success)
    assertEquals(120, repository.userProfileFlow.first().exp)

    // Sumar anteayer para llegar a racha = 2, pero primero probar con EXP < 100 usando otra instancia limpia
    repository.deleteCompletion("m_exp_only", dayKey.yesterday())
    repository.insertCompletion("m_y1", 0, dayKey.yesterday(), expAwarded = 40)
    repository.insertCompletion("m_y2", 1, dayKey.daysAgo(2), expAwarded = 40)
    val deniedLowExp = repository.freezeStreakToday()
    assertFalse(deniedLowExp.success)

    // Agregar 40 EXP más anteayer -> total 120 EXP, racha = 2 -> éxito
    repository.insertCompletion("m_y3", 2, dayKey.daysAgo(2), expAwarded = 40)
    assertEquals(2, repository.userProfileFlow.first().activeStreakDays)
    assertEquals(120, repository.userProfileFlow.first().exp)

    val frozen = repository.freezeStreakToday()
    assertTrue(frozen.success)
    assertEquals("Racha protegida. Mañana te esperamos", frozen.message)
    assertEquals(20, frozen.remainingExp)
    assertEquals(3, frozen.newStreak)

    val profileAfter = repository.userProfileFlow.first()
    assertEquals(20, profileAfter.exp)
    assertEquals(3, profileAfter.activeStreakDays)
    assertTrue(repository.isFrozenToday.first())

    // Máximo 1 congelación por día (incluso si hubiera más EXP)
    repository.insertCompletion("m_bonus", 3, dayKey.daysAgo(3), expAwarded = 150)
    val secondAttemptSameDay = repository.freezeStreakToday()
    assertFalse(secondAttemptSameDay.success)
  }

  @Test
  fun testWeeklyChallenge80ExpBypassesDailyCapStillAdvancesLevelAndHexagon() = runBlocking {
    val today = dayKey.today()
    // Alcanzar el tope diario de 150 EXP
    for (i in 1..5) {
      repository.completeMissionWithReward(
        missionId = "cap_fill_$i",
        dimensionIndex = 0,
        expAwarded = 30,
        date = today,
        countsTowardDailyCap = true
      )
    }
    val beforeWeekly = repository.userProfileFlow.first()
    assertEquals(150, beforeWeekly.expToday)
    assertEquals(150, beforeWeekly.exp)
    assertEquals(1, beforeWeekly.level)

    // Completar reto semanal (+80 EXP) sin contar para el cap diario
    val weeklyResult = repository.completeMissionWithReward(
      missionId = "weekly_2026_w46_d4_t0",
      dimensionIndex = 4,
      expAwarded = 80,
      date = today,
      countsTowardDailyCap = false
    )
    assertNotNull(weeklyResult)
    assertEquals(80, weeklyResult!!.expEarned)
    assertFalse(weeklyResult.cappedByDailyLimit)
    assertEquals(230, weeklyResult.newTotalExp)
    assertEquals(2, weeklyResult.newLevel)
    assertTrue(weeklyResult.leveledUp)

    val afterWeekly = repository.userProfileFlow.first()
    assertEquals(150, afterWeekly.expToday)
    assertEquals(230, afterWeekly.exp)
    assertEquals(2, afterWeekly.level)

    val expByDim = repository.expByDimension14d.first()
    assertEquals(80, expByDim[4])
  }

  @Test
  fun testMigration4To5PreservesCompletionsAndCreatesStreakFreezesTable() {
    val dbName = "migration_4_5_test.db"
    context.deleteDatabase(dbName)

    val config = SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(dbName)
      .callback(object : SupportSQLiteOpenHelper.Callback(4) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS completions (
              id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              missionId TEXT NOT NULL,
              dimensionIndex INTEGER NOT NULL,
              date TEXT NOT NULL,
              helpfulRating TEXT,
              reflectionNote TEXT NOT NULL,
              completedAtMillis INTEGER NOT NULL,
              expAwarded INTEGER NOT NULL
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO completions (missionId, dimensionIndex, date, helpfulRating, reflectionNote, completedAtMillis, expAwarded)
            VALUES ('desc_1', 2, '2026-11-11', 'HELPFUL', 'Dormí bien', 1762850000000, 30)
            """.trimIndent()
          )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
      })
      .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(config)
    val sqliteDb = helper.writableDatabase
    AppDatabase.MIGRATION_4_5.migrate(sqliteDb)

    sqliteDb.query("SELECT missionId, expAwarded, countsTowardDailyCap FROM completions WHERE missionId = 'desc_1'").use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals("desc_1", cursor.getString(0))
      assertEquals(30, cursor.getInt(1))
      assertEquals(1, cursor.getInt(2))
    }

    sqliteDb.execSQL(
      "INSERT INTO streak_freezes (date, expSpent, createdAtMillis) VALUES ('2026-11-12', 100, 1762936400000)"
    )
    sqliteDb.query("SELECT date, expSpent FROM streak_freezes WHERE date = '2026-11-12'").use { cursor ->
      assertTrue(cursor.moveToFirst())
      assertEquals("2026-11-12", cursor.getString(0))
      assertEquals(100, cursor.getInt(1))
    }

    helper.close()
    context.deleteDatabase(dbName)
  }
}
