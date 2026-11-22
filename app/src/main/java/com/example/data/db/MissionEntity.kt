package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.HabitScheduleEvaluator
import com.example.model.BalanceMission
import com.example.model.DimensionsProvider

@Entity(tableName = "custom_missions")
data class MissionEntity(
  @PrimaryKey
  val id: String,
  val dimensionIndex: Int,
  val title: String,
  val description: String,
  val benefit: String,
  val durationLabel: String,
  val durationMinutes: Int = 10,
  val frequency: String = "Diariamente",
  val isHabit: Boolean = false,
  val expReward: Int = 30,
  val difficulty: String = "Media",
  val habitDays: String = "",
  val habitEndRule: String = "Nunca",
  val createdAtMillis: Long = 0L
) {
  fun toBalanceMission(): BalanceMission {
    val dim = DimensionsProvider.dimensions.getOrNull(dimensionIndex) ?: DimensionsProvider.dimensions[0]
    val parsedDays = HabitScheduleEvaluator.parseHabitDays(habitDays)
    return BalanceMission(
      id = id,
      dimensionIndex = dimensionIndex,
      dimensionTitle = dim.title,
      title = title,
      summary = description.take(70),
      description = description,
      steps = listOf(
        "Reserva ${durationLabel.lowercase()} sin prisas para esta acción.",
        description,
        "Al terminar, anota cómo te hizo sentir."
      ),
      benefit = benefit,
      durationLabel = durationLabel,
      durationMinutes = durationMinutes,
      frequency = frequency,
      isHabit = isHabit,
      habitDays = parsedDays,
      habitEndRule = habitEndRule,
      baseExpReward = expReward,
      expReward = expReward,
      difficulty = difficulty,
      createdAtMillis = createdAtMillis,
      isCustom = true
    )
  }
}
