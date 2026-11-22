package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.DailyPlanEntity
import com.example.domain.HelpfulnessRating
import com.example.domain.HexagonCalculator
import com.example.model.AssessmentReport
import com.example.model.BalanceMission
import com.example.model.DimensionsProvider
import com.example.model.GamificationStats
import com.example.model.UserProfile
import com.example.ui.components.DualLayerHexagonRadarChart
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.equilibrioColors

private data class CalmPhrase(
  val quote: String,
  val author: String
)

/**
 * Pantalla principal "Hoy" (tab central):
 * Diseño minimalista, sin scroll largo, guiado por frases inspiradoras,
 * hexágono dinámico de dos capas interactivo y mazo compacto de misión prioritaria.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun HoySection(
  userProfile: UserProfile,
  report: AssessmentReport,
  realScores: Map<Int, Int>,
  realScoresFloat: Map<Int, Float>,
  todayCompletedIds: Set<String>,
  allMissions: List<BalanceMission>,
  selectedDimensionForDetail: Int?,
  onSelectDimension: (Int) -> Unit,
  onToggleCompleteMission: (BalanceMission) -> Unit,
  onViewMissionDetails: (BalanceMission) -> Unit,
  liveHarmonyPercentage: Int,
  onOpenFullReport: () -> Unit,
  onEditGoal: () -> Unit,
  onOpenSettings: () -> Unit = onEditGoal,
  dailyGoalCount: Int = 3,
  dailyPlanEntries: List<DailyPlanEntity> = emptyList(),
  onSwapDailyMission: (Int) -> Unit = {},
  onQuickRateMission: (BalanceMission, HelpfulnessRating) -> Unit = { _, _ -> },
  acceptedWeeklyChallenge: BalanceMission? = null,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val dimensions = DimensionsProvider.dimensions
  val chosenFocus = userProfile.chosenFocusDimensions

  val priorityDimIndex = remember(realScores, report.scores, chosenFocus) {
    HexagonCalculator.findPriorityDimension(realScores, report.scores, chosenFocus)
  }
  val priorityDim = dimensions.getOrNull(priorityDimIndex) ?: dimensions[0]
  val hasRealData = remember(realScoresFloat) { realScoresFloat.values.any { it > 0.05f } }

  val animatedHarmony by animateIntAsState(
    targetValue = liveHarmonyPercentage,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "hoy_animated_harmony"
  )

  val missionsById = remember(allMissions) { allMissions.associateBy { it.id } }
  val dailyPlanIds = remember(dailyPlanEntries) { dailyPlanEntries.map { it.missionId }.toSet() }
  val allDailyCompleted = remember(dailyPlanEntries, todayCompletedIds) {
    dailyPlanEntries.isNotEmpty() && dailyPlanEntries.all { it.missionId in todayCompletedIds }
  }

  // Mazo compacto de hasta 4 acciones sugeridas para hoy (evita scroll infinito)
  val spotlightMissions = remember(allMissions, priorityDimIndex, todayCompletedIds, dailyPlanIds) {
    val prioritized = allMissions.sortedWith(
      compareBy<BalanceMission> { it.isCompleted }
        .thenBy { it.isHabit && !it.isHabitScheduledToday }
        .thenBy { it.id in dailyPlanIds }
        .thenByDescending { it.dimensionIndex == priorityDimIndex }
        .thenByDescending { it.hasMultiplier }
        .thenByDescending { it.isSavedForLater }
    )
    prioritized.take(4)
  }

  var deckIndex by remember { mutableIntStateOf(0) }
  var slideDirection by remember { mutableIntStateOf(1) }
  val safeDeckIndex = if (spotlightMissions.isNotEmpty()) {
    deckIndex.coerceIn(0, spotlightMissions.lastIndex)
  } else 0
  val activeMission = spotlightMissions.getOrNull(safeDeckIndex)

  val completedTodayCount = remember(allMissions, todayCompletedIds) {
    allMissions.count { todayCompletedIds.contains(it.id) }
  }

  var phraseIndex by remember { mutableIntStateOf(0) }
  var phraseRefreshCount by remember { mutableIntStateOf(0) }
  val phrases = remember(report.wisdomQuote, report.wisdomAuthor, priorityDim.title, userProfile.monthlyGoal) {
    listOf(
      CalmPhrase(
        quote = "Foco sugerido hoy: ${priorityDim.title}. Un paso breve suma a tu hexágono.",
        author = "Tu equilibrio hoy"
      ),
      CalmPhrase(
        quote = report.wisdomQuote,
        author = report.wisdomAuthor
      ),
      CalmPhrase(
        quote = userProfile.monthlyGoal.ifBlank { "Paso a paso se construye la armonía diaria." },
        author = "Tu meta del mes"
      ),
      CalmPhrase(
        quote = "La constancia tranquila vence a cualquier esfuerzo extremo de un solo día.",
        author = "Equilibrio"
      )
    )
  }
  val currentPhrase = phrases[phraseIndex % phrases.size]

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp)
      .padding(top = 10.dp, bottom = 104.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Encabezado compacto con saludo, racha/EXP y avatar (tocar abre Ajustes)
      HoyCompactHeader(
        userProfile = userProfile,
        completedTodayCount = completedTodayCount,
        onOpenSettings = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onOpenSettings()
        },
        onOpenReport = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onOpenFullReport()
        }
      )

      // 2. Frase guía interactiva (sin recargar la pantalla)
      CalmPhraseBanner(
        phrase = currentPhrase,
        accentColor = priorityDim.accentColor,
        refreshSpinCount = phraseRefreshCount,
        onNextPhrase = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          phraseIndex = (phraseIndex + 1) % phrases.size
          phraseRefreshCount++
        }
      )

      // 3. Tarjeta del Hexágono Dinámico de 2 capas (vértices táctiles -> DimensionDetailDialog)
      val eqColors = MaterialTheme.equilibrioColors
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = eqColors.surfaceCard,
        border = BorderStroke(1.dp, eqColors.border),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tu Hexágono en Vivo",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.textPrimary
              )
              Text(
                text = if (hasRealData) {
                  "Toca un vértice para ver detalle real vs base"
                } else {
                  "Completa misiones para expandir tu huella real"
                },
                style = MaterialTheme.typography.labelSmall,
                color = eqColors.textSecondary
              )
            }

            val reportInteraction = rememberBounceInteractionSource()
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenFullReport()
              },
              interactionSource = reportInteraction,
              shape = RoundedCornerShape(12.dp),
              color = eqColors.surfaceVariant,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .bouncyScale(reportInteraction, scaleDown = 0.93f)
                .testTag("btn_hoy_open_report")
            ) {
              Text(
                text = "$animatedHarmony% · Reporte",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontFeatureSettings = "tnum"
                ),
                color = eqColors.accentPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          DualLayerHexagonRadarChart(
            perceivedScores = report.scores,
            realScores = realScoresFloat,
            selectedDimensionIndex = selectedDimensionForDetail,
            onDimensionSelect = { idx ->
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSelectDimension(idx)
            },
            chartSize = 204.dp,
            showLegend = true,
            showChips = false
          )
        }
      }

      // 4. Plan Diario Conectado: "Tus X para hoy" ARRIBA de las misiones actuales de Hoy
      if (dailyPlanEntries.isNotEmpty()) {
        DailyPlanTodaySection(
          entries = dailyPlanEntries,
          dailyGoalCount = dailyGoalCount,
          missionsById = missionsById,
          todayCompletedIds = todayCompletedIds,
          allCompleted = allDailyCompleted,
          streakDays = userProfile.activeStreakDays,
          onToggleComplete = { mission ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleCompleteMission(mission)
          },
          onViewDetails = { mission ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onViewMissionDetails(mission)
          },
          onSwapSlot = { slotIndex ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSwapDailyMission(slotIndex)
          },
          onQuickRate = { mission, rating ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onQuickRateMission(mission, rating)
          }
        )
      }

      // 4b. Reto de la semana aceptado por el usuario (añadido a Hoy y a Plan)
      if (acceptedWeeklyChallenge != null) {
        AcceptedWeeklyChallengeHoyCard(
          challenge = acceptedWeeklyChallenge,
          isCompleted = todayCompletedIds.contains(acceptedWeeklyChallenge.id),
          onToggleComplete = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleCompleteMission(acceptedWeeklyChallenge)
          },
          onViewDetails = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onViewMissionDetails(acceptedWeeklyChallenge)
          },
          onQuickRate = { rating ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onQuickRateMission(acceptedWeeklyChallenge, rating)
          }
        )
      }

      // 5. Mazo interactivo de Misión del Momento (1 tarjeta a la vez, cero scroll largo)
      if (activeMission != null) {
        val isCompleted = todayCompletedIds.contains(activeMission.id)
        HoySpotlightDeckCard(
          mission = activeMission,
          isCompletedToday = isCompleted,
          positionIndex = safeDeckIndex,
          totalCount = spotlightMissions.size,
          slideDirection = slideDirection,
          onPrevious = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            slideDirection = -1
            deckIndex = if (safeDeckIndex - 1 < 0) spotlightMissions.lastIndex else safeDeckIndex - 1
          },
          onNext = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            slideDirection = 1
            deckIndex = (safeDeckIndex + 1) % spotlightMissions.size
          },
          onViewDetails = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onViewMissionDetails(activeMission)
          },
          onQuickRate = { rating ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onQuickRateMission(activeMission, rating)
          }
        )
      }
    }
  }
}

@Composable
private fun HoyCompactHeader(
  userProfile: UserProfile,
  completedTodayCount: Int,
  onOpenSettings: () -> Unit,
  onOpenReport: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val avatarInteraction = rememberBounceInteractionSource()
  val statsInteraction = rememberBounceInteractionSource()

  val expTodayFraction = (userProfile.expToday.toFloat() / GamificationStats.DAILY_EXP_CAP.toFloat()).coerceIn(0f, 1f)
  val animatedExpProgress by animateFloatAsState(
    targetValue = expTodayFraction,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "hoy_avatar_exp_ring_progress"
  )
  val isDailyCapReached = userProfile.expToday >= GamificationStats.DAILY_EXP_CAP

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = if (userProfile.name.isNotBlank()) "Hola, ${userProfile.name}" else "Hola",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          letterSpacing = (-0.5).sp
        ),
        color = eqColors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = when {
          isDailyCapReached ->
            "Objetivo diario cumplido"
          completedTodayCount > 0 ->
            "$completedTodayCount ${if (completedTodayCount == 1) "misión" else "misiones"} completadas hoy · Nv. ${userProfile.level}"
          else ->
            "Nivel ${userProfile.level} (${userProfile.rank}) · Un paso a la vez"
        },
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = if (isDailyCapReached) FontWeight.SemiBold else FontWeight.Medium
        ),
        color = if (isDailyCapReached) eqColors.successText else eqColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("hoy_header_subtitle")
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        onClick = onOpenReport,
        interactionSource = statsInteraction,
        shape = RoundedCornerShape(14.dp),
        color = eqColors.surfaceCard,
        border = BorderStroke(1.dp, eqColors.border),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .bouncyScale(statsInteraction, scaleDown = 0.93f)
          .testTag("btn_hoy_streak_exp")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = "Racha",
            tint = if (userProfile.activeStreakDays > 0) eqColors.streakFlame else eqColors.textTertiary,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "${userProfile.activeStreakDays}d",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textPrimary
          )
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(12.dp)
              .background(eqColors.border)
          )
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "EXP de hoy",
            tint = if (isDailyCapReached) eqColors.successText else eqColors.accentPrimary,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = "${userProfile.expToday}/${GamificationStats.DAILY_EXP_CAP}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontFeatureSettings = "tnum"
            ),
            color = if (isDailyCapReached) eqColors.successText else eqColors.textSecondary
          )
        }
      }

      // Avatar envuelto en aro animado de progreso de EXP diario -> Abre Ajustes
      val ringTrackColor = eqColors.surfaceVariant
      val ringActiveColor = if (isDailyCapReached) eqColors.successText else eqColors.accentPrimary
      Surface(
        onClick = onOpenSettings,
        interactionSource = avatarInteraction,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
          .size(54.dp)
          .bouncyScale(avatarInteraction, scaleDown = 0.90f)
          .testTag("btn_hoy_avatar_goal")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .padding(2.dp)
              .testTag("hoy_avatar_exp_ring")
          ) {
            val strokePx = 3.dp.toPx()
            drawArc(
              color = ringTrackColor,
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedExpProgress > 0f) {
              drawArc(
                color = ringActiveColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedExpProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = eqColors.surfaceVariant,
            border = BorderStroke(1.dp, eqColors.border),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (userProfile.photoUri != null) {
                AsyncImage(
                  model = Uri.parse(userProfile.photoUri),
                  contentDescription = "Abrir ajustes y perfil",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.Person,
                  contentDescription = "Abrir ajustes y perfil",
                  tint = eqColors.accentPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DailyPlanTodaySection(
  entries: List<DailyPlanEntity>,
  dailyGoalCount: Int,
  missionsById: Map<String, BalanceMission>,
  todayCompletedIds: Set<String>,
  allCompleted: Boolean,
  streakDays: Int,
  onToggleComplete: (BalanceMission) -> Unit,
  onViewDetails: (BalanceMission) -> Unit,
  onSwapSlot: (Int) -> Unit,
  onQuickRate: (BalanceMission, HelpfulnessRating) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val completedCount = entries.count { it.missionId in todayCompletedIds }
  val headerGoalNumber = if (entries.isNotEmpty()) entries.size else dailyGoalCount

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("section_daily_plan_today"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Tus $headerGoalNumber para hoy",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          ),
          color = eqColors.textPrimary
        )
        Text(
          text = "Elegidas según tu ritmo · cámbialas cuando quieras sin penalización",
          style = MaterialTheme.typography.labelSmall,
          color = eqColors.textSecondary
        )
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (allCompleted) eqColors.successBadge else eqColors.surfaceVariant
      ) {
        Text(
          text = "$completedCount / ${entries.size}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum"
          ),
          color = if (allCompleted) eqColors.successText else eqColors.accentPrimary,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
      }
    }

    AnimatedVisibility(
      visible = allCompleted,
      enter = fadeIn(tween(260)) + expandVertically(),
      exit = fadeOut(tween(180)) + shrinkVertically()
    ) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = eqColors.successContainer,
        border = BorderStroke(1.dp, eqColors.successBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("banner_day_completed")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = eqColors.successText,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Día completado",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.successStrongText
              )
              Text(
                text = if (entries.size == 1) {
                  "Cuidaste tu acción esencial de hoy"
                } else {
                  "Cuidaste tus ${entries.size} acciones esenciales de hoy"
                },
                style = MaterialTheme.typography.labelSmall,
                color = eqColors.successBodyText
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = eqColors.surfaceCard,
            border = BorderStroke(1.dp, eqColors.successBorder)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = "Racha",
                tint = eqColors.streakFlame,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${streakDays}d racha",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.successStrongText
              )
            }
          }
        }
      }
    }

    entries.sortedBy { it.slotIndex }.forEach { entry ->
      Crossfade(
        targetState = entry.missionId,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "daily_slot_crossfade_${entry.slotIndex}"
      ) { targetMissionId ->
        val cardMission = missionsById[targetMissionId]
        if (cardMission != null) {
          val isDone = todayCompletedIds.contains(cardMission.id)
          DailyActionCard(
            slotIndex = entry.slotIndex,
            mission = cardMission,
            isCompleted = isDone,
            onToggleComplete = { onToggleComplete(cardMission) },
            onViewDetails = { onViewDetails(cardMission) },
            onSwap = { onSwapSlot(entry.slotIndex) },
            onQuickRate = { rating -> onQuickRate(cardMission, rating) }
          )
        }
      }
    }
  }
}

@Composable
private fun DailyActionCard(
  slotIndex: Int,
  mission: BalanceMission,
  isCompleted: Boolean,
  onToggleComplete: () -> Unit,
  onViewDetails: () -> Unit,
  onSwap: () -> Unit,
  onQuickRate: (HelpfulnessRating) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val dim = DimensionsProvider.dimensions.getOrNull(mission.dimensionIndex)
  val accentColor = dim?.accentColor ?: eqColors.accentPrimary
  val isOffDayHabit = mission.isHabit && !mission.isHabitScheduledToday && !isCompleted

  val cardInteraction = rememberBounceInteractionSource()
  val completeInteraction = rememberBounceInteractionSource()
  val detailInteraction = rememberBounceInteractionSource()
  val swapInteraction = rememberBounceInteractionSource()

  val cardBg by animateColorAsState(
    targetValue = when {
      isCompleted -> eqColors.completedCard
      isOffDayHabit -> eqColors.offDayCard
      else -> eqColors.surfaceCard
    },
    animationSpec = tween(240),
    label = "daily_card_bg_${mission.id}"
  )
  val borderColor by animateColorAsState(
    targetValue = if (isCompleted) eqColors.completedBorder else eqColors.border,
    animationSpec = tween(240),
    label = "daily_card_border_${mission.id}"
  )

  Surface(
    onClick = onViewDetails,
    interactionSource = cardInteraction,
    shape = RoundedCornerShape(20.dp),
    color = cardBg,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = if (isCompleted || isOffDayHabit) 0.dp else 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (isOffDayHabit) 0.64f else 1f)
      .bouncyScale(cardInteraction, scaleDown = 0.98f)
      .testTag("daily_action_card_$slotIndex")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isCompleted || isOffDayHabit) eqColors.completedBadge else accentColor.copy(alpha = 0.12f)
          ) {
            Text(
              text = mission.dimensionTitle.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isCompleted || isOffDayHabit) eqColors.completedTitle else accentColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          if (isCompleted) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = eqColors.completedBadge
            ) {
              Text(
                text = "Completada",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.completedContent,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          } else if (isOffDayHabit) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = eqColors.completedBadge
            ) {
              Text(
                text = "Vuelve el ${mission.nextHabitDayLabel ?: "pronto"}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.completedContent,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (mission.hasMultiplier) eqColors.warningContainer else eqColors.surfaceVariant
            ) {
              Text(
                text = if (mission.hasMultiplier) "+${mission.expReward} EXP · 1.5x" else "+${mission.expReward} EXP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (mission.hasMultiplier) eqColors.warningText else eqColors.textPrimary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }
        }

        if (!isCompleted) {
          Surface(
            onClick = onSwap,
            interactionSource = swapInteraction,
            shape = RoundedCornerShape(10.dp),
            color = eqColors.surfaceVariant,
            modifier = Modifier
              .bouncyScale(swapInteraction, scaleDown = 0.91f)
              .testTag("btn_daily_swap_$slotIndex")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = "Cambiar misión sin penalización",
                tint = eqColors.textSecondary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Cambiar",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = eqColors.textSecondary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = mission.title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.5.sp
        ),
        color = if (isCompleted || isOffDayHabit) eqColors.completedTitle else eqColors.textPrimary
      )

      Spacer(modifier = Modifier.height(3.dp))

      Text(
        text = mission.summary.ifEmpty { mission.description },
        style = MaterialTheme.typography.bodySmall,
        color = if (isCompleted || isOffDayHabit) eqColors.completedSubtitle else eqColors.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onToggleComplete,
          enabled = !isOffDayHabit,
          interactionSource = completeInteraction,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isCompleted) eqColors.completedBadge else eqColors.accentPrimary,
            contentColor = if (isCompleted) eqColors.completedContent else eqColors.onAccentPrimary,
            disabledContainerColor = eqColors.completedBadge,
            disabledContentColor = eqColors.completedTitle
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .then(if (!isOffDayHabit) Modifier.bouncyScale(completeInteraction, scaleDown = 0.93f) else Modifier)
            .testTag("btn_daily_complete_$slotIndex")
        ) {
          Icon(
            imageVector = if (isOffDayHabit) Icons.Outlined.EventRepeat else Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = when {
              isOffDayHabit -> "Vuelve el ${mission.nextHabitDayLabel ?: "pronto"}"
              isCompleted -> "Completada"
              else -> "Completar"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
          )
        }

        Surface(
          onClick = onViewDetails,
          interactionSource = detailInteraction,
          shape = RoundedCornerShape(12.dp),
          color = eqColors.surfaceCard,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier
            .height(38.dp)
            .bouncyScale(detailInteraction, scaleDown = 0.93f)
            .testTag("btn_daily_details_$slotIndex")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Detalle",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = eqColors.textPrimary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = eqColors.textPrimary,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      AnimatedVisibility(
        visible = isCompleted,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically()
      ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          QuickHelpfulnessRow(
            missionId = mission.id,
            selectedRating = mission.helpfulRating,
            onSelectRating = onQuickRate
          )
        }
      }
    }
  }
}

@Composable
private fun QuickHelpfulnessRow(
  missionId: String,
  selectedRating: HelpfulnessRating?,
  onSelectRating: (HelpfulnessRating) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "¿Cómo te sentiste? (puedes cambiarlo después)",
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
      color = eqColors.textSecondary
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      val options = listOf(
        Triple(HelpfulnessRating.HELPFUL, "Me ayudó", Pair(Icons.Outlined.ThumbUp, eqColors.successText)),
        Triple(HelpfulnessRating.NEUTRAL, "Neutro", Pair(Icons.Outlined.Remove, eqColors.accentPrimary)),
        Triple(HelpfulnessRating.HEAVY, "Pesado", Pair(Icons.Outlined.HourglassEmpty, eqColors.warningAccent))
      )
      options.forEach { (rating, label, iconAndColor) ->
        val (icon, activeColor) = iconAndColor
        val isSelected = selectedRating == rating
        val chipInteraction = rememberBounceInteractionSource()
        val bg by animateColorAsState(
          targetValue = if (isSelected) activeColor.copy(alpha = 0.14f) else eqColors.surfaceCard,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "quick_rate_bg_${missionId}_${rating.name}"
        )

        Surface(
          onClick = { onSelectRating(rating) },
          interactionSource = chipInteraction,
          shape = RoundedCornerShape(10.dp),
          color = bg,
          border = BorderStroke(1.dp, if (isSelected) activeColor else eqColors.border),
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .bouncyScale(chipInteraction, scaleDown = 0.92f)
            .testTag("quick_rate_${missionId}_${rating.name.lowercase()}")
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = if (isSelected) activeColor else eqColors.textSecondary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              ),
              color = if (isSelected) activeColor else eqColors.textPrimary,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HoySpotlightDeckCard(
  mission: BalanceMission,
  isCompletedToday: Boolean,
  positionIndex: Int,
  totalCount: Int,
  slideDirection: Int,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onViewDetails: () -> Unit,
  onQuickRate: (HelpfulnessRating) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val dim = DimensionsProvider.dimensions.getOrNull(mission.dimensionIndex)
  val accentColor = dim?.accentColor ?: eqColors.accentPrimary
  val isOffDayHabit = mission.isHabit && !mission.isHabitScheduledToday && !isCompletedToday
  val cardInteraction = rememberBounceInteractionSource()
  val btnInteraction = rememberBounceInteractionSource()
  val prevInteraction = rememberBounceInteractionSource()
  val nextInteraction = rememberBounceInteractionSource()

  val cardBg by animateColorAsState(
    targetValue = when {
      isCompletedToday -> eqColors.completedCard
      isOffDayHabit -> eqColors.offDayCard
      else -> eqColors.surfaceCard
    },
    animationSpec = tween(250),
    label = "hoy_spotlight_bg"
  )
  val borderColor by animateColorAsState(
    targetValue = if (isCompletedToday) eqColors.completedBorder else eqColors.border,
    animationSpec = tween(250),
    label = "hoy_spotlight_border"
  )

  Surface(
    onClick = onViewDetails,
    interactionSource = cardInteraction,
    shape = RoundedCornerShape(22.dp),
    color = cardBg,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = if (isCompletedToday || isOffDayHabit) 0.dp else 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (isOffDayHabit) 0.64f else 1f)
      .bouncyScale(cardInteraction, scaleDown = 0.98f)
      .testTag("hoy_spotlight_card_${mission.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "FOCO SUGERIDO",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = eqColors.textSecondary
        )

        if (totalCount > 1) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              onClick = onPrevious,
              interactionSource = prevInteraction,
              shape = CircleShape,
              color = eqColors.surfaceVariant,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(34.dp)
                .bouncyScale(prevInteraction, scaleDown = 0.88f)
                .testTag("btn_hoy_mission_prev")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDescription = "Misión anterior",
                  tint = eqColors.textPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            Text(
              text = "${positionIndex + 1}/$totalCount",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
              ),
              color = eqColors.textSecondary,
              modifier = Modifier.padding(horizontal = 6.dp)
            )

            Surface(
              onClick = onNext,
              interactionSource = nextInteraction,
              shape = CircleShape,
              color = eqColors.surfaceVariant,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(34.dp)
                .bouncyScale(nextInteraction, scaleDown = 0.88f)
                .testTag("btn_hoy_mission_next")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Siguiente misión",
                  tint = eqColors.textPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      AnimatedContent(
        targetState = mission,
        transitionSpec = {
          val offset = if (slideDirection >= 0) 40 else -40
          (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { offset }) togetherWith
            (fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { -offset / 2 })
        },
        label = "hoy_spotlight_mission_slide"
      ) { item ->
        val itemOffDay = item.isHabit && !item.isHabitScheduledToday && !isCompletedToday
        Column {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isCompletedToday || itemOffDay) eqColors.completedBadge else accentColor.copy(alpha = 0.12f)
            ) {
              Text(
                text = item.dimensionTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isCompletedToday || itemOffDay) eqColors.completedTitle else accentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }

            if (isCompletedToday) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = eqColors.completedBadge
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = eqColors.completedContent,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Completada",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = eqColors.completedContent
                  )
                }
              }
            } else if (itemOffDay) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = eqColors.completedBadge
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.EventRepeat,
                    contentDescription = null,
                    tint = eqColors.completedContent,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Vuelve el ${item.nextHabitDayLabel ?: "pronto"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = eqColors.completedContent
                  )
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (item.hasMultiplier) eqColors.warningContainer else eqColors.surfaceVariant
              ) {
                Text(
                  text = if (item.hasMultiplier) "+${item.expReward} EXP · 1.5x" else "+${item.expReward} EXP",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (item.hasMultiplier) eqColors.warningText else eqColors.textPrimary,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isCompletedToday || itemOffDay) eqColors.completedTitle else eqColors.textPrimary
          )

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = item.summary.ifEmpty { item.description },
            style = MaterialTheme.typography.bodySmall,
            color = if (isCompletedToday || itemOffDay) eqColors.completedSubtitle else eqColors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = eqColors.textTertiary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${mission.durationLabel} · ${mission.difficulty}",
            style = MaterialTheme.typography.labelSmall,
            color = eqColors.textSecondary
          )
        }

        Button(
          onClick = onViewDetails,
          interactionSource = btnInteraction,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isCompletedToday || isOffDayHabit) eqColors.completedBadge else eqColors.accentPrimary,
            contentColor = if (isCompletedToday || isOffDayHabit) eqColors.completedContent else eqColors.onAccentPrimary
          ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier
            .height(40.dp)
            .bouncyScale(btnInteraction, scaleDown = 0.94f)
            .testTag("btn_hoy_details_${mission.id}")
        ) {
          Text(
            text = "Ver detalles",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      AnimatedVisibility(
        visible = isCompletedToday,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically()
      ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          QuickHelpfulnessRow(
            missionId = "spotlight_${mission.id}",
            selectedRating = mission.helpfulRating,
            onSelectRating = onQuickRate
          )
        }
      }
    }
  }
}

@Composable
private fun CalmPhraseBanner(
  phrase: CalmPhrase,
  accentColor: Color,
  refreshSpinCount: Int,
  onNextPhrase: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val bannerInteraction = rememberBounceInteractionSource()
  val rotation by animateFloatAsState(
    targetValue = refreshSpinCount * 180f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "phrase_refresh_spin"
  )

  Surface(
    onClick = onNextPhrase,
    interactionSource = bannerInteraction,
    shape = RoundedCornerShape(18.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    modifier = Modifier
      .fillMaxWidth()
      .bouncyScale(bannerInteraction, scaleDown = 0.97f)
      .testTag("btn_hoy_next_phrase")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.12f),
        modifier = Modifier.size(30.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      AnimatedContent(
        targetState = phrase,
        transitionSpec = {
          (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(260)) { it / 3 }) togetherWith
            (fadeOut(tween(170, easing = FastOutSlowInEasing)) +
              slideOutVertically(tween(170)) { -it / 3 })
        },
        modifier = Modifier.weight(1f),
        label = "hoy_phrase_cycle"
      ) { item ->
        Column {
          Text(
            text = item.quote,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.Medium,
              fontStyle = FontStyle.Italic,
              lineHeight = 18.sp
            ),
            color = eqColors.textPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${item.author} · Toca para leer otra",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = eqColors.textTertiary
          )
        }
      }

      Box(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .size(36.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Siguiente frase",
          tint = eqColors.textSecondary,
          modifier = Modifier
            .size(16.dp)
            .graphicsLayer { rotationZ = rotation }
        )
      }
    }
  }
}

@Composable
private fun AcceptedWeeklyChallengeHoyCard(
  challenge: BalanceMission,
  isCompleted: Boolean,
  onToggleComplete: () -> Unit,
  onViewDetails: () -> Unit,
  onQuickRate: (HelpfulnessRating) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val dim = DimensionsProvider.dimensions.getOrNull(challenge.dimensionIndex)
  val accentColor = dim?.accentColor ?: eqColors.accentPrimary
  val cardInteraction = rememberBounceInteractionSource()
  val completeInteraction = rememberBounceInteractionSource()
  val detailInteraction = rememberBounceInteractionSource()

  val cardBg by animateColorAsState(
    targetValue = if (isCompleted) eqColors.completedCard else eqColors.surfaceCard,
    animationSpec = tween(240),
    label = "hoy_weekly_card_bg"
  )

  Surface(
    onClick = onViewDetails,
    interactionSource = cardInteraction,
    shape = RoundedCornerShape(20.dp),
    color = cardBg,
    border = BorderStroke(1.5.dp, if (isCompleted) eqColors.completedBorder else accentColor.copy(alpha = 0.45f)),
    shadowElevation = if (isCompleted) 0.dp else 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .bouncyScale(cardInteraction, scaleDown = 0.98f)
      .testTag("hoy_weekly_challenge_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isCompleted) eqColors.completedBadge else accentColor.copy(alpha = 0.14f)
        ) {
          Text(
            text = "RETO DE LA SEMANA · ${challenge.dimensionTitle.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isCompleted) eqColors.completedTitle else accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isCompleted) eqColors.completedBadge else eqColors.warningContainer
        ) {
          Text(
            text = if (isCompleted) "Completado" else "+${challenge.expReward} EXP · Extra",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isCompleted) eqColors.completedContent else eqColors.warningText,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = challenge.title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.5.sp
        ),
        color = if (isCompleted) eqColors.completedTitle else eqColors.textPrimary
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = "${challenge.durationLabel} · ${challenge.steps.size} pasos · Fuera del cap diario",
        style = MaterialTheme.typography.labelSmall,
        color = eqColors.textSecondary
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onToggleComplete,
          interactionSource = completeInteraction,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isCompleted) eqColors.completedBadge else eqColors.accentPrimary,
            contentColor = if (isCompleted) eqColors.completedContent else eqColors.onAccentPrimary
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .bouncyScale(completeInteraction, scaleDown = 0.93f)
            .testTag("btn_hoy_weekly_complete")
        ) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (isCompleted) "Completado" else "Completar (+80 EXP)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
          )
        }

        Surface(
          onClick = onViewDetails,
          interactionSource = detailInteraction,
          shape = RoundedCornerShape(12.dp),
          color = eqColors.surfaceCard,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier
            .height(38.dp)
            .bouncyScale(detailInteraction, scaleDown = 0.93f)
            .testTag("btn_hoy_weekly_details")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Pasos",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = eqColors.textPrimary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = eqColors.textPrimary,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      AnimatedVisibility(
        visible = isCompleted,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically()
      ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          QuickHelpfulnessRow(
            missionId = "weekly_${challenge.id}",
            selectedRating = challenge.helpfulRating,
            onSelectRating = onQuickRate
          )
        }
      }
    }
  }
}
