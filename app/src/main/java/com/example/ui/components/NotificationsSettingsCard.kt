package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationPreferences
import com.example.notifications.NotificationHelper
import com.example.ui.theme.equilibrioColors
import java.util.Locale

private enum class PendingNotificationAction {
  NONE,
  ENABLE_DAILY,
  ENABLE_GUARDIAN
}

@Composable
fun NotificationsSettingsCard(
  prefs: NotificationPreferences,
  activeStreakDays: Int,
  totalExp: Int,
  isFrozenToday: Boolean,
  freezeMessage: String?,
  onUpdatePrefs: (NotificationPreferences) -> Unit,
  onFreezeStreak: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
  var showPermissionSheet by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var pendingAction by remember { mutableStateOf(PendingNotificationAction.NONE) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted || NotificationHelper.hasNotificationPermission(context)
    if (hasPermission) {
      when (pendingAction) {
        PendingNotificationAction.ENABLE_DAILY -> {
          onUpdatePrefs(
            prefs.copy(
              dailyReminderEnabled = true,
              hasPromptedNotificationPermission = true
            )
          )
        }
        PendingNotificationAction.ENABLE_GUARDIAN -> {
          onUpdatePrefs(
            prefs.copy(
              streakGuardianEnabled = true,
              hasPromptedNotificationPermission = true
            )
          )
        }
        PendingNotificationAction.NONE -> {
          onUpdatePrefs(prefs.copy(hasPromptedNotificationPermission = true))
        }
      }
    }
    pendingAction = PendingNotificationAction.NONE
  }

  fun requestWithGentleContext(action: PendingNotificationAction) {
    pendingAction = action
    if (NotificationHelper.hasNotificationPermission(context)) {
      hasPermission = true
      when (action) {
        PendingNotificationAction.ENABLE_DAILY ->
          onUpdatePrefs(prefs.copy(dailyReminderEnabled = true, hasPromptedNotificationPermission = true))
        PendingNotificationAction.ENABLE_GUARDIAN ->
          onUpdatePrefs(prefs.copy(streakGuardianEnabled = true, hasPromptedNotificationPermission = true))
        PendingNotificationAction.NONE -> Unit
      }
      pendingAction = PendingNotificationAction.NONE
    } else {
      showPermissionSheet = true
    }
  }

  val eqColors = MaterialTheme.equilibrioColors
  val canFreezeStreak = activeStreakDays >= 2 && totalExp >= 100
  val timeInteraction = rememberBounceInteractionSource()
  val freezeInteraction = rememberBounceInteractionSource()
  val formattedTime = String.format(Locale.US, "%02d:%02d", prefs.dailyReminderHour, prefs.dailyReminderMinute)

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_plan_notifications")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(eqColors.accentPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = null,
              tint = eqColors.accentPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Recordatorios amables",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = eqColors.textPrimary
            )
            Text(
              text = "Te avisamos una vez al día, sin spam",
              style = MaterialTheme.typography.labelSmall,
              color = eqColors.textSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // a) Recordatorio diario con hora configurable (default 20:00)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Recordatorio diario",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textPrimary
          )
          Text(
            text = "Tu momento de equilibrio con acciones pendientes",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
          checked = prefs.dailyReminderEnabled,
          onCheckedChange = { enabled ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (enabled) {
              requestWithGentleContext(PendingNotificationAction.ENABLE_DAILY)
            } else {
              onUpdatePrefs(prefs.copy(dailyReminderEnabled = false))
            }
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = eqColors.onAccentPrimary,
            checkedTrackColor = eqColors.accentPrimary
          ),
          modifier = Modifier.testTag("switch_daily_reminder")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Surface(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          showTimePicker = true
        },
        interactionSource = timeInteraction,
        shape = RoundedCornerShape(12.dp),
        color = eqColors.surfaceVariant,
        border = BorderStroke(1.dp, eqColors.border),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .bouncyScale(timeInteraction, scaleDown = 0.94f)
          .testTag("btn_daily_reminder_time")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = "Elegir hora del recordatorio",
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Hora elegida: $formattedTime",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFeatureSettings = "tnum"
            ),
            color = eqColors.accentPrimary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = eqColors.border)
      Spacer(modifier = Modifier.height(12.dp))

      // b) Guardián de racha (>21:00 si no se completó nada hoy y racha >= 2)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Shield,
              contentDescription = null,
              tint = eqColors.accentPrimary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Guardián de racha",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = eqColors.textPrimary
            )
          }
          Text(
            text = "Solo tras las 21:00 si tienes racha ≥ 2 días y hoy no registraste acciones",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
          checked = prefs.streakGuardianEnabled,
          onCheckedChange = { enabled ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (enabled) {
              requestWithGentleContext(PendingNotificationAction.ENABLE_GUARDIAN)
            } else {
              onUpdatePrefs(prefs.copy(streakGuardianEnabled = false))
            }
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = eqColors.onAccentPrimary,
            checkedTrackColor = eqColors.accentPrimary
          ),
          modifier = Modifier.testTag("switch_streak_guardian")
        )
      }

      // Opción de congelar racha dentro del Guardián de racha cuando cumple criterios
      AnimatedVisibility(visible = canFreezeStreak || isFrozenToday || !freezeMessage.isNullOrBlank()) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          if (isFrozenToday) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = eqColors.infoContainer,
              border = BorderStroke(1.dp, eqColors.infoBorder),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("guardian_frozen_banner")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.AcUnit,
                  contentDescription = null,
                  tint = eqColors.infoIcon,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = freezeMessage ?: "Racha protegida. Mañana te esperamos",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = eqColors.infoText
                )
              }
            }
          } else if (canFreezeStreak) {
            OutlinedButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFreezeStreak()
              },
              interactionSource = freezeInteraction,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, eqColors.accentPrimary.copy(alpha = 0.35f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.accentPrimary),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .bouncyScale(freezeInteraction, scaleDown = 0.94f)
                .testTag("btn_freeze_streak_guardian")
            ) {
              Icon(
                imageVector = Icons.Outlined.AcUnit,
                contentDescription = null,
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
  }

  if (showPermissionSheet) {
    NotificationPermissionModal(
      onConfirm = {
        showPermissionSheet = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          hasPermission = true
          when (pendingAction) {
            PendingNotificationAction.ENABLE_DAILY ->
              onUpdatePrefs(prefs.copy(dailyReminderEnabled = true, hasPromptedNotificationPermission = true))
            PendingNotificationAction.ENABLE_GUARDIAN ->
              onUpdatePrefs(prefs.copy(streakGuardianEnabled = true, hasPromptedNotificationPermission = true))
            PendingNotificationAction.NONE ->
              onUpdatePrefs(prefs.copy(hasPromptedNotificationPermission = true))
          }
          pendingAction = PendingNotificationAction.NONE
        }
      },
      onDismiss = {
        showPermissionSheet = false
        pendingAction = PendingNotificationAction.NONE
        onUpdatePrefs(prefs.copy(hasPromptedNotificationPermission = true))
      }
    )
  }

  if (showTimePicker) {
    ReminderTimePickerDialog(
      initialHour = prefs.dailyReminderHour,
      initialMinute = prefs.dailyReminderMinute,
      onConfirm = { hour, minute ->
        showTimePicker = false
        onUpdatePrefs(prefs.copy(dailyReminderHour = hour, dailyReminderMinute = minute))
      },
      onDismiss = { showTimePicker = false }
    )
  }
}

@Composable
fun NotificationPermissionModal(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val confirmInteraction = rememberBounceInteractionSource()
  val dismissInteraction = rememberBounceInteractionSource()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = eqColors.canvasBackground,
    shape = RoundedCornerShape(26.dp),
    icon = {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(eqColors.accentPrimary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.NotificationsActive,
          contentDescription = null,
          tint = eqColors.accentPrimary,
          modifier = Modifier.size(26.dp)
        )
      }
    },
    title = {
      Text(
        text = "Recordatorios a tu ritmo",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = eqColors.textPrimary
      )
    },
    text = {
      Text(
        text = "Te avisamos una vez al día, sin spam. Puedes ajustar la hora o apagar cada aviso cuando quieras desde Plan.",
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
        color = eqColors.textSecondary
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        interactionSource = confirmInteraction,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = eqColors.accentPrimary,
          contentColor = eqColors.onAccentPrimary
        ),
        modifier = Modifier
          .bouncyScale(confirmInteraction, scaleDown = 0.94f)
          .testTag("btn_confirm_notification_permission")
      ) {
        Text("Activar avisos", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        interactionSource = dismissInteraction,
        modifier = Modifier
          .bouncyScale(dismissInteraction)
          .testTag("btn_dismiss_notification_permission")
      ) {
        Text("Ahora no", color = eqColors.textSecondary, fontWeight = FontWeight.Medium)
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
  initialHour: Int,
  initialMinute: Int,
  onConfirm: (Int, Int) -> Unit,
  onDismiss: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val timeState = rememberTimePickerState(
    initialHour = initialHour,
    initialMinute = initialMinute,
    is24Hour = true
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = eqColors.canvasBackground,
    shape = RoundedCornerShape(26.dp),
    title = {
      Text(
        text = "Elige la hora de tu recordatorio",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = eqColors.textPrimary
      )
    },
    text = {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        TimePicker(
          state = timeState,
          colors = TimePickerDefaults.colors(
            clockDialColor = eqColors.surfaceVariant,
            selectorColor = eqColors.accentPrimary,
            timeSelectorSelectedContainerColor = eqColors.accentPrimary,
            timeSelectorSelectedContentColor = eqColors.onAccentPrimary
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(timeState.hour, timeState.minute) },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = eqColors.accentPrimary,
          contentColor = eqColors.onAccentPrimary
        )
      ) {
        Text("Guardar hora", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar", color = eqColors.textSecondary)
      }
    }
  )
}
