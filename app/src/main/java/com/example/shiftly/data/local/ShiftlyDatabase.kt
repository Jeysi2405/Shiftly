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
        RecurringBillEntity::class,
        AllowanceEntryEntity::class,
        SplitBillEntity::class,
        SplitParticipantEntity::class,
        SavingsGoalEntity::class,
        SavingsDepositEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class ShiftlyDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun extraIncomeDao(): ExtraIncomeDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun allowanceDao(): AllowanceDao
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM shifts WHERE id NOT IN (SELECT MAX(id) FROM shifts GROUP BY dateEpochDay)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_shifts_dateEpochDay ON shifts(dateEpochDay)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN linkedRecurringBillId INTEGER")
                db.execSQL("ALTER TABLE expenses ADD COLUMN recurringOccurrenceEpochDay INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_expenses_linkedRecurringBillId ON expenses(linkedRecurringBillId)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        dueEpochDay INTEGER NOT NULL,
                        dueHour INTEGER NOT NULL DEFAULT 9,
                        dueMinute INTEGER NOT NULL DEFAULT 0,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        lastExecutedEpochDay INTEGER NOT NULL DEFAULT 0,
                        lastExecutedAtMillis INTEGER NOT NULL DEFAULT 0,
                        createdAtMillis INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS allowance_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        source TEXT NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        cutoffKey TEXT,
                        createdAtMillis INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_allowance_entries_cutoffKey ON allowance_entries(cutoffKey)"
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
