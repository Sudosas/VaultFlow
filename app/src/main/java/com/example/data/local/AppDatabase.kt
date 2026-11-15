package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities =
    [
      WorkspaceEntity::class,
      AccountEntity::class,
      TransactionEntity::class,
      GoalEntity::class,
      SubscriptionEntity::class,
      ParsingRuleEntity::class,
      CurrencyRateEntity::class,
    ],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun workspaceDao(): WorkspaceDao
  abstract fun accountDao(): AccountDao
  abstract fun transactionDao(): TransactionDao
  abstract fun goalDao(): GoalDao
  abstract fun subscriptionDao(): SubscriptionDao
  abstract fun parsingRuleDao(): ParsingRuleDao
  abstract fun currencyRateDao(): CurrencyRateDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "vaultflow_database.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = false)
              .build()
              .also { instance = it }
        }
  }
}
