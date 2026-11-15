package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BankType {
  MANUAL,
  MONOBANK,
  ABANK,
  ALLIANCE,
  PRIVAT,
  PUMB,
}

enum class TransactionType {
  EXPENSE,
  INCOME,
  TRANSFER,
}

enum class TransactionSource {
  MANUAL,
  MONOBANK,
  NOTIFICATION,
}

enum class BillingCycle {
  WEEKLY,
  MONTHLY,
  YEARLY,
}

enum class ParsingRuleDirection {
  AUTO,
  EXPENSE,
  INCOME,
}

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val emoji: String = "💼",
  val primaryCurrency: String = "UAH",
  val themeJson: String,
  val sortOrder: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "accounts",
  foreignKeys =
    [
      ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("workspaceId"), Index(value = ["externalAccountId"], unique = true)],
)
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val workspaceId: Long,
  val name: String,
  val bankType: String = BankType.MANUAL.name,
  val externalAccountId: String? = null,
  val maskedPan: String = "",
  val balanceMinor: Long = 0L,
  val creditLimitMinor: Long = 0L,
  val currency: String = "UAH",
  val cardColorHex: String = "#10B981",
  val lastSyncedAt: Long = 0L,
)

@Entity(
  tableName = "transactions",
  foreignKeys =
    [
      ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE,
      ),
      ForeignKey(
        entity = AccountEntity::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices = [
    Index("workspaceId"),
    Index("accountId"),
    Index("timestamp"),
    Index(value = ["externalId"], unique = true),
  ],
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val externalId: String? = null,
  val workspaceId: Long,
  val accountId: Long,
  val amountMinor: Long,
  val currency: String = "UAH",
  val type: String = TransactionType.EXPENSE.name,
  val category: String = "OTHER",
  val title: String,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val source: String = TransactionSource.MANUAL.name,
)

@Entity(
  tableName = "goals",
  foreignKeys =
    [
      ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("workspaceId")],
)
data class GoalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val workspaceId: Long,
  val name: String,
  val emoji: String = "🎯",
  val targetAmountMinor: Long,
  val currentAmountMinor: Long = 0L,
  val currency: String = "UAH",
  val colorHex: String = "#10B981",
  val deadlineEpochMillis: Long? = null,
)

@Entity(
  tableName = "subscriptions",
  foreignKeys =
    [
      ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("workspaceId"), Index("accountId")],
)
data class SubscriptionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val workspaceId: Long,
  val accountId: Long? = null,
  val name: String,
  val amountMinor: Long,
  val currency: String = "UAH",
  val billingCycle: String = BillingCycle.MONTHLY.name,
  val nextPaymentEpochMillis: Long,
  val notifyBeforeDays: Int = 2,
  val isActive: Boolean = true,
)

@Entity(
  tableName = "parsing_rules",
  indices = [Index("packageName"), Index("targetAccountId")],
)
data class ParsingRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val packageName: String,
  val targetAccountId: Long? = null,
  val direction: String = ParsingRuleDirection.AUTO.name,
  val regexPattern: String,
  val amountGroupIndex: Int = 1,
  val currencyGroupIndex: Int = 2,
  val merchantGroupIndex: Int = 3,
  val keywordExpense: String = "списання|оплата|покупка|зняття|переказ з|spent|paid|purchase",
  val keywordIncome: String = "зарахування|поповнення|надходження|отримано|credited|received",
  val sampleText: String = "",
  val isEnabled: Boolean = true,
)

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
  @PrimaryKey val pairKey: String,
  val currencyCodeA: Int,
  val currencyCodeB: Int,
  val rateBuy: Double,
  val rateSell: Double,
  val rateCross: Double,
  val updatedAt: Long,
)
