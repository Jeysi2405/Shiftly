package com.example.shiftly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ShiftEntity::class,
        ExpenseEntity::class,
        SplitBillEntity::class,
        SplitParticipantEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShiftlyDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun splitBillDao(): SplitBillDao

    companion object {
        @Volatile
        private var INSTANCE: ShiftlyDatabase? = null

        fun getInstance(context: Context): ShiftlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShiftlyDatabase::class.java,
                    "shiftly_offline.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}