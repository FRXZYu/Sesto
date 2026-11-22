package com.example.data

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.CompletionEntity
import com.example.data.db.DailyPlanEntity
import com.example.data.db.MissionEntity
import com.example.data.db.StreakFreezeEntity
import com.example.domain.CompletionRecord
import com.example.domain.DailyMissionPlanner
import com.example.domain.DailyPlanHistoryItem
import com.example.domain.DayKey
import com.example.domain.HelpfulnessRating
import com.example.domain.HelpfulnessSummary
import com.example.domain.HexagonCalculator
import com.example.model.AssessmentReport
import com.example.model.BalanceMission
import com.example.model.GamificationStats
import com.example.model.MissionRewardResult
import com.example.model.MissionsProvider
import com.example.model.UserProfile
import com.example.widget.EquilibrioWidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StreakFreezeResult(
  val success: Boolean,
  val message: String,
  val newStreak: Int = 0,
  val remainingExp: Int = 0
)

fun CompletionEntity.toDomain(): CompletionRecord {
  return CompletionRecord(
    id = id,
    missionId = missionId,
    dimensionIndex = dimensionIndex,
    date = date,
    helpfulness = HelpfulnessRating.fromStorage(helpfulRating),
    reflectionNote = reflectionNote,
    completedAtMillis = completedAtMillis,
    expAwarded = expAwarded,
    countsTowardDailyCap = countsTowardDailyCap
  )
}

/**
 * Repositorio unificado que combina Room (acciones personalizadas, historial con valoración
 * de utilidad y plan diario a las 00:00) con AssessmentRepository (cuestionario, enfoque y perfil).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BalanceRepository(
  context: Context,
  val database: AppDatabase = AppDatabase.getInstance(context),
  val assessmentRepository: AssessmentRepository = AssessmentRepository(context),
  val dayKey: DayKey = DayKey.DEFAULT
) {

  private val appContext = context.applicationContext
  val missionDao = database.missionDao()
  val completionDao = database.completionDao()
  val dailyPlanDao = database.dailyPlanDao()
  val streakFreezeDao = database.streakFreezeDao()

  companion object {
    const val STREAK_FREEZE_COST_EXP = 100
    const val MIN_STREAK_FOR_FREEZE = 2
  }

  private val planMutex = Mutex()
  private val _currentDayKey = MutableStateFlow(dayKey.today())
  val currentDayKey: StateFlow<String> = _currentDayKey.asStateFlow()

  /**
   * Comprueba si el reloj local cruzó las 00:00 y actualiza el flujo reactivo del día actual.
   * Retorna true si cambió de día.
   */
  fun refreshDayIfNeeded(): Boolean {
    val today = dayKey.today()
    return if (_currentDayKey.value != today) {
      _currentDayKey.value = today
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      true
    } else {
      false
    }
  }

  // ==========================================
  // Misiones y hábitos personalizados (Room)
  // ==========================================
  val customMissions: Flow<List<MissionEntity>> = missionDao.getAllCustomMissions()

  suspend fun insertCustomMission(mission: MissionEntity) {
    missionDao.insertCustomMission(mission)
  }

  suspend fun deleteCustomMission(id: String) {
    missionDao.deleteCustomMissionById(id)
  }

  // ==========================================
  // Áreas de enfoque elegidas y guardadas
  // ==========================================
  val chosenFocusDimensions: Flow<Set<Int>> = assessmentRepository.chosenFocusDimensionsFlow
  val savedMissionIds: Flow<Set<String>> = assessmentRepository.savedMissionIdsFlow

  fun saveFocusDimensions(dimensions: Set<Int>) {
    assessmentRepository.saveFocusDimensions(dimensions)
  }

  fun toggleFocusDimension(dimensionIndex: Int): Set<Int> {
    return assessmentRepository.toggleFocusDimension(dimensionIndex)
  }

  // ==========================================
  // Historial completo, últimos 14 días y hoy
  // ==========================================
  val allCompletions: Flow<List<CompletionRecord>> = completionDao.getAllCompletions().map { list ->
    list.map { it.toDomain() }
  }

  val completionsLast14Days: Flow<List<CompletionRecord>> = combine(allCompletions, currentDayKey) { list, _ ->
    val validDays = dayKey.getRangeLastDays(14).toSet()
    list.filter { it.date in validDays }
  }

  val realScores: Flow<Map<Int, Int>> = combine(completionsLast14Days, currentDayKey) { completions14d, _ ->
    HexagonCalculator.calculateRealScores(completions14d, dayKey)
  }

  val realScoresFloat: Flow<Map<Int, Float>> = combine(completionsLast14Days, currentDayKey) { completions14d, _ ->
    HexagonCalculator.calculateRealScoresFloat(completions14d, dayKey)
  }

  val actionsByDimension14d: Flow<Map<Int, Int>> = combine(completionsLast14Days, currentDayKey) { completions14d, _ ->
    HexagonCalculator.calculateActionsByDimension14d(completions14d, dayKey)
  }

  val expByDimension14d: Flow<Map<Int, Int>> = combine(completionsLast14Days, currentDayKey) { completions14d, _ ->
    HexagonCalculator.calculateExpByDimension14d(completions14d, dayKey)
  }

  val helpfulnessSummary: Flow<HelpfulnessSummary> = allCompletions.map { completions ->
    HexagonCalculator.calculateHelpfulnessSummary(completions)
  }

  val todayCompletions: Flow<List<CompletionRecord>> = currentDayKey.flatMapLatest { date ->
    completionDao.getCompletionsForDate(date).map { list -> list.map { it.toDomain() } }
  }

  val todayCompletedMissionIds: Flow<Set<String>> = todayCompletions.map { list ->
    list.map { it.missionId }.toSet()
  }

  val todayDailyPlan: Flow<List<DailyPlanEntity>> = currentDayKey.flatMapLatest { date ->
    dailyPlanDao.getPlanForDate(date)
  }

  val allFreezes: Flow<List<StreakFreezeEntity>> = streakFreezeDao.getAllFreezes()

  val frozenDates: Flow<Set<String>> = allFreezes.map { freezes ->
    freezes.map { it.date }.toSet()
  }

  val isFrozenToday: Flow<Boolean> = combine(frozenDates, currentDayKey) { dates, today ->
    today in dates
  }

  val acceptedWeeklyChallengeId: Flow<String?> = assessmentRepository.acceptedWeeklyChallengeIdFlow
  val notificationPreferences: Flow<NotificationPreferences> = assessmentRepository.notificationPrefsFlow
  val appPreferences: Flow<AppPreferences> = assessmentRepository.appPrefsFlow

  val userProfileFlow: Flow<UserProfile> = combine(
    allCompletions,
    allFreezes,
    assessmentRepository.baseProfileFlow,
    currentDayKey
  ) { completions, freezes, baseProfile, todayDate ->
    val activeDays = HexagonCalculator.calculateActiveDaysLast14(completions, dayKey)
    val frozenDateSet = freezes.map { it.date }.toSet()
    val streak = HexagonCalculator.calculateStreak(completions, dayKey, frozenDateSet)
    val spentExp = freezes.sumOf { it.expSpent.coerceAtLeast(0) }
    val totalExp = HexagonCalculator.calculateTotalExp(completions, freezeExpSpent = spentExp)
    val expToday = HexagonCalculator.calculateExpOnDate(completions, todayDate, onlyCapped = true)
    val stats = GamificationStats(totalExp = totalExp, expToday = expToday)
    baseProfile.copy(
      level = stats.level,
      rank = stats.rank,
      exp = totalExp,
      expToday = expToday,
      completedMissions = completions.size,
      activeStreakDays = streak,
      completedActionsTotal = completions.size,
      activeDaysRecent = activeDays
    )
  }

  // ==========================================
  // Plan diario automático a las 00:00
  // ==========================================

  private suspend fun buildFullCatalog(perceivedScores: Map<Int, Int>): List<BalanceMission> {
    val focus = assessmentRepository.chosenFocusDimensionsFlow.value
    val base = MissionsProvider.generateMissionsForUser(perceivedScores, focus)
    val custom = missionDao.getAllCustomMissions().first()
      .map { it.toBalanceMission().withHabitSchedule(dayKey) }
      .filter { !it.isHabitExpired && it.isHabitScheduledToday }
    return (custom + base).distinctBy { it.id }
  }

  /**
   * Garantiza que exista un plan personalizado de hasta 3 acciones concretas para la fecha actual.
   * Si cambió de día (00:00 hora local) o aún no hay plan hoy, genera uno evitando repeticiones.
   */
  suspend fun ensureDailyPlanForToday(perceivedScores: Map<Int, Int>): List<DailyPlanEntity> {
    refreshDayIfNeeded()
    val today = dayKey.today()
    return planMutex.withLock {
      val targetCount = assessmentRepository.readAppPreferences().dailyGoalCount.coerceIn(1, 6)
      val existing = dailyPlanDao.getPlanForDateOnce(today)
      val catalog = buildFullCatalog(perceivedScores)
      val catalogIds = catalog.map { it.id }.toSet()
      val validExisting = existing.filter { it.missionId in catalogIds }

      val maxPossibleCount = minOf(targetCount, catalog.size)
      if (validExisting.isNotEmpty() &&
        validExisting.size == existing.size &&
        validExisting.size == maxPossibleCount
      ) {
        return@withLock validExisting
      }

      val focusDims = assessmentRepository.chosenFocusDimensionsFlow.value
      val savedIds = assessmentRepository.savedMissionIdsFlow.value
      val recentDays = dayKey.getRangeLastDays(14)
      val recentCompletions = completionDao.getCompletionsForDates(recentDays).first().map { it.toDomain() }
      val real = HexagonCalculator.calculateRealScores(recentCompletions, dayKey)

      val recentPlanDays = dayKey.getRangeLastDays(DailyMissionPlanner.OFFERED_COOLDOWN_DAYS + 1)
      val planHistory = dailyPlanDao.getPlansInDatesOnce(recentPlanDays).map {
        DailyPlanHistoryItem(date = it.date, missionId = it.missionId, slotIndex = it.slotIndex)
      }

      val todayCompleted = completionDao.getCompletionsForDate(today).first().map { it.missionId }.toSet()
      val preserved = validExisting
        .sortedBy { it.missionId !in todayCompleted }
        .take(targetCount)
      val preservedIds = preserved.map { it.missionId }.toSet()
      val needed = (targetCount - preserved.size).coerceAtLeast(0)

      val extra = if (needed > 0) {
        DailyMissionPlanner.planDailyMissions(
          todayKey = today,
          catalog = catalog.filter { it.id !in preservedIds }.map { it.toCandidate() },
          chosenFocusDimensions = focusDims,
          perceivedScores = perceivedScores,
          realScores = real,
          recentCompletions = recentCompletions,
          recentPlanHistory = planHistory,
          savedMissionIds = savedIds,
          dayKey = dayKey,
          targetCount = needed
        )
      } else {
        emptyList()
      }

      val merged = preserved.mapIndexed { idx, old ->
        old.copy(slotIndex = idx)
      } + extra.mapIndexed { idx, candidate ->
        DailyPlanEntity(
          date = today,
          slotIndex = preserved.size + idx,
          missionId = candidate.id,
          dimensionIndex = candidate.dimensionIndex,
          wasSwapped = false
        )
      }
      val entities = merged.take(targetCount)
      dailyPlanDao.deletePlanForDate(today)
      if (entities.isNotEmpty()) {
        dailyPlanDao.insertPlanEntries(entities)
      }
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      entities
    }
  }

  /**
   * Regenera las acciones no completadas de hoy cuando la persona cambia sus áreas de enfoque.
   */
  suspend fun replanTodayForFocusChange(perceivedScores: Map<Int, Int>): List<DailyPlanEntity> {
    refreshDayIfNeeded()
    val today = dayKey.today()
    return planMutex.withLock {
      val existing = dailyPlanDao.getPlanForDateOnce(today)
      val todayCompleted = completionDao.getCompletionsForDate(today).first().map { it.missionId }.toSet()
      val keptCompleted = existing.filter { it.missionId in todayCompleted }

      val catalog = buildFullCatalog(perceivedScores)
      val focusDims = assessmentRepository.chosenFocusDimensionsFlow.value
      val savedIds = assessmentRepository.savedMissionIdsFlow.value
      val recentDays = dayKey.getRangeLastDays(14)
      val recentCompletions = completionDao.getCompletionsForDates(recentDays).first().map { it.toDomain() }
      val real = HexagonCalculator.calculateRealScores(recentCompletions, dayKey)
      val recentPlanDays = dayKey.getRangeLastDays(DailyMissionPlanner.OFFERED_COOLDOWN_DAYS + 1)
      val planHistory = dailyPlanDao.getPlansInDatesOnce(recentPlanDays).map {
        DailyPlanHistoryItem(date = it.date, missionId = it.missionId, slotIndex = it.slotIndex)
      }

      val targetCount = assessmentRepository.readAppPreferences().dailyGoalCount.coerceIn(1, 6)
      val availableCatalog = catalog.filter { it.id !in todayCompleted }.map { it.toCandidate() }
      val fresh = DailyMissionPlanner.planDailyMissions(
        todayKey = today,
        catalog = availableCatalog,
        chosenFocusDimensions = focusDims,
        perceivedScores = perceivedScores,
        realScores = real,
        recentCompletions = recentCompletions,
        recentPlanHistory = planHistory,
        savedMissionIds = savedIds,
        dayKey = dayKey,
        targetCount = targetCount
      )

      val keptTrimmed = keptCompleted.take(targetCount)
      val neededSlots = (0 until targetCount).filter { slot ->
        keptTrimmed.none { it.slotIndex == slot }
      }
      val filled = neededSlots.zip(fresh).map { (slot, candidate) ->
        DailyPlanEntity(
          date = today,
          slotIndex = slot,
          missionId = candidate.id,
          dimensionIndex = candidate.dimensionIndex,
          wasSwapped = false
        )
      }
      val merged = (keptTrimmed + filled).sortedBy { it.slotIndex }
      dailyPlanDao.deletePlanForDate(today)
      if (merged.isNotEmpty()) {
        dailyPlanDao.insertPlanEntries(merged)
      }
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      merged
    }
  }

  /**
   * Permite cambiar suavemente una acción del plan de hoy por otra alternativa realista sin penalización.
   */
  suspend fun swapDailyMission(
    slotIndex: Int,
    perceivedScores: Map<Int, Int>
  ): DailyPlanEntity? {
    refreshDayIfNeeded()
    val today = dayKey.today()
    return planMutex.withLock {
      val currentPlan = dailyPlanDao.getPlanForDateOnce(today)
      val target = currentPlan.firstOrNull { it.slotIndex == slotIndex } ?: return@withLock null
      val catalog = buildFullCatalog(perceivedScores)
      val currentCandidate = catalog.firstOrNull { it.id == target.missionId }?.toCandidate()
        ?: com.example.domain.MissionCandidate(
          id = target.missionId,
          dimensionIndex = target.dimensionIndex,
          durationMinutes = 10
        )

      val recentDays = dayKey.getRangeLastDays(14)
      val recentCompletions = completionDao.getCompletionsForDates(recentDays).first().map { it.toDomain() }
      val recentPlanDays = dayKey.getRangeLastDays(DailyMissionPlanner.OFFERED_COOLDOWN_DAYS + 1)
      val planHistory = dailyPlanDao.getPlansInDatesOnce(recentPlanDays).map {
        DailyPlanHistoryItem(date = it.date, missionId = it.missionId, slotIndex = it.slotIndex)
      }

      val replacement = DailyMissionPlanner.pickReplacementMission(
        todayKey = today,
        currentDayMissionIds = currentPlan.map { it.missionId }.toSet(),
        replacingMission = currentCandidate,
        catalog = catalog.map { it.toCandidate() },
        chosenFocusDimensions = assessmentRepository.chosenFocusDimensionsFlow.value,
        recentCompletions = recentCompletions,
        recentPlanHistory = planHistory,
        dayKey = dayKey
      ) ?: return@withLock null

      dailyPlanDao.updatePlanSlot(
        date = today,
        slotIndex = slotIndex,
        newMissionId = replacement.id,
        newDimensionIndex = replacement.dimensionIndex
      )
      val replacementEntity = DailyPlanEntity(
        date = today,
        slotIndex = slotIndex,
        missionId = replacement.id,
        dimensionIndex = replacement.dimensionIndex,
        wasSwapped = true
      )
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      replacementEntity
    }
  }

  // ==========================================
  // Registro, revisión de utilidad y congelación de racha
  // ==========================================

  suspend fun toggleMissionCompletion(
    missionId: String,
    dimensionIndex: Int,
    expAwarded: Int = 20,
    date: String = dayKey.today(),
    countsTowardDailyCap: Boolean = true
  ): Boolean {
    refreshDayIfNeeded()
    val existing = completionDao.findCompletion(missionId, date)
    return if (existing != null) {
      completionDao.deleteCompletion(missionId, date)
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      false
    } else {
      val finalExp = if (countsTowardDailyCap) {
        val expTodayCapped = completionDao.getCompletionsForDate(date).first()
          .filter { it.countsTowardDailyCap }
          .sumOf { it.expAwarded }
        val remainingCap = (GamificationStats.DAILY_EXP_CAP - expTodayCapped).coerceAtLeast(0)
        expAwarded.coerceIn(0, remainingCap)
      } else {
        expAwarded.coerceAtLeast(0)
      }
      completionDao.insertCompletion(
        CompletionEntity(
          missionId = missionId,
          dimensionIndex = dimensionIndex,
          date = date,
          helpfulRating = null,
          reflectionNote = "",
          completedAtMillis = dayKey.currentMillis(),
          expAwarded = finalExp,
          countsTowardDailyCap = countsTowardDailyCap
        )
      )
      assessmentRepository.removeSavedMission(missionId)
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      true
    }
  }

  /**
   * Alterna la compleción de una misión devolviendo un [MissionRewardResult] si quedó completada
   * (comparando el nivel antes y después con [HexagonCalculator.calculateLevel]), o null si se desmarcó.
   */
  suspend fun completeMissionWithReward(
    missionId: String,
    dimensionIndex: Int,
    expAwarded: Int = 20,
    date: String = dayKey.today(),
    countsTowardDailyCap: Boolean = true
  ): MissionRewardResult? {
    val freezesBefore = streakFreezeDao.getAllFreezesOnce()
    val spentExp = freezesBefore.sumOf { it.expSpent.coerceAtLeast(0) }
    val frozenSet = freezesBefore.map { it.date }.toSet()

    val before = completionDao.getAllCompletions().first().map { it.toDomain() }
    val oldTotalExp = HexagonCalculator.calculateTotalExp(before, freezeExpSpent = spentExp)
    val oldLevel = HexagonCalculator.calculateLevel(oldTotalExp)

    val completedNow = toggleMissionCompletion(
      missionId = missionId,
      dimensionIndex = dimensionIndex,
      expAwarded = expAwarded,
      date = date,
      countsTowardDailyCap = countsTowardDailyCap
    )
    if (!completedNow) return null

    val after = completionDao.getAllCompletions().first().map { it.toDomain() }
    val newTotalExp = HexagonCalculator.calculateTotalExp(after, freezeExpSpent = spentExp)
    val newLevel = HexagonCalculator.calculateLevel(newTotalExp)
    val actualExpEarned = (newTotalExp - oldTotalExp).coerceAtLeast(0)
    val streak = HexagonCalculator.calculateStreak(after, dayKey, frozenSet)
    val capped = countsTowardDailyCap && actualExpEarned < expAwarded.coerceAtLeast(0)

    return MissionRewardResult(
      missionId = missionId,
      expEarned = actualExpEarned,
      cappedByDailyLimit = capped,
      newTotalExp = newTotalExp,
      newLevel = newLevel,
      leveledUp = newLevel > oldLevel,
      newStreak = streak,
      totalCompleted = after.size,
      message = if (newLevel > oldLevel) {
        "¡Subiste al Nivel $newLevel (${HexagonCalculator.calculateRank(newLevel)})!"
      } else {
        "+$actualExpEarned EXP sumados a tu equilibrio"
      }
    )
  }

  /**
   * Congela la racha de hoy gastando 100 EXP.
   * Máximo 1 congelación por día y solo disponible si activeStreakDays >= 2 y totalExp >= 100.
   */
  suspend fun freezeStreakToday(): StreakFreezeResult {
    refreshDayIfNeeded()
    val today = dayKey.today()
    return planMutex.withLock {
      val existing = streakFreezeDao.getFreezeForDate(today)
      if (existing != null) {
        return@withLock StreakFreezeResult(
          success = false,
          message = "Ya protegiste tu racha hoy. Mañana te esperamos"
        )
      }

      val completions = completionDao.getAllCompletions().first().map { it.toDomain() }
      val freezes = streakFreezeDao.getAllFreezesOnce()
      val frozenSet = freezes.map { it.date }.toSet()
      val currentStreak = HexagonCalculator.calculateStreak(completions, dayKey, frozenSet)
      val currentExp = HexagonCalculator.calculateTotalExp(
        completions,
        freezeExpSpent = freezes.sumOf { it.expSpent.coerceAtLeast(0) }
      )

      if (currentStreak < MIN_STREAK_FOR_FREEZE) {
        return@withLock StreakFreezeResult(
          success = false,
          message = "Necesitas al menos $MIN_STREAK_FOR_FREEZE días de racha para congelarla",
          newStreak = currentStreak,
          remainingExp = currentExp
        )
      }

      if (currentExp < STREAK_FREEZE_COST_EXP) {
        return@withLock StreakFreezeResult(
          success = false,
          message = "Necesitas $STREAK_FREEZE_COST_EXP EXP para proteger tu racha",
          newStreak = currentStreak,
          remainingExp = currentExp
        )
      }

      val insertedId = streakFreezeDao.insertFreeze(
        StreakFreezeEntity(
          date = today,
          expSpent = STREAK_FREEZE_COST_EXP,
          createdAtMillis = dayKey.currentMillis()
        )
      )
      if (insertedId <= 0L) {
        return@withLock StreakFreezeResult(
          success = false,
          message = "Ya protegiste tu racha hoy. Mañana te esperamos"
        )
      }

      val updatedFrozenSet = frozenSet + today
      val updatedStreak = HexagonCalculator.calculateStreak(completions, dayKey, updatedFrozenSet)
      val remainingExp = (currentExp - STREAK_FREEZE_COST_EXP).coerceAtLeast(0)
      EquilibrioWidgetUpdater.requestUpdate(appContext)
      StreakFreezeResult(
        success = true,
        message = "Racha protegida. Mañana te esperamos",
        newStreak = updatedStreak,
        remainingExp = remainingExp
      )
    }
  }

  suspend fun saveReflectionFeedback(
    missionId: String,
    dimensionIndex: Int,
    rating: HelpfulnessRating?,
    reflectionNote: String,
    date: String = dayKey.today(),
    expAwarded: Int = 20,
    countsTowardDailyCap: Boolean = true
  ) {
    refreshDayIfNeeded()
    val cleanNote = reflectionNote.trim()
    val existing = completionDao.findCompletion(missionId, date)
    if (existing != null) {
      completionDao.updateCompletionFeedback(
        missionId = missionId,
        date = date,
        helpfulRating = rating?.name,
        reflectionNote = cleanNote
      )
    } else {
      val finalExp = if (countsTowardDailyCap) {
        val expToday = completionDao.getCompletionsForDate(date).first()
          .filter { it.countsTowardDailyCap }
          .sumOf { it.expAwarded }
        val remainingCap = (GamificationStats.DAILY_EXP_CAP - expToday).coerceAtLeast(0)
        expAwarded.coerceIn(0, remainingCap)
      } else {
        expAwarded.coerceAtLeast(0)
      }
      completionDao.insertCompletion(
        CompletionEntity(
          missionId = missionId,
          dimensionIndex = dimensionIndex,
          date = date,
          helpfulRating = rating?.name,
          reflectionNote = cleanNote,
          completedAtMillis = dayKey.currentMillis(),
          expAwarded = finalExp,
          countsTowardDailyCap = countsTowardDailyCap
        )
      )
      assessmentRepository.removeSavedMission(missionId)
      EquilibrioWidgetUpdater.requestUpdate(appContext)
    }
  }

  suspend fun deleteCompletion(missionId: String, date: String = dayKey.today()): Int {
    val rows = completionDao.deleteCompletion(missionId, date)
    if (rows > 0) {
      EquilibrioWidgetUpdater.requestUpdate(appContext)
    }
    return rows
  }

  suspend fun insertCompletion(
    missionId: String,
    dimensionIndex: Int,
    date: String = dayKey.today(),
    rating: HelpfulnessRating? = null,
    reflectionNote: String = "",
    expAwarded: Int = 20,
    countsTowardDailyCap: Boolean = true
  ): Long {
    val id = completionDao.insertCompletion(
      CompletionEntity(
        missionId = missionId,
        dimensionIndex = dimensionIndex,
        date = date,
        helpfulRating = rating?.name,
        reflectionNote = reflectionNote.trim(),
        completedAtMillis = dayKey.currentMillis(),
        expAwarded = expAwarded,
        countsTowardDailyCap = countsTowardDailyCap
      )
    )
    EquilibrioWidgetUpdater.requestUpdate(appContext)
    return id
  }

  fun acceptWeeklyChallenge(mission: BalanceMission) {
    assessmentRepository.acceptWeeklyChallenge(mission.id)
  }

  fun updateNotificationPreferences(prefs: NotificationPreferences) {
    assessmentRepository.updateNotificationPreferences(prefs)
  }

  fun readNotificationPreferences(): NotificationPreferences =
    assessmentRepository.readNotificationPreferences()

  fun readAppPreferences(): AppPreferences =
    assessmentRepository.readAppPreferences()

  fun updateAppPreferences(prefs: AppPreferences) {
    assessmentRepository.updateAppPreferences(prefs)
    EquilibrioWidgetUpdater.requestUpdate(appContext)
  }

  fun updateProfileIdentity(name: String, zodiacSign: String, zodiacSymbol: String) {
    assessmentRepository.updateProfileIdentity(name, zodiacSign, zodiacSymbol)
  }

  fun updateMonthlyGoal(goal: String, reason: String = "") =
    assessmentRepository.updateMonthlyGoal(goal, reason)

  // ==========================================
  // Métodos heredados de AssessmentRepository
  // ==========================================
  fun hasCompletedAssessment(): Boolean = assessmentRepository.hasCompletedAssessment()
  fun hasPreviousAssessment(): Boolean = assessmentRepository.hasPreviousAssessment()
  fun saveScores(newScores: Map<Int, Int>) = assessmentRepository.saveScores(newScores)
  fun loadCurrentScores(): Map<Int, Int>? = assessmentRepository.loadCurrentScores()
  fun loadPreviousScores(): Map<Int, Int>? = assessmentRepository.loadPreviousScores()
  fun getSavedReport(): AssessmentReport? = assessmentRepository.getSavedReport()
  fun resetAssessment() = assessmentRepository.resetAssessment()

  fun isProfileCompleted(): Boolean = assessmentRepository.isProfileCompleted()
  fun saveProfile(profile: UserProfile) = assessmentRepository.saveProfile(profile)
  fun loadProfile(): UserProfile = assessmentRepository.loadProfile()

  fun toggleSaveMissionForLater(missionId: String): Boolean =
    assessmentRepository.toggleSaveMissionForLater(missionId)
}
