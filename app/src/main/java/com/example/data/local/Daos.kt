package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
  @Query("SELECT * FROM workspaces ORDER BY sortOrder ASC, createdAt ASC")
  fun observeAll(): Flow<List<WorkspaceEntity>>

  @Query("SELECT * FROM workspaces ORDER BY sortOrder ASC, createdAt ASC")
  suspend fun getAll(): List<WorkspaceEntity>

  @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): WorkspaceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(workspace: WorkspaceEntity): Long

  @Update
  suspend fun update(workspace: WorkspaceEntity)

  @Query("UPDATE workspaces SET themeJson = :themeJson WHERE id = :workspaceId")
  suspend fun updateTheme(workspaceId: Long, themeJson: String)

  @Query("DELETE FROM workspaces WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM workspaces")
  suspend fun deleteAll()
}

@Dao
interface AccountDao {
  @Query("SELECT * FROM accounts ORDER BY id ASC")
  fun observeAll(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts WHERE workspaceId = :workspaceId ORDER BY id ASC")
  fun observeByWorkspace(workspaceId: Long): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts")
  suspend fun getAll(): List<AccountEntity>

  @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): AccountEntity?

  @Query("SELECT * FROM accounts WHERE externalAccountId = :externalId LIMIT 1")
  suspend fun getByExternalId(externalId: String): AccountEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(account: AccountEntity): Long

  @Update
  suspend fun update(account: AccountEntity)

  @Query("UPDATE accounts SET balanceMinor = balanceMinor + :deltaMinor WHERE id = :accountId")
  suspend fun adjustBalance(accountId: Long, deltaMinor: Long)

  @Query("UPDATE accounts SET workspaceId = :workspaceId WHERE id = :accountId")
  suspend fun reassignWorkspace(accountId: Long, workspaceId: Long)

  @Query("DELETE FROM accounts WHERE id = :id")
  suspend fun deleteById(id: Long)
}

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
  fun observeAll(limit: Int = 500): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId ORDER BY timestamp DESC LIMIT :limit")
  fun observeByWorkspace(workspaceId: Long, limit: Int = 500): Flow<List<TransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIgnoreDuplicate(transaction: TransactionEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAllIgnoreDuplicates(transactions: List<TransactionEntity>): List<Long>

  @Query(
    "SELECT COUNT(*) FROM transactions WHERE accountId = :accountId AND amountMinor = :amountMinor AND title = :title AND timestamp >= :sinceMillis"
  )
  suspend fun countRecentSimilar(
    accountId: Long,
    amountMinor: Long,
    title: String,
    sinceMillis: Long,
  ): Int

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): TransactionEntity?

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: Long)
}

@Dao
interface GoalDao {
  @Query("SELECT * FROM goals ORDER BY id ASC")
  fun observeAll(): Flow<List<GoalEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(goal: GoalEntity): Long

  @Query("UPDATE goals SET currentAmountMinor = currentAmountMinor + :deltaMinor WHERE id = :goalId")
  suspend fun addProgress(goalId: Long, deltaMinor: Long)

  @Query("DELETE FROM goals WHERE id = :id")
  suspend fun deleteById(id: Long)
}

@Dao
interface SubscriptionDao {
  @Query("SELECT * FROM subscriptions ORDER BY nextPaymentEpochMillis ASC")
  fun observeAll(): Flow<List<SubscriptionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(subscription: SubscriptionEntity): Long

  @Update
  suspend fun update(subscription: SubscriptionEntity)

  @Query("DELETE FROM subscriptions WHERE id = :id")
  suspend fun deleteById(id: Long)
}

@Dao
interface ParsingRuleDao {
  @Query("SELECT * FROM parsing_rules ORDER BY id ASC")
  fun observeAll(): Flow<List<ParsingRuleEntity>>

  @Query("SELECT * FROM parsing_rules WHERE isEnabled = 1 AND packageName = :packageName")
  suspend fun getEnabledByPackage(packageName: String): List<ParsingRuleEntity>

  @Query("SELECT COUNT(*) FROM parsing_rules")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(rule: ParsingRuleEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(rules: List<ParsingRuleEntity>)

  @Update
  suspend fun update(rule: ParsingRuleEntity)

  @Query("DELETE FROM parsing_rules WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM parsing_rules")
  suspend fun deleteAll()
}

@Dao
interface CurrencyRateDao {
  @Query("SELECT * FROM currency_rates ORDER BY currencyCodeA ASC")
  fun observeAll(): Flow<List<CurrencyRateEntity>>

  @Query("SELECT * FROM currency_rates")
  suspend fun getAll(): List<CurrencyRateEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(rates: List<CurrencyRateEntity>)
}
