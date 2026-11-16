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
        ExtraIncomeEntity::class,
        SplitBillEntity::class,
        SplitParticipantEntity::class,
        SavingsGoalEntity::class,
        SavingsDepositEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class ShiftlyDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun extraIncomeDao(): ExtraIncomeDao
    abstract fun splitBillDao(): SplitBillDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: ShiftlyDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shifts ADD COLUMN dayType TEXT NOT NULL DEFAULT 'ORDINARY'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE split_participants ADD COLUMN isCurrentUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE split_participants SET isCurrentUser = 1 WHERE LOWER(TRIM(personLabel)) = 'you'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS extra_incomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        sourceRuleId TEXT,
                        createdAtMillis INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN linkedSavingsDepositId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        targetAmount REAL NOT NULL,
                        targetDateEpochDay INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAtMillis INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS savings_deposits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        timestampMillis INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(goalId) REFERENCES savings_goals(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_savings_deposits_goalId ON savings_deposits(goalId)"
                )
            }
        }

        fun getInstance(context: Context): ShiftlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShiftlyDatabase::class.java,
                    "shiftly_offline.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
