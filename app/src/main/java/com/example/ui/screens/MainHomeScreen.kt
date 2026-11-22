package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BalanceRepository
import com.example.data.db.MissionEntity
import com.example.domain.CompletionRecord
import com.example.domain.HabitScheduleEvaluator
import com.example.domain.HexagonCalculator
import com.example.domain.WeeklyChallengeProvider
import com.example.model.AssessmentReport
import com.example.model.BalanceMission
import com.example.model.DimensionsProvider
import com.example.model.GamificationStats
import com.example.model.MissionRewardResult
import com.example.model.MissionsProvider
import com.example.model.UserProfile
import com.example.notifications.NotificationScheduler
import com.example.ui.components.LevelUpCelebrationOverlay
import com.example.ui.components.bouncyScale
import com.example.ui.components.formatReadableReflectionDate
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.equilibrioColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainHomeScreen(
  userProfile: UserProfile,
  report: AssessmentReport,
  onOpenReport: () -> Unit,
  onUpdateGoal: (String) -> Unit,
  selectedTab: Int = 2,
  onSelectTab: ((Int) -> Unit)? = null,
  onUpdateGoalWithReason: (String, String) -> Unit = { goal, _ -> onUpdateGoal(goal) },
  onUpdateProfileIdentity: (String, String, String) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier
) {
  var fallbackTab by remember { mutableIntStateOf(selectedTab) } // 0: Misiones, 1: Habilidades, 2: Hoy, 3: Plan, 4: Logros
  val currentTab = if (onSelectTab != null) selectedTab else fallbackTab
  val setTab: (Int) -> Unit = { tab ->
    if (onSelectTab != null) onSelectTab(tab) else fallbackTab = tab
  }
  var showEditGoalDialog by remember { mutableStateOf(false) }
  var editedGoal by remember(userProfile.monthlyGoal) { mutableStateOf(userProfile.monthlyGoal) }
  var goalChangeReason by remember { mutableStateOf("") }

  // "+" FAB Expansion & Ajustes state
  var isFabExpanded by remember { mutableStateOf(false) }
  var showCreateMissionDialog by remember { mutableStateOf(false) }
  var showSettingsScreen by remember { mutableStateOf(false) }
  var pendingHabitToConfirm by remember { mutableStateOf<BalanceMission?>(null) }

  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val balanceRepository = remember { BalanceRepository(context) }

  // Estados reactivos derivados 100% de Room + SharedPreferences
  val liveProfile by balanceRepository.userProfileFlow.collectAsStateWithLifecycle(initialValue = userProfile)
  val syncedProfile = remember(liveProfile, userProfile) {
    liveProfile.copy(
      name = userProfile.name,
      photoUri = userProfile.photoUri,
      zodiacSign = userProfile.zodiacSign,
      zodiacSymbol = userProfile.zodiacSymbol,
      monthlyGoal = userProfile.monthlyGoal
    )
  }

  val realScoresFloat by balanceRepository.realScoresFloat.collectAsStateWithLifecycle(initialValue = emptyMap())
  val realScores by balanceRepository.realScores.collectAsStateWithLifecycle(initialValue = emptyMap())
  val expByDimension14d by balanceRepository.expByDimension14d.collectAsStateWithLifecycle(initialValue = emptyMap())
  val completionsLast14Days by balanceRepository.completionsLast14Days.collectAsStateWithLifecycle(initialValue = emptyList())
  val allCompletions by balanceRepository.allCompletions.collectAsStateWithLifecycle(initialValue = emptyList())
  val actionsByDimension14d by balanceRepository.actionsByDimension14d.collectAsStateWithLifecycle(initialValue = emptyMap())
  val todayCompletions by balanceRepository.todayCompletions.collectAsStateWithLifecycle(initialValue = emptyList())
  val todayCompletedIds by balanceRepository.todayCompletedMissionIds.collectAsStateWithLifecycle(initialValue = emptySet())
  val savedMissionIds by balanceRepository.savedMissionIds.collectAsStateWithLifecycle(initialValue = emptySet())
  val customMissionsEntities by balanceRepository.customMissions.collectAsStateWithLifecycle(initialValue = emptyList())
  val dailyPlanEntries by balanceRepository.todayDailyPlan.collectAsStateWithLifecycle(initialValue = emptyList())
  val currentDayKey by balanceRepository.currentDayKey.collectAsStateWithLifecycle()
  val isFrozenToday by balanceRepository.isFrozenToday.collectAsStateWithLifecycle(initialValue = false)
  val acceptedWeeklyChallengeId by balanceRepository.acceptedWeeklyChallengeId.collectAsStateWithLifecycle(initialValue = null)
  val notificationPrefs by balanceRepository.notificationPreferences.collectAsStateWithLifecycle(
    initialValue = balanceRepository.readNotificationPreferences()
  )
  val appPrefs by balanceRepository.appPreferences.collectAsStateWithLifecycle(
    initialValue = balanceRepository.readAppPreferences()
  )

  var freezeFeedbackMessage by remember { mutableStateOf<String?>(null) }
  var transientToastMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(transientToastMessage) {
    if (transientToastMessage != null) {
      delay(2600L)
      transientToastMessage = null
    }
  }

  LaunchedEffect(currentTab, report.scores, appPrefs.dailyGoalCount) {
    balanceRepository.refreshDayIfNeeded()
    if (currentTab == 2) {
      balanceRepository.ensureDailyPlanForToday(report.scores)
    }
  }

  val liveHarmonyPercentage = remember(realScores, report.scores) {
    HexagonCalculator.calculateLiveHarmonyPercentage(realScores, report.scores)
  }

  // Diálogos de detalle, edición de reflexiones históricas y celebración de subida de nivel
  var selectedDimensionForDetail by remember { mutableStateOf<Int?>(null) }
  var activeDetailMissionId by remember { mutableStateOf<String?>(null) }
  var editingReflectionRecord by remember { mutableStateOf<CompletionRecord?>(null) }
  var levelUpCelebration by remember { mutableStateOf<MissionRewardResult?>(null) }

  fun executeMissionCompletion(mission: BalanceMission) {
    scope.launch {
      val result = balanceRepository.completeMissionWithReward(
        missionId = mission.id,
        dimensionIndex = mission.dimensionIndex,
        expAwarded = mission.expReward,
        countsTowardDailyCap = mission.countsTowardDailyCap
      )
      if (result?.leveledUp == true) {
        levelUpCelebration = result
      }
    }
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
  }

  fun attemptCompleteMission(mission: BalanceMission) {
    val isAlreadyCompleted = todayCompletedIds.contains(mission.id)
    if (mission.isHabit && !mission.isHabitScheduledToday && !isAlreadyCompleted) {
      return
    }
    if (mission.isHabit && !isAlreadyCompleted && appPrefs.confirmHabitCompletion) {
      pendingHabitToConfirm = mission
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    } else {
      executeMissionCompletion(mission)
    }
  }

  // Al pulsar atrás en pestañas secundarias o overlays, regresa limpiamente
  BackHandler(enabled = levelUpCelebration != null) {
    levelUpCelebration = null
  }
  BackHandler(
    enabled = currentTab != 2 &&
      !isFabExpanded &&
      levelUpCelebration == null &&
      !showSettingsScreen &&
      pendingHabitToConfirm == null
  ) {
    setTab(2)
  }
  BackHandler(enabled = isFabExpanded && levelUpCelebration == null && !showSettingsScreen) {
    isFabExpanded = false
  }

  // Misiones combinadas (sugeridas por MissionsProvider + personalizadas de Room),
  // sincronizadas con completadas hoy, guardadas, días de hábito y multiplicador 1.5x.
  val allMissions = remember(
    report.scores,
    realScores,
    customMissionsEntities,
    todayCompletedIds,
    todayCompletions,
    savedMissionIds,
    currentDayKey
  ) {
    val weakDimensionIndices = DimensionsProvider.dimensions
      .sortedBy { report.scores[it.index] ?: 5 }
      .mapIndexedNotNull { rank, dim ->
        val score = report.scores[dim.index] ?: 5
        val realScore = realScores[dim.index] ?: 0
        if ((score <= 6 || rank < 2) && realScore < 8) dim.index else null
      }
      .toSet()

    val base = MissionsProvider.generateMissionsForUser(report.scores)
    val custom = customMissionsEntities.map { entity ->
      val dim = DimensionsProvider.dimensions.getOrNull(entity.dimensionIndex) ?: DimensionsProvider.dimensions[0]
      val isWeak = weakDimensionIndices.contains(entity.dimensionIndex)
      val boostedExp = if (isWeak) (entity.expReward * 1.5).toInt() else entity.expReward

      BalanceMission(
        id = entity.id,
        dimensionIndex = entity.dimensionIndex,
        dimensionTitle = dim.title,
        title = entity.title,
        summary = entity.description.take(70),
        description = entity.description,
        steps = listOf(
          "Paso 1 (Preparación): Define tu espacio y propósito para ${entity.title}.",
          "Paso 2 (Ejecución): Realiza la actividad con presencia durante ${entity.durationLabel}.",
          "Paso 3 (Reflexión): Agradece tu compromiso con tu bienestar integral."
        ),
        benefit = entity.benefit,
        durationLabel = entity.durationLabel,
        durationMinutes = entity.durationMinutes,
        baseExpReward = entity.expReward,
        isWeaknessFocus = isWeak,
        hasMultiplier = isWeak,
        expReward = boostedExp,
        isCompleted = false,
        isSavedForLater = false,
        isCustom = true,
        difficulty = entity.difficulty,
        frequency = entity.frequency,
        isHabit = entity.isHabit,
        habitDays = HabitScheduleEvaluator.parseHabitDays(entity.habitDays),
        habitEndRule = entity.habitEndRule,
        createdAtMillis = entity.createdAtMillis
      ).withHabitSchedule(balanceRepository.dayKey)
    }.filter { !it.isHabitExpired }

    val todayCompletionMap = todayCompletions.associateBy { it.missionId }
    (custom + base).map { m ->
      val todayRecord = todayCompletionMap[m.id]
      m.copy(
        isCompleted = todayCompletedIds.contains(m.id),
        isSavedForLater = savedMissionIds.contains(m.id),
        helpfulRating = todayRecord?.helpfulness,
        reflectionNote = todayRecord?.reflectionNote ?: ""
      )
    }
  }

  val todayCompletionMap = remember(todayCompletions) {
    todayCompletions.associateBy { it.missionId }
  }

  // Reto de la semana: determinista por semana ISO, 45-60 min, +80 EXP fuera del tope diario
  val weeklyChallenge = remember(realScores, report.scores, currentDayKey, todayCompletedIds, todayCompletionMap, savedMissionIds) {
    val baseChallenge = WeeklyChallengeProvider.getChallengeForWeek(
      realScores = realScores,
      perceivedScores = report.scores,
      dayKey = balanceRepository.dayKey
    )
    val todayRecord = todayCompletionMap[baseChallenge.id]
    baseChallenge.copy(
      isCompleted = todayCompletedIds.contains(baseChallenge.id),
      isSavedForLater = savedMissionIds.contains(baseChallenge.id),
      helpfulRating = todayRecord?.helpfulness,
      reflectionNote = todayRecord?.reflectionNote ?: ""
    )
  }

  val isWeeklyChallengeAccepted = remember(acceptedWeeklyChallengeId, weeklyChallenge.id) {
    acceptedWeeklyChallengeId == weeklyChallenge.id
  }

  val acceptedWeeklyChallenge = remember(isWeeklyChallengeAccepted, weeklyChallenge) {
    if (isWeeklyChallengeAccepted) weeklyChallenge else null
  }

  fun resolveMissionById(missionId: String): BalanceMission? {
    if (missionId == weeklyChallenge.id) return weeklyChallenge
    return allMissions.find { it.id == missionId }
      ?: WeeklyChallengeProvider.findById(missionId)
  }

  val currentDetailMission = remember(activeDetailMissionId, editingReflectionRecord, allMissions, weeklyChallenge) {
    val record = editingReflectionRecord
    if (record != null) {
      val resolved = resolveMissionById(record.missionId)
        ?: BalanceMission(
          id = record.missionId,
          dimensionIndex = record.dimensionIndex,
          dimensionTitle = DimensionsProvider.dimensions.getOrNull(record.dimensionIndex)?.title ?: "Equilibrio",
          title = "Acción de equilibrio",
          description = "Reflexión guardada de tu jornada.",
          benefit = "Registrar cómo te sientes ayuda a personalizar tu camino.",
          durationLabel = "10 min"
        )
      resolved.copy(
        isCompleted = true,
        helpfulRating = record.helpfulness,
        reflectionNote = record.reflectionNote
      )
    } else {
      activeDetailMissionId?.let { resolveMissionById(it) }
    }
  }

  val handleFreezeStreak: () -> Unit = {
    scope.launch {
      val result = balanceRepository.freezeStreakToday()
      freezeFeedbackMessage = result.message
      transientToastMessage = result.message
    }
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
  }

  val eqColors = MaterialTheme.equilibrioColors
  val fabPillInteraction = rememberBounceInteractionSource()
  val fabGoalPillInteraction = rememberBounceInteractionSource()

  // Lienzo minimalista adaptable al tema
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(eqColors.canvasBackground)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        AnimatedContent(
          targetState = currentTab,
          transitionSpec = {
            fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
              fadeOut(tween(150, easing = FastOutSlowInEasing))
          },
          label = "home_tab_transition"
        ) { tab ->
          when (tab) {
            0 -> {
              // ==========================================
              // APARTADO DE MISIONES (CATEGORÍAS Y DETALLES)
              // ==========================================
              val gamificationStats = GamificationStats(
                totalExp = syncedProfile.exp,
                expToday = syncedProfile.expToday,
                completedMissionsCount = syncedProfile.completedMissions,
                activeStreakDays = syncedProfile.activeStreakDays,
                completedMissionIds = todayCompletedIds,
                savedMissionIds = savedMissionIds
              )
              MissionsSection(
                report = report,
                missions = allMissions,
                gamificationStats = gamificationStats,
                onViewMissionDetails = { mission ->
                  editingReflectionRecord = null
                  activeDetailMissionId = mission.id
                },
                onOpenReport = onOpenReport,
                onOpenCreateMission = { showCreateMissionDialog = true },
                weeklyChallenge = weeklyChallenge,
                isWeeklyChallengeAccepted = isWeeklyChallengeAccepted,
                onAcceptWeeklyChallenge = { challenge ->
                  balanceRepository.acceptWeeklyChallenge(challenge)
                  transientToastMessage = "Reto de la semana añadido a Hoy y Plan"
                }
              )
            }

            1 -> {
              // ==========================================
              // APARTADO DE HABILIDADES (REDISEÑADO Y SINCRONIZADO)
              // ==========================================
              HabilidadesSection(
                report = report,
                realScores = realScores,
                realScoresFloat = realScoresFloat,
                expByDimension14d = expByDimension14d,
                liveHarmonyPercentage = liveHarmonyPercentage,
                allMissions = allMissions,
                onLaunchMission = { mission ->
                  editingReflectionRecord = null
                  activeDetailMissionId = mission.id
                },
                onOpenCreateMission = {
                  showCreateMissionDialog = true
                },
                onOpenFullReport = onOpenReport
              )
            }

            2 -> {
              // ==========================================
              // APARTADO HOY (HEXÁGONO DINÁMICO DE 2 CAPAS)
              // ==========================================
              HoySection(
                userProfile = syncedProfile,
                report = report,
                realScores = realScores,
                realScoresFloat = realScoresFloat,
                todayCompletedIds = todayCompletedIds,
                allMissions = allMissions,
                selectedDimensionForDetail = selectedDimensionForDetail,
                onSelectDimension = { selectedDimensionForDetail = it },
                onToggleCompleteMission = { mission ->
                  attemptCompleteMission(mission)
                },
                onViewMissionDetails = { mission ->
                  editingReflectionRecord = null
                  activeDetailMissionId = mission.id
                },
                liveHarmonyPercentage = liveHarmonyPercentage,
                onOpenFullReport = onOpenReport,
                onEditGoal = {
                  editedGoal = syncedProfile.monthlyGoal
                  goalChangeReason = ""
                  showEditGoalDialog = true
                },
                onOpenSettings = {
                  isFabExpanded = false
                  showSettingsScreen = true
                },
                dailyGoalCount = appPrefs.dailyGoalCount,
                dailyPlanEntries = dailyPlanEntries,
                onSwapDailyMission = { slotIndex ->
                  scope.launch {
                    balanceRepository.swapDailyMission(slotIndex, report.scores)
                  }
                },
                onQuickRateMission = { mission, rating ->
                  scope.launch {
                    balanceRepository.saveReflectionFeedback(
                      missionId = mission.id,
                      dimensionIndex = mission.dimensionIndex,
                      rating = rating,
                      reflectionNote = mission.reflectionNote,
                      expAwarded = mission.expReward,
                      countsTowardDailyCap = mission.countsTowardDailyCap
                    )
                  }
                },
                acceptedWeeklyChallenge = acceptedWeeklyChallenge
              )
            }

            3 -> {
              // ==========================================
              // APARTADO PLAN (SEMANA ACTIVA, RECORDATORIOS Y REFLEXIONES)
              // ==========================================
              PlanSection(
                userProfile = syncedProfile,
                report = report,
                realScores = realScores,
                completionsLast14Days = completionsLast14Days,
                allMissions = allMissions,
                onViewMissionDetails = { mission ->
                  editingReflectionRecord = null
                  activeDetailMissionId = mission.id
                },
                onOpenCreateMission = {
                  showCreateMissionDialog = true
                },
                onEditGoal = {
                  editedGoal = syncedProfile.monthlyGoal
                  goalChangeReason = ""
                  showEditGoalDialog = true
                },
                allCompletions = allCompletions,
                acceptedWeeklyChallenge = acceptedWeeklyChallenge,
                notificationPrefs = notificationPrefs,
                firstDayOfWeek = appPrefs.firstDayOfWeek,
                isFrozenToday = isFrozenToday,
                freezeMessage = freezeFeedbackMessage,
                onUpdateNotificationPrefs = { updatedPrefs ->
                  balanceRepository.updateNotificationPreferences(updatedPrefs)
                  NotificationScheduler.syncSchedules(context, updatedPrefs)
                },
                onFreezeStreak = handleFreezeStreak,
                onEditReflection = { record ->
                  activeDetailMissionId = null
                  editingReflectionRecord = record
                },
                onResolveMissionById = { missionId ->
                  resolveMissionById(missionId)
                }
              )
            }

            else -> {
              // ==========================================
              // APARTADO LOGROS (NIVEL, CONGELAR RACHA E INSIGNIAS)
              // ==========================================
              LogrosSection(
                userProfile = syncedProfile,
                report = report,
                realScores = realScores,
                expByDimension14d = expByDimension14d,
                liveHarmonyPercentage = liveHarmonyPercentage,
                completionsCount = syncedProfile.completedMissions,
                customMissionsCount = allMissions.count { it.isCustom },
                onOpenFullReport = onOpenReport,
                onNavigateToMissions = { setTab(0) },
                isFrozenToday = isFrozenToday,
                freezeMessage = freezeFeedbackMessage,
                onFreezeStreak = handleFreezeStreak
              )
            }
          }
        }
      }

      // ==========================================
      // BARRA DE NAVEGACIÓN CENTRADA CON REBOTE TÁCTIL
      // ==========================================
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          shape = RoundedCornerShape(32.dp),
          color = eqColors.heroSurface,
          border = if (eqColors.heroBorder != Color.Transparent) BorderStroke(1.dp, eqColors.heroBorder) else null,
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            NavBottomItem(
              icon = Icons.Outlined.TaskAlt,
              label = "Misiones",
              isSelected = currentTab == 0,
              modifier = Modifier.weight(1f),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFabExpanded = false
                setTab(0)
              }
            )

            NavBottomItem(
              icon = Icons.Outlined.Hexagon,
              label = "Habilidades",
              isSelected = currentTab == 1,
              modifier = Modifier.weight(1f),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFabExpanded = false
                setTab(1)
              }
            )

            NavBottomItem(
              icon = Icons.Outlined.Person,
              label = "Hoy",
              isSelected = currentTab == 2,
              modifier = Modifier.weight(1f),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFabExpanded = false
                setTab(2)
              }
            )

            NavBottomItem(
              icon = Icons.Outlined.CalendarMonth,
              label = "Plan",
              isSelected = currentTab == 3,
              modifier = Modifier.weight(1f),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFabExpanded = false
                setTab(3)
              }
            )

            NavBottomItem(
              icon = Icons.Outlined.EmojiEvents,
              label = "Logros",
              isSelected = currentTab == 4,
              modifier = Modifier.weight(1f),
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isFabExpanded = false
                setTab(4)
              }
            )
          }
        }
      }
    }

    // ==========================================
    // FLOATING ACTION BUTTON (+) CON PÍLDORAS DESPLEGABLES
    // ==========================================
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .navigationBarsPadding()
        .padding(bottom = 104.dp, end = 20.dp),
      horizontalAlignment = Alignment.End
    ) {
      AnimatedVisibility(
        visible = isFabExpanded,
        enter = fadeIn(tween(200, delayMillis = 40)) +
          scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) +
          slideInVertically { it / 2 },
        exit = fadeOut(tween(150)) + scaleOut() + slideOutVertically { it / 2 }
      ) {
        Surface(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isFabExpanded = false
            editedGoal = syncedProfile.monthlyGoal
            goalChangeReason = ""
            showEditGoalDialog = true
          },
          interactionSource = fabGoalPillInteraction,
          shape = RoundedCornerShape(26.dp),
          color = eqColors.surfaceCard,
          shadowElevation = 8.dp,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier
            .padding(bottom = 10.dp)
            .bouncyScale(fabGoalPillInteraction, scaleDown = 0.92f)
            .testTag("fab_new_monthly_goal_pill")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Flag,
              contentDescription = null,
              tint = eqColors.accentPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Nueva meta del mes",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              color = eqColors.textPrimary
            )
          }
        }
      }

      AnimatedVisibility(
        visible = isFabExpanded,
        enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + slideInVertically { it / 2 },
        exit = fadeOut() + scaleOut() + slideOutVertically { it / 2 }
      ) {
        Surface(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isFabExpanded = false
            showCreateMissionDialog = true
          },
          interactionSource = fabPillInteraction,
          shape = RoundedCornerShape(26.dp),
          color = eqColors.heroSurface,
          shadowElevation = 8.dp,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier
            .padding(bottom = 12.dp)
            .bouncyScale(fabPillInteraction, scaleDown = 0.92f)
            .testTag("fab_mission_pill")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Checklist,
              contentDescription = null,
              tint = eqColors.onHeroSurface,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Misión",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              color = eqColors.onHeroSurface
            )
          }
        }
      }

      val fabInteractionSource = remember { MutableInteractionSource() }
      val isFabPressed by fabInteractionSource.collectIsPressedAsState()
      val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.88f else 1f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow
        ),
        label = "fab_bounce"
      )

      val rotationDegree by animateFloatAsState(
        targetValue = if (isFabExpanded) 45f else 0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
      )

      FloatingActionButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          isFabExpanded = !isFabExpanded
        },
        interactionSource = fabInteractionSource,
        containerColor = eqColors.heroSurface,
        contentColor = eqColors.onHeroSurface,
        shape = CircleShape,
        modifier = Modifier
          .size(54.dp)
          .graphicsLayer {
            scaleX = fabScale
            scaleY = fabScale
          }
          .shadow(6.dp, CircleShape)
          .testTag("fab_add_mission")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = if (isFabExpanded) "Cerrar" else "Añadir misión",
          modifier = Modifier
            .size(26.dp)
            .rotate(rotationDegree)
        )
      }
    }

    // Pantalla de Ajustes (6 secciones) abierta al tocar el botón del perfil
    AnimatedVisibility(
      visible = showSettingsScreen,
      enter = slideInVertically(
        animationSpec = spring(
          dampingRatio = 0.85f,
          stiffness = Spring.StiffnessMediumLow
        )
      ) { it / 4 } + fadeIn(tween(220)),
      exit = slideOutVertically(tween(180)) { it / 4 } + fadeOut(tween(160))
    ) {
      SettingsScreen(
        userProfile = syncedProfile,
        appPreferences = appPrefs,
        notificationPreferences = notificationPrefs,
        isFrozenToday = isFrozenToday,
        freezeMessage = freezeFeedbackMessage,
        onBack = { showSettingsScreen = false },
        onUpdateAppPreferences = { updatedPrefs ->
          val dailyCountChanged = updatedPrefs.dailyGoalCount != appPrefs.dailyGoalCount
          balanceRepository.updateAppPreferences(updatedPrefs)
          if (dailyCountChanged) {
            scope.launch {
              balanceRepository.ensureDailyPlanForToday(report.scores)
            }
          }
        },
        onUpdateNotificationPreferences = { updatedPrefs ->
          balanceRepository.updateNotificationPreferences(updatedPrefs)
          NotificationScheduler.syncSchedules(context, updatedPrefs)
        },
        onFreezeStreak = handleFreezeStreak,
        onUpdateProfileIdentity = { name, zodiacSign, zodiacSymbol ->
          onUpdateProfileIdentity(name, zodiacSign, zodiacSymbol)
        },
        onOpenEditMonthlyGoal = {
          editedGoal = syncedProfile.monthlyGoal
          goalChangeReason = ""
          showEditGoalDialog = true
        },
        onShowFeedback = { message ->
          transientToastMessage = message
        }
      )
    }

    // Banner sereno de confirmación (ej. "Racha protegida. Mañana te esperamos")
    AnimatedVisibility(
      visible = transientToastMessage != null,
      enter = fadeIn(tween(220)) + slideInVertically { -it },
      exit = fadeOut(tween(180)) + slideOutVertically { -it },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 10.dp, start = 20.dp, end = 20.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = eqColors.heroSurface,
        border = if (eqColors.heroBorder != Color.Transparent) BorderStroke(1.dp, eqColors.heroBorder) else null,
        shadowElevation = 6.dp,
        modifier = Modifier.testTag("equilibrio_toast_banner")
      ) {
        Text(
          text = transientToastMessage.orEmpty(),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = eqColors.onHeroSurface,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
      }
    }

    // Celebración de subida de nivel a pantalla completa (2 segundos)
    if (levelUpCelebration != null) {
      LevelUpCelebrationOverlay(
        reward = levelUpCelebration!!,
        onDismiss = { levelUpCelebration = null }
      )
    }
  }

  // Diálogo para Crear Misión y Hábito (persistido en Room)
  if (showCreateMissionDialog) {
    CreateMissionDialog(
      onDismiss = { showCreateMissionDialog = false },
      onMissionCreated = { newMission ->
        scope.launch {
          balanceRepository.insertCustomMission(
            MissionEntity(
              id = newMission.id,
              dimensionIndex = newMission.dimensionIndex,
              title = newMission.title,
              description = newMission.description,
              benefit = newMission.benefit,
              durationLabel = newMission.durationLabel,
              durationMinutes = newMission.durationMinutes,
              frequency = newMission.frequency,
              isHabit = newMission.isHabit,
              expReward = newMission.expReward,
              difficulty = newMission.difficulty,
              habitDays = HabitScheduleEvaluator.serializeHabitDays(newMission.habitDays),
              habitEndRule = newMission.habitEndRule,
              createdAtMillis = newMission.createdAtMillis
            )
          )
          balanceRepository.ensureDailyPlanForToday(report.scores)
        }
        showCreateMissionDialog = false
        setTab(if (newMission.isHabit) 3 else 0)
      }
    )
  }

  // Diálogo de detalle de dimensión al tocar el hexágono
  if (selectedDimensionForDetail != null) {
    val dimIdx = selectedDimensionForDetail!!
    DimensionDetailDialog(
      dimensionIndex = dimIdx,
      report = report,
      realScore = realScores[dimIdx] ?: 0,
      actionsLast14d = actionsByDimension14d[dimIdx] ?: 0,
      isChosenFocus = dimIdx in syncedProfile.chosenFocusDimensions,
      onToggleFocus = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        balanceRepository.toggleFocusDimension(dimIdx)
      },
      onDismiss = { selectedDimensionForDetail = null }
    )
  }

  // Modal de detalles de la misión (completar, valorar, editar reflexión histórica o guardar)
  if (currentDetailMission != null) {
    val activeRecord = editingReflectionRecord
    val modalDateLabel = remember(activeRecord) {
      if (activeRecord != null) {
        formatReadableReflectionDate(activeRecord.date, balanceRepository.dayKey)
      } else {
        "Hoy"
      }
    }
    MissionDetailModal(
      mission = currentDetailMission,
      stats = GamificationStats(
        totalExp = syncedProfile.exp,
        expToday = syncedProfile.expToday,
        completedMissionsCount = syncedProfile.completedMissions,
        activeStreakDays = syncedProfile.activeStreakDays,
        completedMissionIds = todayCompletedIds,
        savedMissionIds = savedMissionIds
      ),
      initialDateLabel = modalDateLabel,
      onDismiss = {
        activeDetailMissionId = null
        editingReflectionRecord = null
      },
      onMarkCompleted = {
        attemptCompleteMission(currentDetailMission)
      },
      onUnmarkCompleted = {
        val missionId = currentDetailMission.id
        val targetDate = activeRecord?.date ?: balanceRepository.dayKey.today()
        activeDetailMissionId = null
        editingReflectionRecord = null
        scope.launch {
          balanceRepository.deleteCompletion(missionId, date = targetDate)
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      },
      onSaveReflection = { rating, note ->
        val missionId = currentDetailMission.id
        val dimIndex = currentDetailMission.dimensionIndex
        val exp = currentDetailMission.expReward
        val countsCap = currentDetailMission.countsTowardDailyCap
        val targetDate = activeRecord?.date ?: balanceRepository.dayKey.today()
        activeDetailMissionId = null
        editingReflectionRecord = null
        scope.launch {
          balanceRepository.saveReflectionFeedback(
            missionId = missionId,
            dimensionIndex = dimIndex,
            rating = rating,
            reflectionNote = note,
            date = targetDate,
            expAwarded = exp,
            countsTowardDailyCap = countsCap
          )
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      },
      onSaveForLater = {
        val missionId = currentDetailMission.id
        activeDetailMissionId = null
        editingReflectionRecord = null
        balanceRepository.toggleSaveMissionForLater(missionId)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
    )
  }

  // Diálogo de confirmación antes de completar un hábito (activo por defecto en Ajustes -> Hábitos)
  if (pendingHabitToConfirm != null) {
    val habit = pendingHabitToConfirm!!
    val confirmHabitInteraction = rememberBounceInteractionSource()
    val cancelHabitInteraction = rememberBounceInteractionSource()
    AlertDialog(
      onDismissRequest = { pendingHabitToConfirm = null },
      containerColor = eqColors.surfaceCard,
      shape = RoundedCornerShape(26.dp),
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(eqColors.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      },
      title = {
        Text(
          text = "¿Completaste este hábito?",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Vas a marcar «${habit.title}» como completado hoy.",
            style = MaterialTheme.typography.bodyMedium,
            color = eqColors.textPrimary
          )
          Text(
            text = "Puedes desactivar esta confirmación en Ajustes > Hábitos.",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            pendingHabitToConfirm = null
            executeMissionCompletion(habit)
          },
          interactionSource = confirmHabitInteraction,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = eqColors.accentPrimary,
            contentColor = eqColors.onAccentPrimary
          ),
          modifier = Modifier
            .bouncyScale(confirmHabitInteraction, scaleDown = 0.92f)
            .testTag("btn_confirm_habit_yes")
        ) {
          Text("Confirmar", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { pendingHabitToConfirm = null },
          interactionSource = cancelHabitInteraction,
          modifier = Modifier
            .bouncyScale(cancelHabitInteraction)
            .testTag("btn_confirm_habit_no")
        ) {
          Text("Cancelar", color = eqColors.textSecondary)
        }
      },
      modifier = Modifier.testTag("dialog_confirm_habit")
    )
  }

  // Diálogo para editar o crear "Nueva meta del mes" preguntando por qué decidió cambiarla
  if (showEditGoalDialog) {
    val saveGoalInteraction = rememberBounceInteractionSource()
    val cancelGoalInteraction = rememberBounceInteractionSource()
    val quickReasons = listOf(
      "Cambió mi prioridad",
      "Hacerla más realista",
      "Ya logré la anterior"
    )
    AlertDialog(
      onDismissRequest = { showEditGoalDialog = false },
      containerColor = eqColors.surfaceCard,
      shape = RoundedCornerShape(26.dp),
      icon = {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(eqColors.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(22.dp)
          )
        }
      },
      title = {
        Text(
          text = "Nueva meta del mes",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Ajusta tu propósito mensual y cuéntanos por qué decidiste cambiarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = eqColors.textSecondary
          )
          OutlinedTextField(
            value = editedGoal,
            onValueChange = { editedGoal = it },
            label = { Text("Meta del mes") },
            placeholder = { Text("¿Cuál es tu nuevo propósito?") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = eqColors.accentPrimary,
              unfocusedBorderColor = eqColors.border
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_monthly_goal")
          )
          OutlinedTextField(
            value = goalChangeReason,
            onValueChange = { goalChangeReason = it },
            label = { Text("¿Por qué decidiste cambiarla?") },
            placeholder = { Text("Ej. Quiero priorizar mi descanso o ajustar el ritmo") },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = eqColors.accentPrimary,
              unfocusedBorderColor = eqColors.border
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_monthly_goal_reason")
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quickReasons.forEach { reasonChip ->
              val isChosen = goalChangeReason.equals(reasonChip, ignoreCase = true)
              val chipInteraction = rememberBounceInteractionSource()
              Surface(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  goalChangeReason = reasonChip
                },
                interactionSource = chipInteraction,
                shape = RoundedCornerShape(12.dp),
                color = if (isChosen) eqColors.accentPrimary else eqColors.surfaceVariant,
                border = BorderStroke(1.dp, if (isChosen) eqColors.accentPrimary else eqColors.border),
                modifier = Modifier.bouncyScale(chipInteraction, scaleDown = 0.92f)
              ) {
                Text(
                  text = reasonChip,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isChosen) eqColors.onAccentPrimary else eqColors.textPrimary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editedGoal.isNotBlank()) {
              onUpdateGoalWithReason(editedGoal.trim(), goalChangeReason.trim())
              transientToastMessage = "Meta del mes actualizada"
            }
            showEditGoalDialog = false
          },
          enabled = editedGoal.isNotBlank(),
          interactionSource = saveGoalInteraction,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = eqColors.accentPrimary,
            contentColor = eqColors.onAccentPrimary
          ),
          modifier = Modifier
            .bouncyScale(saveGoalInteraction, scaleDown = 0.92f)
            .testTag("btn_save_monthly_goal")
        ) {
          Text("Guardar", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showEditGoalDialog = false },
          interactionSource = cancelGoalInteraction,
          modifier = Modifier.bouncyScale(cancelGoalInteraction)
        ) {
          Text("Cancelar", color = eqColors.textSecondary)
        }
      }
    )
  }
}

@Composable
private fun NavBottomItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val interactionSource = rememberBounceInteractionSource()
  val contentColor by animateColorAsState(
    targetValue = if (isSelected) eqColors.onHeroSurface else eqColors.onHeroSurface.copy(alpha = 0.50f),
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "nav_color_$label"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .height(52.dp)
      .bouncyScale(interactionSource, scaleDown = 0.90f)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 2.dp, vertical = 2.dp)
      .testTag("nav_tab_${label.lowercase()}")
  ) {
    Box(
      modifier = Modifier.size(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.5.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      ),
      color = contentColor,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(3.dp))
    Box(
      modifier = Modifier
        .size(3.5.dp)
        .clip(CircleShape)
        .background(if (isSelected) eqColors.onHeroSurface else Color.Transparent)
    )
  }
}
