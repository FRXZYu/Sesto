package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro diario de congelación de racha canjeada con 100 EXP.
 * La clave primaria por fecha ("yyyy-MM-dd") garantiza máximo 1 congelación por día.
 */
@Entity(tableName = "streak_freezes")
data class StreakFreezeEntity(
  @PrimaryKey
  val date: String,
  val expSpent: Int = 100,
  val createdAtMillis: Long = System.currentTimeMillis()
)
