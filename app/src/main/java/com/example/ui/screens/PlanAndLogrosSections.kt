package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirstDayOfWeek
import com.example.data.NotificationPreferences
import com.example.domain.CompletionRecord
import com.example.domain.DayKey
import com.example.domain.HexagonCalculator
import com.example.model.AssessmentReport
import com.example.model.BalanceMission
import com.example.model.DimensionsProvider
import com.example.model.GamificationStats
import com.example.model.UserProfile
import com.example.ui.components.EmptyReflectionsCard
import com.example.ui.components.NotificationsSettingsCard
import com.example.ui.components.ReflectionDiaryEntryCard
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.equilibrioColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayActivitySummary(
  val dateKey: String,
  val dayLetter: String,
  val dayNumber: String,
  val isToday: Boolean,
  val isFuture: Boolean,
  val completedCount: Int
)

object WeeklyCalendarStripBuilder {
  fun buildWeekDays(
    firstDayOfWeek: FirstDayOfWeek,
    completions: List<CompletionRecord>,
    dayKey: DayKey = DayKey.DEFAULT
  ): List<DayActivitySummary> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayKey = dayKey.today()
    val countByDate = completions.groupingBy { it.date }.eachCount()

    val cal = Calendar.getInstance()
    val todayParsed = runCatching { formatter.parse(todayKey) }.getOrNull()
    if (todayParsed != null) {
      cal.time = todayParsed
    }

    val startCalendarDay = if (firstDayOfWeek == FirstDayOfWeek.SUNDAY) {
      Calendar.SUNDAY
    } else {
      Calendar.MONDAY
    }
    val currentDow = cal.get(Calendar.DAY_OF_WEEK)
    val offsetFromStart = (currentDow - startCalendarDay + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -offsetFromStart)

    return (0..6).map { _ ->
      val key = formatter.format(cal.time)
      val letter = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "L"
        Calendar.TUESDAY -> "M"
        Calendar.WEDNESDAY -> "X"
        Calendar.THURSDAY -> "J"
        Calendar.FRIDAY -> "V"
        Calendar.SATURDAY -> "S"
        Calendar.SUNDAY -> "D"
        else -> ""
      }
      val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString()
      val summary = DayActivitySummary(
        dateKey = key,
        dayLetter = letter,
        dayNumber = dayNum,
        isToday = key == todayKey,
        isFuture = key > todayKey,
        completedCount = countByDate[key] ?: 0
      )
      cal.add(Calendar.DAY_OF_YEAR, 1)
      summary
    }
  }
}

private data class RealAchievementBadge(
  val id: String,
  val title: String,
  val description: String,
  val icon: ImageVector,
  val unlocked: Boolean,
  val progressLabel: String
)

/**
 * Apartado 3: Plan de Acción Semanal y Hábitos.
 * Muestra la racha de los últimos 7 días reales desde Room, el norte mensual editable,
 * las misiones/hábitos creados por el usuario y las prioridades diarias sugeridas.
 */
@Composable
fun PlanSection(
  userProfile: UserProfile,
  report: AssessmentReport,
  realScores: Map<Int, Int>,
  completionsLast14Days: List<CompletionRecord>,
  allMissions: List<BalanceMission>,
  onViewMissionDetails: (BalanceMission) -> Unit,
  onOpenCreateMission: () -> Unit,
  onEditGoal: () -> Unit,
  allCompletions: List<CompletionRecord> = completionsLast14Days,
  acceptedWeeklyChallenge: BalanceMission? = null,
  notificationPrefs: NotificationPreferences = NotificationPreferences(),
  firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY,
  isFrozenToday: Boolean = false,
  freezeMessage: String? = null,
  onUpdateNotificationPrefs: (NotificationPreferences) -> Unit = {},
  onFreezeStreak: () -> Unit = {},
  onEditReflection: (CompletionRecord) -> Unit = {},
  onResolveMissionById: (String) -> BalanceMission? = { id -> allMissions.find { it.id == id } },
  modifier: Modifier = Modifier
) {
  val weekDays = remember(completionsLast14Days, firstDayOfWeek) {
    WeeklyCalendarStripBuilder.buildWeekDays(
      firstDayOfWeek = firstDayOfWeek,
      completions = completionsLast14Days,
      dayKey = DayKey.DEFAULT
    )
  }

  val activeDaysInWeek = remember(weekDays) {
    weekDays.count { it.completedCount > 0 }
  }

  val customAndHabitMissions = remember(allMissions) {
    allMissions.filter { it.isCustom || it.isHabit }
  }

  val lowestDimIndex = remember(realScores, report.scores) {
    HexagonCalculator.findLowestRealDimension(realScores, report.scores)
  }

  val priorityPlanMissions = remember(allMissions, lowestDimIndex) {
    val lowestList = allMissions.filter { it.dimensionIndex == lowestDimIndex }
    val others = allMissions.filter { it.dimensionIndex != lowestDimIndex && it.isWeaknessFocus }
    (lowestList + others).distinctBy { it.id }.take(4)
  }

  val reflectionEntries = remember(allCompletions) {
    allCompletions
      .filter { it.reflectionNote.isNotBlank() || it.helpfulness != null }
      .sortedByDescending { it.completedAtMillis }
  }

  val eqColors = MaterialTheme.equilibrioColors
  val createInteraction = rememberBounceInteractionSource()
  val editGoalInteraction = rememberBounceInteractionSource()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Encabezado
    item(key = "plan_header") {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Tu Plan de Acción",
            style = MaterialTheme.typography.displaySmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 26.sp
            ),
            color = eqColors.textPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Consistencia diaria para transformar tu hexágono",
            style = MaterialTheme.typography.bodyMedium,
            color = eqColors.textSecondary
          )
        }

        Button(
          onClick = onOpenCreateMission,
          interactionSource = createInteraction,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = eqColors.accentPrimary,
            contentColor = eqColors.onAccentPrimary
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier
            .bouncyScale(createInteraction, scaleDown = 0.92f)
            .testTag("btn_plan_create_mission")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Nuevo",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }

    // Tarjeta de consistencia semanal (7 días reales de Room)
    item(key = "plan_weekly_strip") {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = eqColors.surfaceCard,
        border = BorderStroke(1.dp, eqColors.border),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = eqColors.accentPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (firstDayOfWeek == FirstDayOfWeek.SUNDAY) "Tu semana · Dom – Sáb" else "Tu semana · Lun – Dom",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = eqColors.textPrimary
              )
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (activeDaysInWeek > 0) eqColors.successBadge else eqColors.surfaceVariant
            ) {
              Text(
                text = "$activeDaysInWeek / 7 días activos",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (activeDaysInWeek > 0) eqColors.successText else eqColors.textSecondary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            weekDays.forEach { day ->
              val hasActivity = day.completedCount > 0
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = day.dayLetter,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (day.isToday) eqColors.accentPrimary else eqColors.textTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        hasActivity -> eqColors.accentPrimary
                        day.isToday -> eqColors.surfaceVariant
                        day.isFuture -> eqColors.surfaceVariant.copy(alpha = 0.45f)
                        else -> eqColors.surfaceCard
                      }
                    )
                    .border(
                      width = if (day.isToday && !hasActivity) 1.5.dp else 1.dp,
                      color = when {
                        hasActivity -> eqColors.accentPrimary
                        day.isToday -> eqColors.accentPrimary
                        else -> eqColors.border
                      },
                      shape = CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (hasActivity) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Día completado",
                      tint = eqColors.onAccentPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  } else {
                    Text(
                      text = day.dayNumber,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                      ),
                      color = if (day.isToday) eqColors.accentPrimary else eqColors.textSecondary
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (day.completedCount > 0) "+${day.completedCount}" else "",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = eqColors.accentPrimary
                )
              }
            }
          }
        }
      }
    }

    // Meta del mes editable
    item(key = "plan_monthly_goal") {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = eqColors.surfaceCard,
        border = BorderStroke(1.dp, eqColors.border),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(eqColors.accentPrimary.copy(alpha = 0.08f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = eqColors.accentPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "META DEL MES",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.6.sp
                ),
                color = eqColors.textTertiary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = userProfile.monthlyGoal.ifBlank { "Quiero ser mi mejor versión" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.textPrimary
              )
            }
          }

          Surface(
            onClick = onEditGoal,
            interactionSource = editGoalInteraction,
            shape = RoundedCornerShape(12.dp),
            color = eqColors.surfaceVariant,
            modifier = Modifier
              .bouncyScale(editGoalInteraction, scaleDown = 0.92f)
              .testTag("btn_plan_edit_goal")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar meta",
                tint = eqColors.accentPrimary,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Editar",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = eqColors.accentPrimary
              )
            }
          }
        }
      }
    }

    // Recordatorios amables y Guardián de racha (WorkManager)
    item(key = "plan_notifications_settings") {
      NotificationsSettingsCard(
        prefs = notificationPrefs,
        activeStreakDays = userProfile.activeStreakDays,
        totalExp = userProfile.exp,
        isFrozenToday = isFrozenToday,
        freezeMessage = freezeMessage,
        onUpdatePrefs = onUpdateNotificationPrefs,
        onFreezeStreak = onFreezeStreak
      )
    }

    // Reto de la semana aceptado (si el usuario lo añadió desde Misiones)
    if (acceptedWeeklyChallenge != null) {
      item(key = "plan_accepted_weekly_header") {
        Text(
          text = "Reto de la semana en curso",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      item(key = "plan_accepted_weekly_card") {
        CompactMissionCard(
          mission = acceptedWeeklyChallenge,
          onContinue = { onViewMissionDetails(acceptedWeeklyChallenge) }
        )
      }
    }

    // Sección: Tus Hábitos y Misiones Personalizadas
    item(key = "plan_custom_header") {
      Text(
        text = "Tus Hábitos y Misiones (${customAndHabitMissions.size})",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = eqColors.textPrimary,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    if (customAndHabitMissions.isEmpty()) {
      item(key = "plan_custom_empty") {
        val emptyCreateInteraction = rememberBounceInteractionSource()
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = eqColors.surfaceVariant,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Outlined.EventRepeat,
              contentDescription = null,
              tint = eqColors.accentPrimary,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Aún no has creado hábitos o misiones propias",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = eqColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Diseña actividades diarias o semanales para fortalecer las áreas que elijas.",
              style = MaterialTheme.typography.bodySmall,
              color = eqColors.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
              onClick = onOpenCreateMission,
              interactionSource = emptyCreateInteraction,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, eqColors.accentPrimary),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.accentPrimary),
              modifier = Modifier.bouncyScale(emptyCreateInteraction)
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Crear Hábito / Misión",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    } else {
      items(customAndHabitMissions, key = { "plan_custom_${it.id}" }) { mission ->
        CompactMissionCard(
          mission = mission,
          onContinue = { onViewMissionDetails(mission) }
        )
      }
    }

    // Sección: Enfoque Prioritario de Hoy
    item(key = "plan_priority_header") {
      val focusDim = DimensionsProvider.dimensions.getOrNull(lowestDimIndex)
      Column(modifier = Modifier.padding(top = 6.dp)) {
        Text(
          text = "Plan Recomendado · ${focusDim?.title ?: "Equilibrio"}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary
        )
        Text(
          text = "Actividades seleccionadas según tu puntaje real de los últimos 14 días.",
          style = MaterialTheme.typography.bodySmall,
          color = eqColors.textSecondary
        )
      }
    }

    items(priorityPlanMissions, key = { "plan_prio_${it.id}" }) { mission ->
      CompactMissionCard(
        mission = mission,
        onContinue = { onViewMissionDetails(mission) }
      )
    }

    // Sección: Tus reflexiones (Diario cronológico inverso editable)
    item(key = "plan_reflections_header") {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Tus reflexiones",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textPrimary
          )
          Text(
            text = "Toca una nota para editar tu valoración o aprendizaje",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
        if (reflectionEntries.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = eqColors.surfaceVariant
          ) {
            Text(
              text = "${reflectionEntries.size}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = eqColors.accentPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    if (reflectionEntries.isEmpty()) {
      item(key = "plan_reflections_empty") {
        EmptyReflectionsCard()
      }
    } else {
      items(reflectionEntries, key = { "refl_${it.id}_${it.missionId}_${it.date}" }) { record ->
        ReflectionDiaryEntryCard(
          record = record,
          mission = onResolveMissionById(record.missionId),
          onEditReflection = onEditReflection
        )
      }
    }

    item(key = "plan_bottom_space") {
      Spacer(modifier = Modifier.height(116.dp))
    }
  }
}

/**
 * Apartado 4: Logros, Progreso por Dimensión e Insignias Reales.
 * Todo calculado en tiempo real desde Room (cero datos inventados).
 */
@Composable
fun LogrosSection(
  userProfile: UserProfile,
  report: AssessmentReport,
  realScores: Map<Int, Int>,
  expByDimension14d: Map<Int, Int>,
  liveHarmonyPercentage: Int,
  completionsCount: Int,
  customMissionsCount: Int,
  onOpenFullReport: () -> Unit,
  onNavigateToMissions: () -> Unit,
  isFrozenToday: Boolean = false,
  freezeMessage: String? = null,
  onFreezeStreak: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val expPerLevel = GamificationStats.EXP_PER_LEVEL
  val expInCurrentLevel = userProfile.exp % expPerLevel
  val levelProgress = (expInCurrentLevel.toFloat() / expPerLevel.toFloat()).coerceIn(0f, 1f)
  val animatedLevelProgress by animateFloatAsState(
    targetValue = levelProgress,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "logros_level_progress"
  )
  val animatedTotalExp by animateIntAsState(
    targetValue = userProfile.exp,
    animationSpec = tween(durationMillis = 550),
    label = "logros_total_exp"
  )

  val badges = remember(completionsCount, userProfile.activeStreakDays, customMissionsCount, liveHarmonyPercentage, realScores) {
    val allDimsActive = (0..5).all { (realScores[it] ?: 0) >= 3 }
    listOf(
      RealAchievementBadge(
        id = "first_mission",
        title = "Primer Paso",
        description = "Completa tu primera misión en cualquier área",
        icon = Icons.Outlined.Flag,
        unlocked = completionsCount >= 1,
        progressLabel = "${minOf(completionsCount, 1)} / 1"
      ),
      RealAchievementBadge(
        id = "streak_3",
        title = "Ritmo Constante",
        description = "Mantén una racha activa de 3 días seguidos",
        icon = Icons.Outlined.LocalFireDepartment,
        unlocked = userProfile.activeStreakDays >= 3,
        progressLabel = "${minOf(userProfile.activeStreakDays, 3)} / 3 días"
      ),
      RealAchievementBadge(
        id = "streak_7",
        title = "Semana Imparable",
        description = "Alcanza 7 días consecutivos cuidando tu equilibrio",
        icon = Icons.Outlined.DateRange,
        unlocked = userProfile.activeStreakDays >= 7,
        progressLabel = "${minOf(userProfile.activeStreakDays, 7)} / 7 días"
      ),
      RealAchievementBadge(
        id = "custom_creator",
        title = "Arquitecto Personal",
        description = "Crea tu propia misión o hábito personalizado",
        icon = Icons.Outlined.AutoAwesome,
        unlocked = customMissionsCount >= 1,
        progressLabel = "${minOf(customMissionsCount, 1)} / 1"
      ),
      RealAchievementBadge(
        id = "ten_missions",
        title = "Disciplina Activa",
        description = "Completa 10 misiones registradas en tu historial",
        icon = Icons.Outlined.EmojiEvents,
        unlocked = completionsCount >= 10,
        progressLabel = "${minOf(completionsCount, 10)} / 10"
      ),
      RealAchievementBadge(
        id = "balanced_hex",
        title = "Armonía Viva",
        description = "Llega a 75% de armonía o 3/10 real en las 6 áreas",
        icon = Icons.Outlined.Hexagon,
        unlocked = liveHarmonyPercentage >= 75 || allDimsActive,
        progressLabel = "$liveHarmonyPercentage%"
      )
    )
  }

  val eqColors = MaterialTheme.equilibrioColors
  val unlockedCount = badges.count { it.unlocked }
  val reportButtonInteraction = rememberBounceInteractionSource()
  val missionsButtonInteraction = rememberBounceInteractionSource()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Encabezado
    item(key = "logros_header") {
      Column(modifier = Modifier.padding(top = 14.dp)) {
        Text(
          text = "Logros y Progreso",
          style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
          ),
          color = eqColors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Tus avances reales reflejados en nivel e insignias",
          style = MaterialTheme.typography.bodyMedium,
          color = eqColors.textSecondary
        )
      }
    }

    // Tarjeta Hero de Nivel y EXP
    item(key = "logros_level_hero") {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = eqColors.heroSurface,
        border = BorderStroke(1.dp, eqColors.heroBorder),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(eqColors.onHeroSurface.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.WorkspacePremium,
                  contentDescription = null,
                  tint = eqColors.heroGold,
                  modifier = Modifier.size(26.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Nivel ${userProfile.level}",
                  style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                  ),
                  color = eqColors.onHeroSurface
                )
                Text(
                  text = userProfile.rank,
                  style = MaterialTheme.typography.labelLarge,
                  color = eqColors.onHeroSurface.copy(alpha = 0.78f)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = eqColors.onHeroSurface.copy(alpha = 0.12f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.ElectricBolt,
                  contentDescription = null,
                  tint = eqColors.heroGold,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$animatedTotalExp EXP",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = eqColors.onHeroSurface
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Progreso al Nivel ${userProfile.level + 1}",
              style = MaterialTheme.typography.labelSmall,
              color = eqColors.onHeroSurface.copy(alpha = 0.75f)
            )
            Text(
              text = "$expInCurrentLevel / $expPerLevel EXP",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = eqColors.onHeroSurface
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(eqColors.onHeroSurface.copy(alpha = 0.16f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(animatedLevelProgress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(eqColors.heroGold)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            LogrosMetricPill(label = "Completadas", value = "$completionsCount", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            LogrosMetricPill(label = "Racha", value = "${userProfile.activeStreakDays}d", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            LogrosMetricPill(label = "Armonía", value = "$liveHarmonyPercentage%", modifier = Modifier.weight(1f))
          }

          val canFreezeStreak = userProfile.activeStreakDays >= 2 && userProfile.exp >= 100
          if (isFrozenToday || !freezeMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = eqColors.onHeroSurface.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, eqColors.onHeroSurface.copy(alpha = 0.25f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("logros_frozen_banner")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.AcUnit,
                  contentDescription = null,
                  tint = eqColors.heroFrost,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = freezeMessage ?: "Racha protegida. Mañana te esperamos",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = eqColors.onHeroSurface
                )
              }
            }
          } else if (canFreezeStreak) {
            val freezeInteraction = rememberBounceInteractionSource()
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
              onClick = onFreezeStreak,
              interactionSource = freezeInteraction,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, eqColors.onHeroSurface.copy(alpha = 0.35f)),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = eqColors.onHeroSurface.copy(alpha = 0.08f),
                contentColor = eqColors.onHeroSurface
              ),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .bouncyScale(freezeInteraction, scaleDown = 0.94f)
                .testTag("btn_freeze_streak_logros")
            ) {
              Icon(
                imageVector = Icons.Outlined.AcUnit,
                contentDescription = null,
                tint = eqColors.heroFrost,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Congelar racha (100 EXP)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }

    // Progreso de los 6 ejes del hexágono (14 días)
    item(key = "logros_hexagon_dimensions") {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = eqColors.surfaceCard,
        border = BorderStroke(1.dp, eqColors.border),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Dominio por Dimensión (14 días)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = eqColors.textPrimary
              )
              Text(
                text = "Meta: ${HexagonCalculator.DEFAULT_OBJETIVO_EXP} EXP por área para 10/10 real",
                style = MaterialTheme.typography.bodySmall,
                color = eqColors.textSecondary
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DimensionsProvider.dimensions.forEach { dim ->
              val exp14d = expByDimension14d[dim.index] ?: 0
              val targetExp = HexagonCalculator.DEFAULT_EXP_OBJETIVOS[dim.index] ?: HexagonCalculator.DEFAULT_OBJETIVO_EXP
              val realScore = realScores[dim.index] ?: 0
              val progress = (exp14d.toFloat() / targetExp.toFloat()).coerceIn(0f, 1f)
              val animProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 550),
                label = "logro_dim_${dim.index}"
              )

              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dim.accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = dim.title,
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                      color = eqColors.textPrimary
                    )
                  }
                  Text(
                    text = "$realScore/10 · $exp14d/$targetExp EXP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = eqColors.textSecondary
                  )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(eqColors.surfaceVariant)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth(animProgress)
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp))
                      .background(dim.accentColor)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onOpenFullReport,
              interactionSource = reportButtonInteraction,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, eqColors.border),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.accentPrimary),
              modifier = Modifier
                .weight(1f)
                .bouncyScale(reportButtonInteraction)
                .testTag("btn_logros_report")
            ) {
              Icon(
                imageVector = Icons.Outlined.Hexagon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Diagnóstico",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
            }

            Button(
              onClick = onNavigateToMissions,
              interactionSource = missionsButtonInteraction,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = eqColors.accentPrimary,
                contentColor = eqColors.onAccentPrimary
              ),
              modifier = Modifier
                .weight(1f)
                .bouncyScale(missionsButtonInteraction)
                .testTag("btn_logros_missions")
            ) {
              Text(
                text = "Sumar EXP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }

    // Insignias desbloqueadas
    item(key = "logros_badges_header") {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Insignias de Equilibrio",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary
        )
        Text(
          text = "$unlockedCount / ${badges.size} logradas",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = eqColors.accentPrimary
        )
      }
    }

    items(badges, key = { it.id }) { badge ->
      val cardBg by animateColorAsState(
        targetValue = if (badge.unlocked) eqColors.surfaceCard else eqColors.offDayCard,
        label = "badge_bg_${badge.id}"
      )
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(
          1.dp,
          if (badge.unlocked) eqColors.accentPrimary.copy(alpha = 0.28f) else eqColors.border
        ),
        shadowElevation = if (badge.unlocked) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (badge.unlocked) eqColors.accentPrimary else eqColors.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (badge.unlocked) badge.icon else Icons.Default.Lock,
              contentDescription = null,
              tint = if (badge.unlocked) eqColors.onAccentPrimary else eqColors.textTertiary,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = badge.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = if (badge.unlocked) eqColors.textPrimary else eqColors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = badge.description,
              style = MaterialTheme.typography.bodySmall,
              color = if (badge.unlocked) eqColors.textSecondary else eqColors.textTertiary
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (badge.unlocked) eqColors.successBadge else eqColors.surfaceVariant
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (badge.unlocked) {
                Icon(
                  imageVector = Icons.Outlined.CheckCircle,
                  contentDescription = null,
                  tint = eqColors.successText,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = if (badge.unlocked) "Lograda" else badge.progressLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (badge.unlocked) eqColors.successText else eqColors.textSecondary
              )
            }
          }
        }
      }
    }

    item(key = "logros_bottom_space") {
      Spacer(modifier = Modifier.height(116.dp))
    }
  }
}

@Composable
private fun LogrosMetricPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = eqColors.onHeroSurface.copy(alpha = 0.10f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = eqColors.onHeroSurface
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = eqColors.onHeroSurface.copy(alpha = 0.72f)
      )
    }
  }
}
