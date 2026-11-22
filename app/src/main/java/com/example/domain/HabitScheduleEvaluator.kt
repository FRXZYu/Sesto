package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HabitScheduleStatus(
  val isScheduledToday: Boolean = true,
  val isExpired: Boolean = false,
  val nextAvailableDayLabel: String? = null
)

/**
 * Evalúa de forma pura (100% testeable en JVM) en qué días está activo un hábito
 * y cuándo expira según habitEndRule.
 */
object HabitScheduleEvaluator {

  // Orden estándar de lunes a domingo matching CreateMissionDialog
  private val ORDERED_DAYS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

  private val CALENDAR_TO_DAY_INDEX = mapOf(
    Calendar.MONDAY to 0,
    Calendar.TUESDAY to 1,
    Calendar.WEDNESDAY to 2,
    Calendar.THURSDAY to 3,
    Calendar.FRIDAY to 4,
    Calendar.SATURDAY to 5,
    Calendar.SUNDAY to 6
  )

  fun normalizeDayIndex(raw: String): Int? {
    val clean = raw.trim()
      .lowercase(Locale.ROOT)
      .replace("á", "a")
      .replace("é", "e")
      .replace("í", "i")
      .replace("ó", "o")
      .replace("ú", "u")
    return when (clean) {
      "lun", "lunes", "l" -> 0
      "mar", "martes", "m" -> 1
      "mie", "miercoles", "x" -> 2
      "jue", "jueves", "j" -> 3
      "vie", "viernes", "v" -> 4
      "sab", "sabado", "s" -> 5
      "dom", "domingo", "d" -> 6
      else -> null
    }
  }

  fun parseHabitDays(rawCsv: String): List<String> {
    if (rawCsv.isBlank()) return emptyList()
    return rawCsv.split(",")
      .mapNotNull { token ->
        val idx = normalizeDayIndex(token)
        idx?.let { ORDERED_DAYS[it] }
      }
      .distinct()
  }

  fun serializeHabitDays(days: List<String>): String {
    return days.mapNotNull { normalizeDayIndex(it) }
      .distinct()
      .sorted()
      .joinToString(",") { ORDERED_DAYS[it] }
  }

  fun evaluate(
    isHabit: Boolean,
    habitDays: List<String>,
    habitEndRule: String,
    createdAtMillis: Long,
    dayKey: DayKey = DayKey.DEFAULT
  ): HabitScheduleStatus {
    if (!isHabit) {
      return HabitScheduleStatus(
        isScheduledToday = true,
        isExpired = false,
        nextAvailableDayLabel = null
      )
    }

    val expired = isHabitExpired(habitEndRule, createdAtMillis, dayKey)
    if (expired) {
      return HabitScheduleStatus(
        isScheduledToday = false,
        isExpired = true,
        nextAvailableDayLabel = null
      )
    }

    val allowedIndices = habitDays.mapNotNull { normalizeDayIndex(it) }.toSet()
    if (allowedIndices.isEmpty()) {
      return HabitScheduleStatus(
        isScheduledToday = true,
        isExpired = false,
        nextAvailableDayLabel = null
      )
    }

    val cal = Calendar.getInstance().apply {
      timeInMillis = dayKey.currentMillis()
    }
    val todayIdx = CALENDAR_TO_DAY_INDEX[cal.get(Calendar.DAY_OF_WEEK)] ?: 0
    if (todayIdx in allowedIndices) {
      return HabitScheduleStatus(
        isScheduledToday = true,
        isExpired = false,
        nextAvailableDayLabel = null
      )
    }

    var nextLabel: String? = null
    for (offset in 1..7) {
      val candidateIdx = (todayIdx + offset) % 7
      if (candidateIdx in allowedIndices) {
        nextLabel = ORDERED_DAYS[candidateIdx]
        break
      }
    }

    return HabitScheduleStatus(
      isScheduledToday = false,
      isExpired = false,
      nextAvailableDayLabel = nextLabel
    )
  }

  fun isHabitExpired(
    habitEndRule: String,
    createdAtMillis: Long,
    dayKey: DayKey = DayKey.DEFAULT
  ): Boolean {
    val trimmed = habitEndRule.trim()
    if (trimmed.isEmpty() || trimmed.equals("Nunca", ignoreCase = true)) {
      return false
    }

    val todayKey = dayKey.today()

    // Soporte fecha ISO explícita yyyy-MM-dd
    val isoMatch = Regex("""^\d{4}-\d{2}-\d{2}$""").find(trimmed)
    if (isoMatch != null) {
      return todayKey >= trimmed
    }

    // Reglas relativas a la fecha de creación requieren createdAtMillis > 0
    if (createdAtMillis <= 0L) return false

    val endDateKey = computeEndDateKey(trimmed, createdAtMillis) ?: return false
    return todayKey >= endDateKey
  }

  fun computeEndDateKey(
    habitEndRule: String,
    createdAtMillis: Long
  ): String? {
    val trimmed = habitEndRule.trim()
    if (trimmed.isEmpty() || trimmed.equals("Nunca", ignoreCase = true)) return null
    if (createdAtMillis <= 0L) return null

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // "En 30 días", "En 90 días", "En N días"
    val inDaysMatch = Regex("""^En\s+(\d+)\s+d[ií]as$""", RegexOption.IGNORE_CASE).find(trimmed)
    if (inDaysMatch != null) {
      val days = inDaysMatch.groupValues[1].toIntOrNull() ?: return null
      val cal = Calendar.getInstance().apply {
        timeInMillis = createdAtMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, days)
      }
      return sdf.format(cal.time)
    }

    // "El día 30" (el próximo día N del mes respecto a la fecha de creación)
    val dayOfMonthMatch = Regex("""^El\s+d[ií]a\s+(\d+)$""", RegexOption.IGNORE_CASE).find(trimmed)
    if (dayOfMonthMatch != null) {
      val targetDay = dayOfMonthMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 31) ?: return null
      val cal = Calendar.getInstance().apply {
        timeInMillis = createdAtMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val currentDay = cal.get(Calendar.DAY_OF_MONTH)
      if (currentDay >= targetDay) {
        cal.add(Calendar.MONTH, 1)
      }
      val maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      cal.set(Calendar.DAY_OF_MONTH, minOf(targetDay, maxDayInMonth))
      return sdf.format(cal.time)
    }

    return null
  }
}
