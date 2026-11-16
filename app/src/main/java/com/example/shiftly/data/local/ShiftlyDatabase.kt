package com.example.shiftly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ShiftEntity::class,
        ExpenseEntity::class,
        SplitBillEntity::class,
        SplitParticipantEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ShiftlyDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun splitBillDao(): SplitBillDao

    companion object {
        @Volatile
        private var INSTANCE: ShiftlyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shifts ADD COLUMN dayType TEXT NOT NULL DEFAULT 'ORDINARY'")
            }
        }

        fun getInstance(context: Context): ShiftlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShiftlyDatabase::class.java,
                    "shiftly_offline.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}