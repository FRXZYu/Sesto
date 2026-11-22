package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakFreezeDao {
  @Query("SELECT * FROM streak_freezes ORDER BY date DESC")
  fun getAllFreezes(): Flow<List<StreakFreezeEntity>>

  @Query("SELECT * FROM streak_freezes ORDER BY date DESC")
  suspend fun getAllFreezesOnce(): List<StreakFreezeEntity>

  @Query("SELECT * FROM streak_freezes WHERE date = :date LIMIT 1")
  suspend fun getFreezeForDate(date: String): StreakFreezeEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertFreeze(freeze: StreakFreezeEntity): Long
}
