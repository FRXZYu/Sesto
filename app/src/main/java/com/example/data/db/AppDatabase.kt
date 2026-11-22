package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    MissionEntity::class,
    CompletionEntity::class,
    DailyPlanEntity::class,
    StreakFreezeEntity::class
  ],
  version = 5,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun missionDao(): MissionDao
  abstract fun completionDao(): CompletionDao
  abstract fun dailyPlanDao(): DailyPlanDao
  abstract fun streakFreezeDao(): StreakFreezeDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE completions ADD COLUMN expAwarded INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_missions ADD COLUMN habitDays TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE custom_missions ADD COLUMN habitEndRule TEXT NOT NULL DEFAULT 'Nunca'")
        db.execSQL("ALTER TABLE custom_missions ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE completions ADD COLUMN countsTowardDailyCap INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS streak_freezes (
            date TEXT NOT NULL PRIMARY KEY,
            expSpent INTEGER NOT NULL,
            createdAtMillis INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "equilibrio_database"
        )
          .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
