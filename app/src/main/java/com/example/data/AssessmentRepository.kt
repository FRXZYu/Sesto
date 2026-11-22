package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.analysis.LifeAnalysisEngine
import com.example.model.AssessmentReport
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationPreferences(
  val dailyReminderEnabled: Boolean = false,
  val dailyReminderHour: Int = 20,
  val dailyReminderMinute: Int = 0,
  val streakGuardianEnabled: Boolean = false,
  val hasPromptedNotificationPermission: Boolean = false
)

enum class AppThemeMode {
  SYSTEM,
  LIGHT,
  DARK
}

enum class FirstDayOfWeek {
  MONDAY,
  SUNDAY
}

data class AppPreferences(
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY,
  val dailyGoalCount: Int = 3,
  val confirmHabitCompletion: Boolean = true,
  val accountEmail: String = "",
  val lastGoalChangeReason: String = ""
)

class AssessmentRepository(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("equilibrio_prefs", Context.MODE_PRIVATE)

  private val _savedMissionIdsFlow = MutableStateFlow(
    prefs.getStringSet(KEY_SAVED_MISSION_IDS, emptySet())?.toSet() ?: emptySet()
  )
  val savedMissionIdsFlow: StateFlow<Set<String>> = _savedMissionIdsFlow.asStateFlow()

  private val _chosenFocusDimensionsFlow = MutableStateFlow(readFocusDimensions())
  val chosenFocusDimensionsFlow: StateFlow<Set<Int>> = _chosenFocusDimensionsFlow.asStateFlow()

  private val _baseProfileFlow = MutableStateFlow(readBaseProfile())
  val baseProfileFlow: StateFlow<UserProfile> = _baseProfileFlow.asStateFlow()

  private val _acceptedWeeklyChallengeIdFlow = MutableStateFlow(
    prefs.getString(KEY_ACCEPTED_WEEKLY_CHALLENGE_ID, null)
  )
  val acceptedWeeklyChallengeIdFlow: StateFlow<String?> = _acceptedWeeklyChallengeIdFlow.asStateFlow()

  private val _notificationPrefsFlow = MutableStateFlow(readNotificationPreferences())
  val notificationPrefsFlow: StateFlow<NotificationPreferences> = _notificationPrefsFlow.asStateFlow()

  private val _appPrefsFlow = MutableStateFlow(readAppPreferences())
  val appPrefsFlow: StateFlow<AppPreferences> = _appPrefsFlow.asStateFlow()

  private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
    _savedMissionIdsFlow.value = prefs.getStringSet(KEY_SAVED_MISSION_IDS, emptySet())?.toSet() ?: emptySet()
    _chosenFocusDimensionsFlow.value = readFocusDimensions()
    _baseProfileFlow.value = readBaseProfile()
    _acceptedWeeklyChallengeIdFlow.value = prefs.getString(KEY_ACCEPTED_WEEKLY_CHALLENGE_ID, null)
    _notificationPrefsFlow.value = readNotificationPreferences()
    _appPrefsFlow.value = readAppPreferences()
  }

  init {
    prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
  }

  companion object {
    private const val KEY_IS_COMPLETED = "key_is_completed"
    private const val KEY_CURRENT_PREFIX = "key_curr_score_"
    private const val KEY_PREV_PREFIX = "key_prev_score_"
    private const val KEY_HAS_PREVIOUS = "key_has_previous"
    private const val KEY_LAST_UPDATED = "key_last_updated"
    private const val KEY_SAVED_MISSION_IDS = "key_saved_mission_ids"
    private const val KEY_FOCUS_DIMENSIONS = "key_focus_dimensions"

    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_PHOTO = "key_user_photo"
    private const val KEY_USER_ZODIAC = "key_user_zodiac"
    private const val KEY_USER_ZODIAC_SYMBOL = "key_user_zodiac_symbol"
    private const val KEY_USER_GOAL = "key_user_goal"
    private const val KEY_PROFILE_COMPLETED = "key_profile_completed"
    private const val DEFAULT_GOAL = "Cuidarme con pasos pequeños y realistas"

    private const val KEY_ACCEPTED_WEEKLY_CHALLENGE_ID = "key_accepted_weekly_challenge_id"
    private const val KEY_DAILY_REMINDER_ENABLED = "key_daily_reminder_enabled"
    private const val KEY_DAILY_REMINDER_HOUR = "key_daily_reminder_hour"
    private const val KEY_DAILY_REMINDER_MINUTE = "key_daily_reminder_minute"
    private const val KEY_STREAK_GUARDIAN_ENABLED = "key_streak_guardian_enabled"
    private const val KEY_NOTIFICATION_PERMISSION_PROMPTED = "key_notification_permission_prompted"
    private const val KEY_HAS_SIGNED_IN = "key_has_signed_in"

    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_FIRST_DAY_OF_WEEK = "key_first_day_of_week"
    private const val KEY_DAILY_GOAL_COUNT = "key_daily_goal_count"
    private const val KEY_CONFIRM_HABIT_COMPLETION = "key_confirm_habit_completion"
    private const val KEY_ACCOUNT_EMAIL = "key_account_email"
    private const val KEY_GOAL_CHANGE_REASON = "key_goal_change_reason"
  }

  fun hasCompletedAssessment(): Boolean {
    return prefs.getBoolean(KEY_IS_COMPLETED, false)
  }

  fun hasPreviousAssessment(): Boolean {
    return prefs.getBoolean(KEY_HAS_PREVIOUS, false)
  }

  fun saveScores(newScores: Map<Int, Int>) {
    val existingCurrent = loadCurrentScores()
    val editor = prefs.edit()

    if (existingCurrent != null && existingCurrent.size == 6) {
      existingCurrent.forEach { (index, score) ->
        editor.putInt("$KEY_PREV_PREFIX$index", score)
      }
      editor.putBoolean(KEY_HAS_PREVIOUS, true)
    } else if (!hasPreviousAssessment()) {
      for (i in 0..5) {
        editor.putInt("$KEY_PREV_PREFIX$i", 5)
      }
      editor.putBoolean(KEY_HAS_PREVIOUS, true)
    }

    newScores.forEach { (index, score) ->
      editor.putInt("$KEY_CURRENT_PREFIX$index", score)
    }
    editor.putBoolean(KEY_IS_COMPLETED, true)
    editor.putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
    editor.apply()
  }

  fun loadCurrentScores(): Map<Int, Int>? {
    if (!hasCompletedAssessment()) return null
    val result = mutableMapOf<Int, Int>()
    for (i in 0..5) {
      val score = prefs.getInt("$KEY_CURRENT_PREFIX$i", -1)
      if (score != -1) {
        result[i] = score
      }
    }
    return if (result.size == 6) result else null
  }

  fun loadPreviousScores(): Map<Int, Int>? {
    if (!hasPreviousAssessment()) return null
    val result = mutableMapOf<Int, Int>()
    for (i in 0..5) {
      val score = prefs.getInt("$KEY_PREV_PREFIX$i", -1)
      if (score != -1) {
        result[i] = score
      }
    }
    return if (result.size == 6) result else null
  }

  fun getSavedReport(): AssessmentReport? {
    val current = loadCurrentScores() ?: return null
    val previous = loadPreviousScores()
    return LifeAnalysisEngine.generateReport(current, previous)
  }

  fun resetAssessment() {
    prefs.edit().clear().apply()
    _savedMissionIdsFlow.value = emptySet()
    _chosenFocusDimensionsFlow.value = emptySet()
    _baseProfileFlow.value = readBaseProfile()
    _acceptedWeeklyChallengeIdFlow.value = null
    _notificationPrefsFlow.value = readNotificationPreferences()
    _appPrefsFlow.value = readAppPreferences()
  }

  // ==========================================
  // Reto de la semana aceptado
  // ==========================================

  fun acceptWeeklyChallenge(missionId: String) {
    prefs.edit().putString(KEY_ACCEPTED_WEEKLY_CHALLENGE_ID, missionId).apply()
    _acceptedWeeklyChallengeIdFlow.value = missionId
  }

  fun getAcceptedWeeklyChallengeId(): String? {
    return prefs.getString(KEY_ACCEPTED_WEEKLY_CHALLENGE_ID, null)
  }

  // ==========================================
  // Preferencias de notificaciones y recordatorios
  // ==========================================

  fun readNotificationPreferences(): NotificationPreferences {
    return NotificationPreferences(
      dailyReminderEnabled = prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, false),
      dailyReminderHour = prefs.getInt(KEY_DAILY_REMINDER_HOUR, 20).coerceIn(0, 23),
      dailyReminderMinute = prefs.getInt(KEY_DAILY_REMINDER_MINUTE, 0).coerceIn(0, 59),
      streakGuardianEnabled = prefs.getBoolean(KEY_STREAK_GUARDIAN_ENABLED, false),
      hasPromptedNotificationPermission = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, false)
    )
  }

  fun updateNotificationPreferences(update: NotificationPreferences) {
    val sanitizedHour = update.dailyReminderHour.coerceIn(0, 23)
    val sanitizedMinute = update.dailyReminderMinute.coerceIn(0, 59)
    val clean = update.copy(
      dailyReminderHour = sanitizedHour,
      dailyReminderMinute = sanitizedMinute
    )
    prefs.edit()
      .putBoolean(KEY_DAILY_REMINDER_ENABLED, clean.dailyReminderEnabled)
      .putInt(KEY_DAILY_REMINDER_HOUR, clean.dailyReminderHour)
      .putInt(KEY_DAILY_REMINDER_MINUTE, clean.dailyReminderMinute)
      .putBoolean(KEY_STREAK_GUARDIAN_ENABLED, clean.streakGuardianEnabled)
      .putBoolean(KEY_NOTIFICATION_PERMISSION_PROMPTED, clean.hasPromptedNotificationPermission)
      .apply()
    _notificationPrefsFlow.value = clean
  }

  // ==========================================
  // Áreas de enfoque elegidas por la persona
  // ==========================================

  private fun readFocusDimensions(): Set<Int> {
    val raw = prefs.getString(KEY_FOCUS_DIMENSIONS, "") ?: ""
    if (raw.isBlank()) return emptySet()
    return raw.split(",")
      .mapNotNull { it.trim().toIntOrNull() }
      .filter { it in 0..5 }
      .toSet()
  }

  fun saveFocusDimensions(dimensions: Set<Int>) {
    val clean = dimensions.filter { it in 0..5 }.toSet()
    val serialized = clean.sorted().joinToString(",")
    prefs.edit().putString(KEY_FOCUS_DIMENSIONS, serialized).apply()
    _chosenFocusDimensionsFlow.value = clean
    _baseProfileFlow.value = _baseProfileFlow.value.copy(chosenFocusDimensions = clean)
  }

  fun toggleFocusDimension(dimensionIndex: Int): Set<Int> {
    if (dimensionIndex !in 0..5) return _chosenFocusDimensionsFlow.value
    val updated = _chosenFocusDimensionsFlow.value.toMutableSet()
    if (updated.contains(dimensionIndex)) {
      updated.remove(dimensionIndex)
    } else {
      updated.add(dimensionIndex)
    }
    saveFocusDimensions(updated)
    return updated
  }

  // ==========================================
  // Acciones guardadas para más tarde
  // ==========================================

  fun toggleSaveMissionForLater(missionId: String): Boolean {
    val savedIds = (prefs.getStringSet(KEY_SAVED_MISSION_IDS, emptySet()) ?: emptySet()).toMutableSet()
    val isNowSaved = if (savedIds.contains(missionId)) {
      savedIds.remove(missionId)
      false
    } else {
      savedIds.add(missionId)
      true
    }
    prefs.edit().putStringSet(KEY_SAVED_MISSION_IDS, savedIds).apply()
    _savedMissionIdsFlow.value = savedIds.toSet()
    return isNowSaved
  }

  fun removeSavedMission(missionId: String) {
    val savedIds = (prefs.getStringSet(KEY_SAVED_MISSION_IDS, emptySet()) ?: emptySet()).toMutableSet()
    if (savedIds.remove(missionId)) {
      prefs.edit().putStringSet(KEY_SAVED_MISSION_IDS, savedIds).apply()
      _savedMissionIdsFlow.value = savedIds.toSet()
    }
  }

  // ==========================================
  // Perfil sereno del usuario
  // ==========================================

  fun hasSignedIn(): Boolean {
    return prefs.getBoolean(KEY_HAS_SIGNED_IN, false)
  }

  fun markSignedIn() {
    prefs.edit().putBoolean(KEY_HAS_SIGNED_IN, true).apply()
  }

  fun isProfileCompleted(): Boolean {
    return prefs.getBoolean(KEY_PROFILE_COMPLETED, false)
  }

  fun saveProfile(profile: UserProfile) {
    val cleanFocus = profile.chosenFocusDimensions.filter { it in 0..5 }.toSet()
    prefs.edit()
      .putString(KEY_USER_NAME, profile.name)
      .putString(KEY_USER_PHOTO, profile.photoUri)
      .putString(KEY_USER_ZODIAC, profile.zodiacSign.ifBlank { "Aries" })
      .putString(KEY_USER_ZODIAC_SYMBOL, profile.zodiacSymbol.ifBlank { "♈" })
      .putString(KEY_USER_GOAL, profile.monthlyGoal.ifBlank { DEFAULT_GOAL })
      .putString(KEY_FOCUS_DIMENSIONS, cleanFocus.sorted().joinToString(","))
      .putBoolean(KEY_PROFILE_COMPLETED, true)
      .apply()
    _chosenFocusDimensionsFlow.value = cleanFocus
    _baseProfileFlow.value = readBaseProfile()
  }

  fun updateMonthlyGoal(goal: String, reason: String = "") {
    val sanitized = goal.trim().ifBlank { DEFAULT_GOAL }
    val sanitizedReason = reason.trim()
    val editor = prefs.edit().putString(KEY_USER_GOAL, sanitized)
    if (sanitizedReason.isNotEmpty()) {
      editor.putString(KEY_GOAL_CHANGE_REASON, sanitizedReason)
    }
    editor.apply()
    _baseProfileFlow.value = _baseProfileFlow.value.copy(monthlyGoal = sanitized)
    if (sanitizedReason.isNotEmpty()) {
      _appPrefsFlow.value = _appPrefsFlow.value.copy(lastGoalChangeReason = sanitizedReason)
    }
  }

  fun updateProfileIdentity(name: String, zodiacSign: String, zodiacSymbol: String) {
    val cleanName = name.trim()
    val cleanZodiac = zodiacSign.trim().ifBlank { "Aries" }
    val cleanSymbol = zodiacSymbol.trim().ifBlank { "♈" }
    prefs.edit()
      .putString(KEY_USER_NAME, cleanName)
      .putString(KEY_USER_ZODIAC, cleanZodiac)
      .putString(KEY_USER_ZODIAC_SYMBOL, cleanSymbol)
      .apply()
    _baseProfileFlow.value = _baseProfileFlow.value.copy(
      name = cleanName,
      zodiacSign = cleanZodiac,
      zodiacSymbol = cleanSymbol
    )
  }

  fun readAppPreferences(): AppPreferences {
    val themeModeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
    val themeMode = runCatching { AppThemeMode.valueOf(themeModeName ?: AppThemeMode.SYSTEM.name) }
      .getOrDefault(AppThemeMode.SYSTEM)
    val firstDayName = prefs.getString(KEY_FIRST_DAY_OF_WEEK, FirstDayOfWeek.MONDAY.name)
    val firstDay = runCatching { FirstDayOfWeek.valueOf(firstDayName ?: FirstDayOfWeek.MONDAY.name) }
      .getOrDefault(FirstDayOfWeek.MONDAY)
    val dailyGoal = prefs.getInt(KEY_DAILY_GOAL_COUNT, 3).coerceIn(1, 6)
    val confirmHabits = prefs.getBoolean(KEY_CONFIRM_HABIT_COMPLETION, true)
    val email = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: ""
    val reason = prefs.getString(KEY_GOAL_CHANGE_REASON, "") ?: ""
    return AppPreferences(
      themeMode = themeMode,
      firstDayOfWeek = firstDay,
      dailyGoalCount = dailyGoal,
      confirmHabitCompletion = confirmHabits,
      accountEmail = email,
      lastGoalChangeReason = reason
    )
  }

  fun updateAppPreferences(update: AppPreferences) {
    val clean = update.copy(
      dailyGoalCount = update.dailyGoalCount.coerceIn(1, 6),
      accountEmail = update.accountEmail.trim(),
      lastGoalChangeReason = update.lastGoalChangeReason.trim()
    )
    prefs.edit()
      .putString(KEY_THEME_MODE, clean.themeMode.name)
      .putString(KEY_FIRST_DAY_OF_WEEK, clean.firstDayOfWeek.name)
      .putInt(KEY_DAILY_GOAL_COUNT, clean.dailyGoalCount)
      .putBoolean(KEY_CONFIRM_HABIT_COMPLETION, clean.confirmHabitCompletion)
      .putString(KEY_ACCOUNT_EMAIL, clean.accountEmail)
      .putString(KEY_GOAL_CHANGE_REASON, clean.lastGoalChangeReason)
      .apply()
    _appPrefsFlow.value = clean
  }

  fun setThemeMode(mode: AppThemeMode) {
    updateAppPreferences(readAppPreferences().copy(themeMode = mode))
  }

  fun setFirstDayOfWeek(firstDay: FirstDayOfWeek) {
    updateAppPreferences(readAppPreferences().copy(firstDayOfWeek = firstDay))
  }

  fun setDailyGoalCount(count: Int) {
    updateAppPreferences(readAppPreferences().copy(dailyGoalCount = count.coerceIn(1, 6)))
  }

  fun setConfirmHabitCompletion(enabled: Boolean) {
    updateAppPreferences(readAppPreferences().copy(confirmHabitCompletion = enabled))
  }

  fun saveAccountEmail(email: String) {
    updateAppPreferences(readAppPreferences().copy(accountEmail = email.trim()))
  }

  private fun readBaseProfile(): UserProfile {
    val name = prefs.getString(KEY_USER_NAME, "") ?: ""
    val photo = prefs.getString(KEY_USER_PHOTO, null)
    val zodiac = prefs.getString(KEY_USER_ZODIAC, "Aries") ?: "Aries"
    val zodiacSymbol = prefs.getString(KEY_USER_ZODIAC_SYMBOL, "♈") ?: "♈"
    val goal = prefs.getString(KEY_USER_GOAL, DEFAULT_GOAL) ?: DEFAULT_GOAL
    val focus = readFocusDimensions()
    return UserProfile(
      name = name,
      photoUri = photo,
      zodiacSign = zodiac,
      zodiacSymbol = zodiacSymbol,
      monthlyGoal = goal,
      chosenFocusDimensions = focus
    )
  }

  fun loadProfile(): UserProfile = readBaseProfile()

  fun restoreUserDataFromCloud(cloudData: Map<String, Any>) {
    val name = cloudData["name"] as? String ?: ""
    val photoUri = cloudData["photoUri"] as? String
    val zodiacSign = cloudData["zodiacSign"] as? String ?: "Aries"
    val zodiacSymbol = cloudData["zodiacSymbol"] as? String ?: "♈"
    val monthlyGoal = cloudData["monthlyGoal"] as? String ?: DEFAULT_GOAL
    val email = cloudData["email"] as? String ?: ""

    @Suppress("UNCHECKED_CAST")
    val focusList = (cloudData["chosenFocusDimensions"] as? List<Long>)?.map { it.toInt() }?.toSet()
      ?: (cloudData["chosenFocusDimensions"] as? List<Int>)?.toSet() ?: emptySet()

    val profile = UserProfile(
      name = name,
      photoUri = photoUri,
      zodiacSign = zodiacSign,
      zodiacSymbol = zodiacSymbol,
      monthlyGoal = monthlyGoal,
      chosenFocusDimensions = focusList
    )
    saveProfile(profile)
    saveAccountEmail(email)
    markSignedIn()

    @Suppress("UNCHECKED_CAST")
    val currentMap = cloudData["currentScores"] as? Map<String, Any>
    if (currentMap != null && currentMap.isNotEmpty()) {
      val scores = mutableMapOf<Int, Int>()
      currentMap.forEach { (k, v) ->
        val kInt = k.toIntOrNull()
        val vInt = (v as? Number)?.toInt()
        if (kInt != null && vInt != null) {
          scores[kInt] = vInt
        }
      }
      if (scores.size == 6) {
        saveScores(scores)
      }
    }
  }
}
