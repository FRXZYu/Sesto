package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.AppThemeMode
import com.example.data.FirstDayOfWeek
import com.example.data.NotificationPreferences
import com.example.model.AllZodiacSigns
import com.example.model.UserProfile
import com.example.notifications.NotificationHelper
import com.example.ui.components.NotificationsSettingsCard
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.EditorialSerif
import com.example.ui.theme.equilibrioColors
import java.util.Locale
import kotlinx.coroutines.delay

enum class SettingsSection(
  val title: String,
  val subtitle: String,
  val icon: ImageVector
) {
  APPEARANCE(
    title = "Apariencia",
    subtitle = "Tema claro, oscuro o automático",
    icon = Icons.Outlined.Palette
  ),
  NOTIFICATIONS(
    title = "Notificaciones",
    subtitle = "Hora de avisos y gestión",
    icon = Icons.Outlined.Notifications
  ),
  HABITS(
    title = "Hábitos",
    subtitle = "Inicio de semana, objetivo diario y confirmación",
    icon = Icons.Outlined.EventRepeat
  ),
  SYNC(
    title = "Sincronización",
    subtitle = "Correo registrado de tu cuenta",
    icon = Icons.Outlined.Sync
  ),
  PROFILE(
    title = "Perfil y Meta",
    subtitle = "Nombre, signo y meta del mes",
    icon = Icons.Outlined.Person
  ),
  ABOUT(
    title = "Acerca de",
    subtitle = "Dedicatoria especial e información",
    icon = Icons.Outlined.FavoriteBorder
  )
}

@Composable
fun SettingsScreen(
  userProfile: UserProfile,
  appPreferences: AppPreferences,
  notificationPreferences: NotificationPreferences,
  isFrozenToday: Boolean,
  freezeMessage: String?,
  onBack: () -> Unit,
  onUpdateAppPreferences: (AppPreferences) -> Unit,
  onUpdateNotificationPreferences: (NotificationPreferences) -> Unit,
  onFreezeStreak: () -> Unit,
  onUpdateProfileIdentity: (name: String, zodiacSign: String, zodiacSymbol: String) -> Unit,
  onOpenEditMonthlyGoal: () -> Unit,
  onShowFeedback: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current
  var activeSection by remember { mutableStateOf<SettingsSection?>(null) }

  BackHandler {
    if (activeSection != null) {
      activeSection = null
    } else {
      onBack()
    }
  }

  Surface(
    color = eqColors.canvasBackground,
    modifier = modifier
      .fillMaxSize()
      .testTag("settings_screen_root")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Encabezado superior con botón atrás elástico
      SettingsTopBar(
        title = activeSection?.title ?: "Ajustes",
        subtitle = activeSection?.subtitle ?: "Personaliza tu espacio en Equilibrio",
        onBackClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          if (activeSection != null) {
            activeSection = null
          } else {
            onBack()
          }
        }
      )

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
      ) {
        AnimatedContent(
          targetState = activeSection,
          transitionSpec = {
            if (targetState != null) {
              (slideInHorizontally(
                animationSpec = spring(
                  dampingRatio = 0.82f,
                  stiffness = Spring.StiffnessMediumLow
                )
              ) { it / 3 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally { -it / 4 } + fadeOut(tween(160)))
            } else {
              (slideInHorizontally(
                animationSpec = spring(
                  dampingRatio = 0.82f,
                  stiffness = Spring.StiffnessMediumLow
                )
              ) { -it / 3 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally { it / 4 } + fadeOut(tween(160)))
            }
          },
          label = "settings_section_transition"
        ) { section ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              when (section) {
                null -> {
                  SettingsHubList(
                    userProfile = userProfile,
                    appPreferences = appPreferences,
                    notificationPreferences = notificationPreferences,
                    onSelectSection = { selected ->
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      activeSection = selected
                    }
                  )
                }

                SettingsSection.APPEARANCE -> {
                  AppearanceDetailScreen(
                    currentMode = appPreferences.themeMode,
                    onSelectMode = { mode ->
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      onUpdateAppPreferences(appPreferences.copy(themeMode = mode))
                    }
                  )
                }

                SettingsSection.NOTIFICATIONS -> {
                  NotificationsDetailScreen(
                    prefs = notificationPreferences,
                    activeStreakDays = userProfile.activeStreakDays,
                    totalExp = userProfile.exp,
                    isFrozenToday = isFrozenToday,
                    freezeMessage = freezeMessage,
                    onUpdatePrefs = onUpdateNotificationPreferences,
                    onFreezeStreak = onFreezeStreak
                  )
                }

                SettingsSection.HABITS -> {
                  HabitsDetailScreen(
                    appPreferences = appPreferences,
                    onUpdateAppPreferences = { updated ->
                      onUpdateAppPreferences(updated)
                    }
                  )
                }

                SettingsSection.SYNC -> {
                  SyncDetailScreen(
                    accountEmail = appPreferences.accountEmail,
                    completedMissionsCount = userProfile.completedMissions,
                    onSaveEmail = { newEmail ->
                      onUpdateAppPreferences(appPreferences.copy(accountEmail = newEmail))
                      onShowFeedback("Correo de sincronización actualizado")
                    }
                  )
                }

                SettingsSection.PROFILE -> {
                  ProfileAndGoalDetailScreen(
                    userProfile = userProfile,
                    lastGoalChangeReason = appPreferences.lastGoalChangeReason,
                    onSaveIdentity = { name, zodiacSign, zodiacSymbol ->
                      onUpdateProfileIdentity(name, zodiacSign, zodiacSymbol)
                      onShowFeedback("Perfil actualizado")
                    },
                    onOpenEditMonthlyGoal = onOpenEditMonthlyGoal
                  )
                }

                SettingsSection.ABOUT -> {
                  AboutDetailScreen()
                }
              }

              Spacer(modifier = Modifier.height(24.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsTopBar(
  title: String,
  subtitle: String,
  onBackClick: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val backInteraction = rememberBounceInteractionSource()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      onClick = onBackClick,
      interactionSource = backInteraction,
      shape = CircleShape,
      color = eqColors.surfaceCard,
      border = BorderStroke(1.dp, eqColors.border),
      modifier = Modifier
        .size(48.dp)
        .bouncyScale(backInteraction, scaleDown = 0.88f)
        .testTag("btn_settings_back")
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Volver",
          tint = eqColors.textPrimary,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp
        ),
        color = eqColors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = eqColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SettingsHubList(
  userProfile: UserProfile,
  appPreferences: AppPreferences,
  notificationPreferences: NotificationPreferences,
  onSelectSection: (SettingsSection) -> Unit
) {
  val formattedTime = String.format(
    Locale.US,
    "%02d:%02d",
    notificationPreferences.dailyReminderHour,
    notificationPreferences.dailyReminderMinute
  )

  val sectionBadges = mapOf(
    SettingsSection.APPEARANCE to when (appPreferences.themeMode) {
      AppThemeMode.LIGHT -> "Claro"
      AppThemeMode.DARK -> "Oscuro"
      AppThemeMode.SYSTEM -> "Sistema"
    },
    SettingsSection.NOTIFICATIONS to if (notificationPreferences.dailyReminderEnabled) {
      formattedTime
    } else {
      "En pausa"
    },
    SettingsSection.HABITS to "${appPreferences.dailyGoalCount}/día",
    SettingsSection.SYNC to appPreferences.accountEmail.ifBlank { "Sin correo" },
    SettingsSection.PROFILE to userProfile.name.ifBlank { "Tu perfil" },
    SettingsSection.ABOUT to "v1.0"
  )

  SettingsSection.entries.forEachIndexed { index, section ->
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(section) {
      delay(index * 45L)
      visible = true
    }

    AnimatedVisibility(
      visible = visible,
      enter = fadeIn(tween(240)) + scaleIn(
        initialScale = 0.94f,
        animationSpec = spring(
          dampingRatio = 0.72f,
          stiffness = Spring.StiffnessMediumLow
        )
      )
    ) {
      SettingsHubCard(
        section = section,
        badgeLabel = sectionBadges[section].orEmpty(),
        onClick = { onSelectSection(section) }
      )
    }
  }
}

@Composable
private fun SettingsHubCard(
  section: SettingsSection,
  badgeLabel: String,
  onClick: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val interaction = rememberBounceInteractionSource()

  Surface(
    onClick = onClick,
    interactionSource = interaction,
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .bouncyScale(interaction, scaleDown = 0.96f)
      .testTag("settings_section_${section.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 15.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(eqColors.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = section.icon,
          contentDescription = null,
          tint = eqColors.accentPrimary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = section.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          ),
          color = eqColors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = section.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = eqColors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (badgeLabel.isNotBlank()) {
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = eqColors.surfaceVariant
        ) {
          Text(
            text = badgeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = eqColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .widthIn(max = 110.dp)
              .padding(horizontal = 9.dp, vertical = 5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = "Abrir ${section.title}",
        tint = eqColors.textTertiary,
        modifier = Modifier.size(22.dp)
      )
    }
  }
}

// ==========================================
// 1. APARIENCIA: CLARO O OSCURO (+ SISTEMA)
// ==========================================
@Composable
private fun AppearanceDetailScreen(
  currentMode: AppThemeMode,
  onSelectMode: (AppThemeMode) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors

  Text(
    text = "Elige cómo se siente mejor tu espacio",
    style = MaterialTheme.typography.bodyMedium,
    color = eqColors.textSecondary
  )

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ThemeModeOptionCard(
      title = "Claro",
      description = "Lienzo editorial luminoso y limpio",
      icon = Icons.Outlined.LightMode,
      isSelected = currentMode == AppThemeMode.LIGHT,
      onClick = { onSelectMode(AppThemeMode.LIGHT) },
      modifier = Modifier
        .weight(1f)
        .testTag("btn_theme_light")
    )

    ThemeModeOptionCard(
      title = "Oscuro",
      description = "Paleta serena para descansar la vista",
      icon = Icons.Outlined.DarkMode,
      isSelected = currentMode == AppThemeMode.DARK,
      onClick = { onSelectMode(AppThemeMode.DARK) },
      modifier = Modifier
        .weight(1f)
        .testTag("btn_theme_dark")
    )
  }

  val systemInteraction = rememberBounceInteractionSource()
  val isSystemSelected = currentMode == AppThemeMode.SYSTEM
  val systemBg by animateColorAsState(
    targetValue = if (isSystemSelected) eqColors.heroSurface else eqColors.surfaceCard,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "theme_system_bg"
  )

  Surface(
    onClick = { onSelectMode(AppThemeMode.SYSTEM) },
    interactionSource = systemInteraction,
    shape = RoundedCornerShape(20.dp),
    color = systemBg,
    border = BorderStroke(
      width = if (isSystemSelected) 1.5.dp else 1.dp,
      color = if (isSystemSelected) eqColors.accentPrimary else eqColors.border
    ),
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .bouncyScale(systemInteraction, scaleDown = 0.96f)
      .testTag("btn_theme_system")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Outlined.Contrast,
        contentDescription = null,
        tint = if (isSystemSelected) eqColors.onHeroSurface else eqColors.accentPrimary,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Automático del sistema",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = if (isSystemSelected) eqColors.onHeroSurface else eqColors.textPrimary
        )
        Text(
          text = "Se adapta al modo claro u oscuro de tu dispositivo",
          style = MaterialTheme.typography.bodySmall,
          color = if (isSystemSelected) eqColors.onHeroSurface.copy(alpha = 0.78f) else eqColors.textSecondary
        )
      }
      if (isSystemSelected) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = "Seleccionado",
          tint = eqColors.onHeroSurface,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Composable
private fun ThemeModeOptionCard(
  title: String,
  description: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val interaction = rememberBounceInteractionSource()
  val bg by animateColorAsState(
    targetValue = if (isSelected) eqColors.heroSurface else eqColors.surfaceCard,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "theme_card_$title"
  )
  val checkScale by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "theme_check_$title"
  )

  Surface(
    onClick = onClick,
    interactionSource = interaction,
    shape = RoundedCornerShape(22.dp),
    color = bg,
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) eqColors.accentPrimary else eqColors.border
    ),
    shadowElevation = if (isSelected) 3.dp else 1.dp,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .bouncyScale(interaction, scaleDown = 0.94f)
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
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
              if (isSelected) eqColors.onHeroSurface.copy(alpha = 0.14f) else eqColors.surfaceVariant
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) eqColors.onHeroSurface else eqColors.accentPrimary,
            modifier = Modifier.size(22.dp)
          )
        }

        if (isSelected) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Seleccionado",
            tint = eqColors.onHeroSurface,
            modifier = Modifier
              .size(20.dp)
              .graphicsLayer {
                scaleX = checkScale
                scaleY = checkScale
              }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = if (isSelected) eqColors.onHeroSurface else eqColors.textPrimary
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = if (isSelected) eqColors.onHeroSurface.copy(alpha = 0.78f) else eqColors.textSecondary,
        lineHeight = 18.sp
      )
    }
  }
}

// ==========================================
// 2. NOTIFICACIONES: HORA Y GESTIÓN COMPLETA
// ==========================================
@Composable
private fun NotificationsDetailScreen(
  prefs: NotificationPreferences,
  activeStreakDays: Int,
  totalExp: Int,
  isFrozenToday: Boolean,
  freezeMessage: String?,
  onUpdatePrefs: (NotificationPreferences) -> Unit,
  onFreezeStreak: () -> Unit
) {
  val context = LocalContext.current
  val eqColors = MaterialTheme.equilibrioColors
  val hasPermission = remember(prefs) { NotificationHelper.hasNotificationPermission(context) }
  val formattedTime = String.format(Locale.US, "%02d:%02d", prefs.dailyReminderHour, prefs.dailyReminderMinute)

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = if (prefs.dailyReminderEnabled && hasPermission) eqColors.successContainer else eqColors.surfaceVariant,
    border = BorderStroke(
      1.dp,
      if (prefs.dailyReminderEnabled && hasPermission) eqColors.successBorder else eqColors.border
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = if (prefs.dailyReminderEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Schedule,
        contentDescription = null,
        tint = if (prefs.dailyReminderEnabled && hasPermission) eqColors.successText else eqColors.accentPrimary,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = if (prefs.dailyReminderEnabled) {
            "Recordatorio programado a las $formattedTime"
          } else {
            "Recordatorio diario en pausa"
          },
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = if (prefs.dailyReminderEnabled && hasPermission) eqColors.successStrongText else eqColors.textPrimary
        )
        Text(
          text = "Configura aquí exactamente a qué hora te llegan los avisos y actívalos o apágalos por separado.",
          style = MaterialTheme.typography.bodySmall,
          color = if (prefs.dailyReminderEnabled && hasPermission) eqColors.successBodyText else eqColors.textSecondary
        )
      }
    }
  }

  NotificationsSettingsCard(
    prefs = prefs,
    activeStreakDays = activeStreakDays,
    totalExp = totalExp,
    isFrozenToday = isFrozenToday,
    freezeMessage = freezeMessage,
    onUpdatePrefs = onUpdatePrefs,
    onFreezeStreak = onFreezeStreak
  )
}

// ==========================================
// 3. HÁBITOS: DÍA INICIAL, OBJETIVO DIARIO Y CONFIRMACIÓN
// ==========================================
@Composable
private fun HabitsDetailScreen(
  appPreferences: AppPreferences,
  onUpdateAppPreferences: (AppPreferences) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current

  // 3a. Primer día de la semana (Domingo o Lunes)
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.CalendarToday,
          contentDescription = null,
          tint = eqColors.accentPrimary,
          modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Primer día de la semana",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Define cómo se ordena tu calendario semanal en Plan.",
        style = MaterialTheme.typography.bodySmall,
        color = eqColors.textSecondary
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        WeekStartOptionCard(
          label = "Domingo",
          sequence = "D · L · M · X · J · V · S",
          isSelected = appPreferences.firstDayOfWeek == FirstDayOfWeek.SUNDAY,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onUpdateAppPreferences(appPreferences.copy(firstDayOfWeek = FirstDayOfWeek.SUNDAY))
          },
          modifier = Modifier
            .weight(1f)
            .testTag("btn_first_day_sunday")
        )

        WeekStartOptionCard(
          label = "Lunes",
          sequence = "L · M · X · J · V · S · D",
          isSelected = appPreferences.firstDayOfWeek == FirstDayOfWeek.MONDAY,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onUpdateAppPreferences(appPreferences.copy(firstDayOfWeek = FirstDayOfWeek.MONDAY))
          },
          modifier = Modifier
            .weight(1f)
            .testTag("btn_first_day_monday")
        )
      }
    }
  }

  // 3b. Objetivo diario (NO la meta del mes)
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Objetivo diario",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textPrimary
          )
          Text(
            text = "Cantidad de misiones o hábitos sugeridos cada día (independiente de tu meta mensual).",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = eqColors.heroSurface
        ) {
          Text(
            text = "${appPreferences.dailyGoalCount} / día",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontFeatureSettings = "tnum"
            ),
            color = eqColors.onHeroSurface,
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 7.dp)
              .testTag("txt_daily_goal_count")
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val minusInteraction = rememberBounceInteractionSource()
        val plusInteraction = rememberBounceInteractionSource()

        Surface(
          onClick = {
            if (appPreferences.dailyGoalCount > 1) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onUpdateAppPreferences(
                appPreferences.copy(dailyGoalCount = appPreferences.dailyGoalCount - 1)
              )
            }
          },
          enabled = appPreferences.dailyGoalCount > 1,
          interactionSource = minusInteraction,
          shape = CircleShape,
          color = eqColors.surfaceVariant,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier
            .size(48.dp)
            .bouncyScale(minusInteraction, scaleDown = 0.88f)
            .testTag("btn_daily_goal_minus")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.Remove,
              contentDescription = "Disminuir objetivo diario",
              tint = if (appPreferences.dailyGoalCount > 1) eqColors.textPrimary else eqColors.textTertiary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          (1..6).forEach { count ->
            val selected = appPreferences.dailyGoalCount == count
            val chipInteraction = rememberBounceInteractionSource()
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onUpdateAppPreferences(appPreferences.copy(dailyGoalCount = count))
              },
              interactionSource = chipInteraction,
              shape = RoundedCornerShape(12.dp),
              color = if (selected) eqColors.accentPrimary else eqColors.surfaceVariant,
              border = BorderStroke(1.dp, if (selected) eqColors.accentPrimary else eqColors.border),
              modifier = Modifier
                .size(36.dp)
                .bouncyScale(chipInteraction, scaleDown = 0.88f)
                .testTag("chip_daily_goal_$count")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "$count",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                  ),
                  color = if (selected) eqColors.onAccentPrimary else eqColors.textPrimary
                )
              }
            }
          }
        }

        Surface(
          onClick = {
            if (appPreferences.dailyGoalCount < 6) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onUpdateAppPreferences(
                appPreferences.copy(dailyGoalCount = appPreferences.dailyGoalCount + 1)
              )
            }
          },
          enabled = appPreferences.dailyGoalCount < 6,
          interactionSource = plusInteraction,
          shape = CircleShape,
          color = eqColors.surfaceVariant,
          border = BorderStroke(1.dp, eqColors.border),
          modifier = Modifier
            .size(48.dp)
            .bouncyScale(plusInteraction, scaleDown = 0.88f)
            .testTag("btn_daily_goal_plus")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Aumentar objetivo diario",
              tint = if (appPreferences.dailyGoalCount < 6) eqColors.textPrimary else eqColors.textTertiary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }

  // 3c. Confirmación de hábitos (encendida por defecto)
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(eqColors.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Confirmación de hábitos",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textPrimary
          )
          Text(
            text = "Preguntar antes de marcar un hábito como completado para evitar toques accidentales.",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      Switch(
        checked = appPreferences.confirmHabitCompletion,
        onCheckedChange = { enabled ->
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onUpdateAppPreferences(appPreferences.copy(confirmHabitCompletion = enabled))
        },
        colors = SwitchDefaults.colors(
          checkedThumbColor = eqColors.onAccentPrimary,
          checkedTrackColor = eqColors.accentPrimary
        ),
        modifier = Modifier.testTag("switch_confirm_habits")
      )
    }
  }
}

@Composable
private fun WeekStartOptionCard(
  label: String,
  sequence: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val interaction = rememberBounceInteractionSource()
  val bg by animateColorAsState(
    targetValue = if (isSelected) eqColors.heroSurface else eqColors.surfaceVariant,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "week_start_bg_$label"
  )

  Surface(
    onClick = onClick,
    interactionSource = interaction,
    shape = RoundedCornerShape(16.dp),
    color = bg,
    border = BorderStroke(1.dp, if (isSelected) eqColors.accentPrimary else eqColors.border),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .bouncyScale(interaction, scaleDown = 0.94f)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = if (isSelected) eqColors.onHeroSurface else eqColors.textPrimary
        )
        if (isSelected) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Seleccionado",
            tint = eqColors.onHeroSurface,
            modifier = Modifier.size(16.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = sequence,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
        color = if (isSelected) eqColors.onHeroSurface.copy(alpha = 0.78f) else eqColors.textSecondary
      )
    }
  }
}

// ==========================================
// 4. SINCRONIZACIÓN: CORREO REGISTRADO
// ==========================================
@Composable
private fun SyncDetailScreen(
  accountEmail: String,
  completedMissionsCount: Int,
  onSaveEmail: (String) -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current
  var emailInput by remember(accountEmail) { mutableStateOf(accountEmail) }
  val saveInteraction = rememberBounceInteractionSource()

  val isValidEmail = remember(emailInput) {
    val trimmed = emailInput.trim()
    trimmed.isNotEmpty() && trimmed.contains("@") && trimmed.contains(".")
  }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(eqColors.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.CloudDone,
            contentDescription = null,
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Cuenta vinculada",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textPrimary
          )
          Text(
            text = if (accountEmail.isNotBlank()) accountEmail else "Aún sin correo registrado",
            style = MaterialTheme.typography.bodySmall,
            color = eqColors.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = eqColors.border)
      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Correo electrónico registrado",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = eqColors.textPrimary
      )
      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = emailInput,
        onValueChange = { emailInput = it },
        placeholder = { Text("tucorreo@ejemplo.com") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Outlined.AlternateEmail,
            contentDescription = null,
            tint = eqColors.accentPrimary
          )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = eqColors.accentPrimary,
          unfocusedBorderColor = eqColors.border
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_sync_email")
      )

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onSaveEmail(emailInput.trim())
        },
        enabled = isValidEmail,
        interactionSource = saveInteraction,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = eqColors.accentPrimary,
          contentColor = eqColors.onAccentPrimary
        ),
        contentPadding = PaddingValues(vertical = 13.dp),
        modifier = Modifier
          .fillMaxWidth()
          .bouncyScale(saveInteraction, scaleDown = 0.95f)
          .testTag("btn_save_sync_email")
      ) {
        Icon(
          imageVector = Icons.Outlined.Sync,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Guardar correo de cuenta",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = eqColors.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Tu progreso local ($completedMissionsCount acciones registradas) está respaldado en este dispositivo y asociado a tu cuenta.",
          style = MaterialTheme.typography.bodySmall,
          color = eqColors.textSecondary,
          modifier = Modifier.padding(12.dp)
        )
      }
    }
  }
}

// ==========================================
// 5. PERFIL Y META MENSUAL
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileAndGoalDetailScreen(
  userProfile: UserProfile,
  lastGoalChangeReason: String,
  onSaveIdentity: (name: String, zodiacSign: String, zodiacSymbol: String) -> Unit,
  onOpenEditMonthlyGoal: () -> Unit
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current
  var editedName by remember(userProfile.name) { mutableStateOf(userProfile.name) }
  var selectedZodiac by remember(userProfile.zodiacSign) {
    mutableStateOf(
      AllZodiacSigns.firstOrNull { it.name.equals(userProfile.zodiacSign, ignoreCase = true) }
        ?: AllZodiacSigns.first()
    )
  }

  val saveIdentityInteraction = rememberBounceInteractionSource()
  val editGoalInteraction = rememberBounceInteractionSource()

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Tu identidad",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = eqColors.textPrimary
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = editedName,
        onValueChange = { editedName = it },
        label = { Text("Nombre") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = eqColors.accentPrimary,
          unfocusedBorderColor = eqColors.border
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_settings_profile_name")
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Signo zodiacal",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = eqColors.textPrimary
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AllZodiacSigns.forEach { sign ->
          val isSelected = sign.name == selectedZodiac.name
          val chipInteraction = rememberBounceInteractionSource()
          Surface(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              selectedZodiac = sign
            },
            interactionSource = chipInteraction,
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) eqColors.accentPrimary else eqColors.surfaceVariant,
            border = BorderStroke(1.dp, if (isSelected) eqColors.accentPrimary else eqColors.border),
            modifier = Modifier.bouncyScale(chipInteraction, scaleDown = 0.92f)
          ) {
            Text(
              text = "${sign.symbol} ${sign.name}",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              ),
              color = if (isSelected) eqColors.onAccentPrimary else eqColors.textPrimary,
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onSaveIdentity(editedName.trim(), selectedZodiac.name, selectedZodiac.symbol)
        },
        enabled = editedName.isNotBlank(),
        interactionSource = saveIdentityInteraction,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = eqColors.accentPrimary,
          contentColor = eqColors.onAccentPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .bouncyScale(saveIdentityInteraction, scaleDown = 0.95f)
          .testTag("btn_save_profile_identity")
      ) {
        Text("Guardar perfil", fontWeight = FontWeight.Bold)
      }
    }
  }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Flag,
          contentDescription = null,
          tint = eqColors.accentPrimary,
          modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Meta del mes actual",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = eqColors.textPrimary
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = userProfile.monthlyGoal.ifBlank { "Quiero ser mi mejor versión" },
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        color = eqColors.textPrimary
      )
      if (lastGoalChangeReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Último motivo del cambio: \"$lastGoalChangeReason\"",
          style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
          color = eqColors.textSecondary
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onOpenEditMonthlyGoal()
        },
        interactionSource = editGoalInteraction,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, eqColors.accentPrimary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.accentPrimary),
        modifier = Modifier
          .fillMaxWidth()
          .bouncyScale(editGoalInteraction, scaleDown = 0.94f)
          .testTag("btn_settings_edit_monthly_goal")
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Cambiar meta del mes", fontWeight = FontWeight.Bold)
      }
    }
  }
}

// ==========================================
// 6. ACERCA DE: DEDICATORIA ESPECIAL
// ==========================================
@Composable
private fun AboutDetailScreen() {
  val eqColors = MaterialTheme.equilibrioColors
  val pulseTransition = rememberInfiniteTransition(label = "about_heart_pulse")
  val heartScale by pulseTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "heart_scale"
  )

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_about_dedication")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        shape = CircleShape,
        color = eqColors.alertBadge,
        modifier = Modifier.size(64.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Con cariño",
            tint = eqColors.alertText,
            modifier = Modifier
              .size(30.dp)
              .graphicsLayer {
                scaleX = heartScale
                scaleY = heartScale
              }
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "Equilibrio",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontFamily = EditorialSerif,
          fontWeight = FontWeight.Bold,
          fontSize = 28.sp
        ),
        color = eqColors.textPrimary
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Esta aplicación está única y diseñadamente para Kirenia Gutierrez, con cariño: Juan Burdick",
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = EditorialSerif,
          fontSize = 19.sp,
          lineHeight = 29.sp,
          fontWeight = FontWeight.Medium
        ),
        color = eqColors.textPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = eqColors.surfaceVariant
      ) {
        Text(
          text = "Versión 1.0 · Hecho con calma y detalle",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = eqColors.textSecondary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }
    }
  }
}
