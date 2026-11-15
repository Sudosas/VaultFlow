package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.data.local.BankType
import com.example.data.local.BillingCycle
import com.example.data.local.CurrencyRateEntity
import com.example.data.local.GoalEntity
import com.example.data.local.ParsingRuleEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionSource
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.data.local.WorkspaceEntity
import com.example.data.parser.BankNotificationParser
import com.example.data.remote.IsoCurrencyMapper
import com.example.data.remote.MonobankApiService
import com.example.ui.theme.ThemePresets
import com.example.ui.theme.WorkspaceThemeConfig
import com.example.ui.theme.WorkspaceThemeJson
import com.example.util.MoneyFormatter
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.abs

sealed class SyncResult {
  data class Success(val accountsUpdated: Int, val newTransactions: Int) : SyncResult()
  data class RateLimited(val remainingSeconds: Int) : SyncResult()
  data class Error(val message: String) : SyncResult()
}

class VaultRepository(
  private val db: AppDatabase,
  private val prefs: UserPreferences,
  private val monobankApi: MonobankApiService,
) {
  private val workspaceDao = db.workspaceDao()
  private val accountDao = db.accountDao()
  private val transactionDao = db.transactionDao()
  private val goalDao = db.goalDao()
  private val subscriptionDao = db.subscriptionDao()
  private val parsingRuleDao = db.parsingRuleDao()
  private val currencyRateDao = db.currencyRateDao()

  val settingsFlow: Flow<AppSettings> = prefs.settingsFlow
  val workspacesFlow: Flow<List<WorkspaceEntity>> = workspaceDao.observeAll()
  val accountsFlow: Flow<List<AccountEntity>> = accountDao.observeAll()
  val transactionsFlow: Flow<List<TransactionEntity>> = transactionDao.observeAll()
  val goalsFlow: Flow<List<GoalEntity>> = goalDao.observeAll()
  val subscriptionsFlow: Flow<List<SubscriptionEntity>> = subscriptionDao.observeAll()
  val parsingRulesFlow: Flow<List<ParsingRuleEntity>> = parsingRuleDao.observeAll()
  val currencyRatesFlow: Flow<List<CurrencyRateEntity>> = currencyRateDao.observeAll()

  suspend fun createWorkspace(
    name: String,
    emoji: String,
    primaryCurrency: String,
    themeConfig: WorkspaceThemeConfig = ThemePresets.minimalDark,
    initialBalanceMinor: Long = 0L,
    createDefaultAccount: Boolean = true,
    defaultAccountName: String = "Main Card",
  ): Long {
    val currency = primaryCurrency.uppercase()
    val existing = workspaceDao.getAll()
    val id =
      workspaceDao.insert(
        WorkspaceEntity(
          name = name.trim(),
          emoji = emoji.ifBlank { "💼" },
          primaryCurrency = currency,
          themeJson = WorkspaceThemeJson.encode(themeConfig),
          sortOrder = existing.size,
        )
      )
    if (createDefaultAccount) {
      accountDao.insert(
        AccountEntity(
          workspaceId = id,
          name = defaultAccountName,
          bankType = BankType.MANUAL.name,
          balanceMinor = initialBalanceMinor,
          currency = currency,
          cardColorHex = themeConfig.accentColorHex,
        )
      )
    }
    prefs.setSelectedWorkspaceId(id)
    return id
  }

  suspend fun updateWorkspace(workspace: WorkspaceEntity) = workspaceDao.update(workspace)

  suspend fun updateWorkspaceTheme(workspaceId: Long, config: WorkspaceThemeConfig) {
    workspaceDao.updateTheme(workspaceId, WorkspaceThemeJson.encode(config))
  }

  suspend fun deleteWorkspace(workspaceId: Long) {
    workspaceDao.deleteById(workspaceId)
    val current = prefs.getCurrentSettings().selectedWorkspaceId
    if (current == workspaceId) {
      prefs.setSelectedWorkspaceId(AppSettings.ALL_WORKSPACES_ID)
    }
  }

  suspend fun addAccount(account: AccountEntity): Long = accountDao.insert(account)

  suspend fun reassignAccountWorkspace(accountId: Long, workspaceId: Long) =
    accountDao.reassignWorkspace(accountId, workspaceId)

  suspend fun deleteAccount(accountId: Long) = accountDao.deleteById(accountId)

  suspend fun addManualTransaction(
    workspaceId: Long,
    accountId: Long,
    targetAccountId: Long? = null,
    amountMinor: Long,
    currency: String,
    type: TransactionType,
    category: String,
    title: String,
    note: String,
  ) {
    if (amountMinor <= 0L) return
    db.withTransaction {
      val now = System.currentTimeMillis()
      when (type) {
        TransactionType.EXPENSE -> {
          transactionDao.insertIgnoreDuplicate(
            TransactionEntity(
              workspaceId = workspaceId,
              accountId = accountId,
              amountMinor = amountMinor,
              currency = currency,
              type = TransactionType.EXPENSE.name,
              category = category,
              title = title,
              note = note,
              timestamp = now,
              source = TransactionSource.MANUAL.name,
            )
          )
          accountDao.adjustBalance(accountId, -amountMinor)
        }
        TransactionType.INCOME -> {
          transactionDao.insertIgnoreDuplicate(
            TransactionEntity(
              workspaceId = workspaceId,
              accountId = accountId,
              amountMinor = amountMinor,
              currency = currency,
              type = TransactionType.INCOME.name,
              category = category,
              title = title,
              note = note,
              timestamp = now,
              source = TransactionSource.MANUAL.name,
            )
          )
          accountDao.adjustBalance(accountId, amountMinor)
        }
        TransactionType.TRANSFER -> {
          val destAccount = targetAccountId?.let { accountDao.getById(it) }
          transactionDao.insertIgnoreDuplicate(
            TransactionEntity(
              workspaceId = workspaceId,
              accountId = accountId,
              amountMinor = amountMinor,
              currency = currency,
              type = TransactionType.TRANSFER.name,
              category = "TRANSFER",
              title = title.ifBlank { destAccount?.name ?: "Transfer" },
              note = note,
              timestamp = now,
              source = TransactionSource.MANUAL.name,
            )
          )
          accountDao.adjustBalance(accountId, -amountMinor)
          if (destAccount != null && destAccount.id != accountId) {
            accountDao.adjustBalance(destAccount.id, amountMinor)
          }
        }
      }
    }
  }

  suspend fun deleteTransaction(transaction: TransactionEntity) {
    db.withTransaction {
      val delta =
        when (transaction.type) {
          TransactionType.INCOME.name -> -transaction.amountMinor
          TransactionType.EXPENSE.name -> transaction.amountMinor
          else -> 0L
        }
      if (delta != 0L) {
        accountDao.adjustBalance(transaction.accountId, delta)
      }
      transactionDao.deleteById(transaction.id)
    }
  }

  suspend fun addGoal(goal: GoalEntity): Long = goalDao.insert(goal)

  suspend fun topUpGoal(goalId: Long, amountMinor: Long, deductAccountId: Long?) {
    if (amountMinor <= 0L) return
    db.withTransaction {
      goalDao.addProgress(goalId, amountMinor)
      if (deductAccountId != null) {
        val account = accountDao.getById(deductAccountId)
        if (account != null) {
          accountDao.adjustBalance(account.id, -amountMinor)
          transactionDao.insertIgnoreDuplicate(
            TransactionEntity(
              workspaceId = account.workspaceId,
              accountId = account.id,
              amountMinor = amountMinor,
              currency = account.currency,
              type = TransactionType.EXPENSE.name,
              category = "TRANSFER",
              title = "Envelope Top-Up",
              timestamp = System.currentTimeMillis(),
              source = TransactionSource.MANUAL.name,
            )
          )
        }
      }
    }
  }

  suspend fun deleteGoal(goalId: Long) = goalDao.deleteById(goalId)

  suspend fun addSubscription(subscription: SubscriptionEntity): Long =
    subscriptionDao.insert(subscription)

  suspend fun markSubscriptionPaid(sub: SubscriptionEntity) {
    db.withTransaction {
      val targetAccount = sub.accountId?.let { accountDao.getById(it) }
      if (targetAccount != null) {
        accountDao.adjustBalance(targetAccount.id, -sub.amountMinor)
        transactionDao.insertIgnoreDuplicate(
          TransactionEntity(
            workspaceId = targetAccount.workspaceId,
            accountId = targetAccount.id,
            amountMinor = sub.amountMinor,
            currency = sub.currency,
            type = TransactionType.EXPENSE.name,
            category = "TECH",
            title = sub.name,
            note = "Subscription payment",
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.MANUAL.name,
          )
        )
      }
      val cal = Calendar.getInstance().apply { timeInMillis = sub.nextPaymentEpochMillis }
      when (sub.billingCycle) {
        BillingCycle.WEEKLY.name -> cal.add(Calendar.DAY_OF_YEAR, 7)
        BillingCycle.YEARLY.name -> cal.add(Calendar.YEAR, 1)
        else -> cal.add(Calendar.MONTH, 1)
      }
      subscriptionDao.update(sub.copy(nextPaymentEpochMillis = cal.timeInMillis))
    }
  }

  suspend fun deleteSubscription(subId: Long) = subscriptionDao.deleteById(subId)

  suspend fun saveParsingRule(rule: ParsingRuleEntity): Long = parsingRuleDao.insert(rule)

  suspend fun toggleParsingRule(rule: ParsingRuleEntity, enabled: Boolean) =
    parsingRuleDao.update(rule.copy(isEnabled = enabled))

  suspend fun assignBankPushToWorkspace(rule: ParsingRuleEntity, workspaceId: Long) {
    db.withTransaction {
      val wsAccounts = accountDao.getAll().filter { it.workspaceId == workspaceId }
      val bankHint =
        when {
          rule.packageName.contains("abank") -> BankType.ABANK
          rule.packageName.contains("alliance") -> BankType.ALLIANCE
          rule.packageName.contains("privat") -> BankType.PRIVAT
          rule.packageName.contains("pumb") -> BankType.PUMB
          else -> BankType.MANUAL
        }
      val existingAcc =
        wsAccounts.firstOrNull { it.bankType == bankHint.name } ?: wsAccounts.firstOrNull()
      val targetAccId =
        existingAcc?.id
          ?: accountDao.insert(
            AccountEntity(
              workspaceId = workspaceId,
              name = rule.name.substringBefore(" (").ifBlank { bankHint.name },
              bankType = bankHint.name,
              balanceMinor = 0L,
              currency = workspaceDao.getById(workspaceId)?.primaryCurrency ?: "UAH",
              cardColorHex =
                when (bankHint) {
                  BankType.ABANK -> "#059669"
                  BankType.ALLIANCE -> "#2563EB"
                  BankType.PRIVAT -> "#16A34A"
                  BankType.PUMB -> "#DC2626"
                  else -> "#10B981"
                },
            )
          )
      parsingRuleDao.update(rule.copy(targetAccountId = targetAccId, isEnabled = true))
    }
  }

  suspend fun deleteParsingRule(ruleId: Long) = parsingRuleDao.deleteById(ruleId)

  suspend fun loadUkrainianParserPresets() {
    val accounts = accountDao.getAll()
    val abankId = accounts.firstOrNull { it.bankType == BankType.ABANK.name }?.id
    val allianceId = accounts.firstOrNull { it.bankType == BankType.ALLIANCE.name }?.id
    parsingRuleDao.insertAll(BankNotificationParser.defaultUkrainianPresets(abankId, allianceId))
  }

  suspend fun processIncomingNotification(
    packageName: String,
    notificationText: String,
    postTimeMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val rules = parsingRuleDao.getEnabledByPackage(packageName)
    for (rule in rules) {
      val parsed = BankNotificationParser.parse(notificationText, rule) ?: continue
      val account =
        rule.targetAccountId?.let { accountDao.getById(it) }
          ?: accountDao.getAll().firstOrNull()
          ?: return false

      // Deduplicate identical push messages within 15 seconds
      val recentDupes =
        transactionDao.countRecentSimilar(
          accountId = account.id,
          amountMinor = parsed.amountMinor,
          title = parsed.merchant,
          sinceMillis = postTimeMillis - 15_000L,
        )
      if (recentDupes > 0) return false

      db.withTransaction {
        val delta =
          if (parsed.transactionType == TransactionType.INCOME) parsed.amountMinor
          else -parsed.amountMinor
        transactionDao.insertIgnoreDuplicate(
          TransactionEntity(
            externalId = "notif_${packageName}_${postTimeMillis}_${parsed.amountMinor}",
            workspaceId = account.workspaceId,
            accountId = account.id,
            amountMinor = parsed.amountMinor,
            currency = parsed.currency,
            type = parsed.transactionType.name,
            category = if (parsed.transactionType == TransactionType.INCOME) "SALARY" else "OTHER",
            title = parsed.merchant,
            note = notificationText.take(140),
            timestamp = postTimeMillis,
            source = TransactionSource.NOTIFICATION.name,
          )
        )
        accountDao.adjustBalance(account.id, delta)
      }
      return true
    }
    return false
  }

  suspend fun refreshCurrencyRates(): Boolean {
    return runCatching {
        val remote = monobankApi.getCurrencyRates()
        val now = System.currentTimeMillis()
        val entities =
          remote.map {
            CurrencyRateEntity(
              pairKey = "${it.currencyCodeA}_${it.currencyCodeB}",
              currencyCodeA = it.currencyCodeA,
              currencyCodeB = it.currencyCodeB,
              rateBuy = it.rateBuy,
              rateSell = it.rateSell,
              rateCross = it.rateCross,
              updatedAt = now,
            )
          }
        currencyRateDao.upsertAll(entities)
        true
      }
      .getOrDefault(false)
  }

  suspend fun syncMonobank(tokenOverride: String? = null, syncStatements: Boolean = true): SyncResult {
    val settings = prefs.getCurrentSettings()
    val token = (tokenOverride ?: settings.monobankToken).trim()
    if (token.isEmpty()) return SyncResult.Error("Monobank X-Token is empty")

    val now = System.currentTimeMillis()
    val elapsed = now - settings.lastMonoSyncEpochMillis
    if (elapsed in 0 until 60_000L) {
      val remaining = ((60_000L - elapsed) / 1000L).toInt().coerceAtLeast(1)
      return SyncResult.RateLimited(remaining)
    }

    return try {
      prefs.setMonobankToken(token)
      prefs.setLastMonoSync(now)

      val clientInfo = monobankApi.getClientInfo(token)
      var workspaces = workspaceDao.getAll()
      val defaultWorkspaceId =
        if (workspaces.isEmpty()) {
          createWorkspace("Monobank", "🐈", "UAH", ThemePresets.minimalDark)
        } else if (settings.selectedWorkspaceId > 0L) {
          settings.selectedWorkspaceId
        } else {
          workspaces.first().id
        }

      var accountsUpdated = 0
      val importedAccountIds = mutableListOf<Pair<AccountEntity, String>>()

      for (monoAcc in clientInfo.accounts) {
        val existing = accountDao.getByExternalId(monoAcc.id)
        val currencyIso = IsoCurrencyMapper.toIso(monoAcc.currencyCode)
        val pan = monoAcc.maskedPan.firstOrNull().orEmpty()
        val title =
          when (monoAcc.type) {
            "black" -> "Monobank Black ($currencyIso)"
            "white" -> "Monobank White ($currencyIso)"
            "platinum" -> "Monobank Platinum ($currencyIso)"
            "iron" -> "Monobank Iron ($currencyIso)"
            "fop" -> "Monobank FOP ($currencyIso)"
            "eAid" -> "Monobank єПідтримка"
            else -> "Monobank ${monoAcc.type.replaceFirstChar { it.uppercase() }}"
          }

        val savedAccount =
          if (existing != null) {
            val updated =
              existing.copy(
                balanceMinor = monoAcc.balance,
                creditLimitMinor = monoAcc.creditLimit,
                maskedPan = pan.ifBlank { existing.maskedPan },
                lastSyncedAt = now,
              )
            accountDao.update(updated)
            updated
          } else {
            val inserted =
              AccountEntity(
                workspaceId = defaultWorkspaceId,
                name = title,
                bankType = BankType.MONOBANK.name,
                externalAccountId = monoAcc.id,
                maskedPan = pan,
                balanceMinor = monoAcc.balance,
                creditLimitMinor = monoAcc.creditLimit,
                currency = currencyIso,
                cardColorHex = if (monoAcc.type == "white") "#E2E8F0" else "#1E293B",
                lastSyncedAt = now,
              )
            val newId = accountDao.insert(inserted)
            inserted.copy(id = newId)
          }
        accountsUpdated++
        importedAccountIds.add(savedAccount to monoAcc.id)
      }

      var newTransactions = 0
      if (syncStatements && importedAccountIds.isNotEmpty()) {
        // Fetch statement for the primary Monobank account (to respect 1 req / 60s limit cleanly)
        val (primaryAcc, monoId) = importedAccountIds.first()
        val toSec = now / 1000L
        val fromSec = toSec - 7 * 24 * 3600L
        val statement = monobankApi.getStatement(token, monoId, fromSec, toSec)
        val entities =
          statement.map { item ->
            val isIncome = item.amount > 0L
            TransactionEntity(
              externalId = "mono_${item.id}",
              workspaceId = primaryAcc.workspaceId,
              accountId = primaryAcc.id,
              amountMinor = abs(item.amount),
              currency = IsoCurrencyMapper.toIso(item.currencyCode),
              type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
              category = MoneyFormatter.mapMccToCategory(item.mcc, item.amount),
              title = item.description.ifBlank { "Monobank" },
              note = item.comment.orEmpty(),
              timestamp = item.time * 1000L,
              source = TransactionSource.MONOBANK.name,
            )
          }
        val results = transactionDao.insertAllIgnoreDuplicates(entities)
        newTransactions = results.count { it != -1L }
      }

      refreshCurrencyRates()
      SyncResult.Success(accountsUpdated, newTransactions)
    } catch (e: retrofit2.HttpException) {
      if (e.code() == 429) SyncResult.RateLimited(60)
      else SyncResult.Error("HTTP ${e.code()}: ${e.message()}")
    } catch (e: Exception) {
      SyncResult.Error(e.localizedMessage ?: "Monobank sync failed")
    }
  }

  suspend fun ensureDefaultParsingRules() {
    if (parsingRuleDao.count() == 0) {
      loadUkrainianParserPresets()
    }
  }

  suspend fun populateDemoData() {
    db.withTransaction {
      workspaceDao.deleteAll()
      parsingRuleDao.deleteAll()

      val now = System.currentTimeMillis()
      val dayMs = 86_400_000L

      // 1. Моно Побут (Obsidian Fintech)
      val monoWsId =
        workspaceDao.insert(
          WorkspaceEntity(
            name = "Моно Побут",
            emoji = "🍔",
            primaryCurrency = "UAH",
            themeJson = WorkspaceThemeJson.encode(ThemePresets.obsidian),
            sortOrder = 0,
          )
        )
      val monoBlackId =
        accountDao.insert(
          AccountEntity(
            workspaceId = monoWsId,
            name = "Monobank Black",
            bankType = BankType.MONOBANK.name,
            externalAccountId = "demo_mono_black",
            maskedPan = "537541******8421",
            balanceMinor = 42_850_40L,
            creditLimitMinor = 50_000_00L,
            currency = "UAH",
            cardColorHex = "#111827",
            lastSyncedAt = now,
          )
        )
      val monoWhiteId =
        accountDao.insert(
          AccountEntity(
            workspaceId = monoWsId,
            name = "Monobank Біла",
            bankType = BankType.MONOBANK.name,
            externalAccountId = "demo_mono_white",
            maskedPan = "444111******1904",
            balanceMinor = 12_400_00L,
            currency = "UAH",
            cardColorHex = "#334155",
            lastSyncedAt = now,
          )
        )

      // 2. А-Банк Робота (Cyberpunk Neon)
      val abankWsId =
        workspaceDao.insert(
          WorkspaceEntity(
            name = "А-Банк Робота",
            emoji = "💼",
            primaryCurrency = "USD",
            themeJson = WorkspaceThemeJson.encode(ThemePresets.cyberpunkNeon),
            sortOrder = 1,
          )
        )
      val abankUsdId =
        accountDao.insert(
          AccountEntity(
            workspaceId = abankWsId,
            name = "А-Банк Зелена USD",
            bankType = BankType.ABANK.name,
            maskedPan = "414960******3318",
            balanceMinor = 3_420_50L,
            currency = "USD",
            cardColorHex = "#065F46",
            lastSyncedAt = now,
          )
        )
      val abankFopId =
        accountDao.insert(
          AccountEntity(
            workspaceId = abankWsId,
            name = "ФОП Рахунок",
            bankType = BankType.ABANK.name,
            maskedPan = "2600******7712",
            balanceMinor = 96_500_00L,
            currency = "UAH",
            cardColorHex = "#4C1D95",
            lastSyncedAt = now,
          )
        )

      // 3. Альянс Резерв (Paper Light)
      val allianceWsId =
        workspaceDao.insert(
          WorkspaceEntity(
            name = "Альянс Резерв",
            emoji = "🛡️",
            primaryCurrency = "EUR",
            themeJson = WorkspaceThemeJson.encode(ThemePresets.paperLight),
            sortOrder = 2,
          )
        )
      val allianceEurId =
        accountDao.insert(
          AccountEntity(
            workspaceId = allianceWsId,
            name = "Alliance Целева EUR",
            bankType = BankType.ALLIANCE.name,
            maskedPan = "516875******4412",
            balanceMinor = 2_180_00L,
            currency = "EUR",
            cardColorHex = "#1D4ED8",
            lastSyncedAt = now,
          )
        )

      // Transactions
      val demoTx =
        listOf(
          TransactionEntity(
            externalId = "demo_1",
            workspaceId = monoWsId,
            accountId = monoBlackId,
            amountMinor = 845_50L,
            currency = "UAH",
            type = TransactionType.EXPENSE.name,
            category = "GROCERIES",
            title = "Сільпо",
            note = "Продукти на тиждень",
            timestamp = now - 2 * 3600_000L,
            source = TransactionSource.MONOBANK.name,
          ),
          TransactionEntity(
            externalId = "demo_2",
            workspaceId = monoWsId,
            accountId = monoBlackId,
            amountMinor = 145_00L,
            currency = "UAH",
            type = TransactionType.EXPENSE.name,
            category = "DINING",
            title = "IDEALIST Coffee",
            timestamp = now - 7 * 3600_000L,
            source = TransactionSource.MONOBANK.name,
          ),
          TransactionEntity(
            externalId = "demo_3",
            workspaceId = monoWsId,
            accountId = monoWhiteId,
            amountMinor = 32_000_00L,
            currency = "UAH",
            type = TransactionType.INCOME.name,
            category = "SALARY",
            title = "Виплата на побут",
            timestamp = now - 2 * dayMs,
            source = TransactionSource.MONOBANK.name,
          ),
          TransactionEntity(
            externalId = "demo_4",
            workspaceId = monoWsId,
            accountId = monoBlackId,
            amountMinor = 2_340_00L,
            currency = "UAH",
            type = TransactionType.EXPENSE.name,
            category = "UTILITIES",
            title = "YASNO Електроенергія",
            timestamp = now - 3 * dayMs,
            source = TransactionSource.MANUAL.name,
          ),
          TransactionEntity(
            externalId = "demo_5",
            workspaceId = monoWsId,
            accountId = monoBlackId,
            amountMinor = 380_00L,
            currency = "UAH",
            type = TransactionType.EXPENSE.name,
            category = "TRANSPORT",
            title = "Uklon",
            timestamp = now - 4 * dayMs,
            source = TransactionSource.MONOBANK.name,
          ),
          TransactionEntity(
            externalId = "demo_6",
            workspaceId = abankWsId,
            accountId = abankUsdId,
            amountMinor = 1_850_00L,
            currency = "USD",
            type = TransactionType.INCOME.name,
            category = "SALARY",
            title = "Client Invoice #42",
            note = "Mobile UI Milestone",
            timestamp = now - 1 * dayMs,
            source = TransactionSource.NOTIFICATION.name,
          ),
          TransactionEntity(
            externalId = "demo_7",
            workspaceId = abankWsId,
            accountId = abankUsdId,
            amountMinor = 49_00L,
            currency = "USD",
            type = TransactionType.EXPENSE.name,
            category = "TECH",
            title = "JetBrains & GitHub",
            timestamp = now - 2 * dayMs,
            source = TransactionSource.NOTIFICATION.name,
          ),
          TransactionEntity(
            externalId = "demo_8",
            workspaceId = abankWsId,
            accountId = abankFopId,
            amountMinor = 4_200_00L,
            currency = "UAH",
            type = TransactionType.EXPENSE.name,
            category = "UTILITIES",
            title = "ЄСВ та ЄП 5%",
            timestamp = now - 5 * dayMs,
            source = TransactionSource.MANUAL.name,
          ),
          TransactionEntity(
            externalId = "demo_9",
            workspaceId = allianceWsId,
            accountId = allianceEurId,
            amountMinor = 500_00L,
            currency = "EUR",
            type = TransactionType.INCOME.name,
            category = "SALARY",
            title = "Щомісячний резерв",
            timestamp = now - 1 * dayMs,
            source = TransactionSource.NOTIFICATION.name,
          ),
          TransactionEntity(
            externalId = "demo_10",
            workspaceId = allianceWsId,
            accountId = allianceEurId,
            amountMinor = 65_00L,
            currency = "EUR",
            type = TransactionType.EXPENSE.name,
            category = "HEALTH",
            title = "Страхування",
            timestamp = now - 6 * dayMs,
            source = TransactionSource.MANUAL.name,
          ),
        )
      transactionDao.insertAllIgnoreDuplicates(demoTx)

      // Goals / Envelopes
      goalDao.insert(
        GoalEntity(
          workspaceId = monoWsId,
          name = "Подушка безпеки",
          emoji = "🛟",
          targetAmountMinor = 150_000_00L,
          currentAmountMinor = 84_500_00L,
          currency = "UAH",
          colorHex = "#34D399",
          deadlineEpochMillis = now + 90 * dayMs,
        )
      )
      goalDao.insert(
        GoalEntity(
          workspaceId = abankWsId,
          name = "MacBook Pro M4",
          emoji = "💻",
          targetAmountMinor = 2_800_00L,
          currentAmountMinor = 1_950_00L,
          currency = "USD",
          colorHex = "#00F5D4",
          deadlineEpochMillis = now + 45 * dayMs,
        )
      )
      goalDao.insert(
        GoalEntity(
          workspaceId = allianceWsId,
          name = "Подорож у Карпати",
          emoji = "🏔️",
          targetAmountMinor = 1_500_00L,
          currentAmountMinor = 920_00L,
          currency = "EUR",
          colorHex = "#2563EB",
          deadlineEpochMillis = now + 60 * dayMs,
        )
      )

      // Subscriptions
      subscriptionDao.insert(
        SubscriptionEntity(
          workspaceId = monoWsId,
          accountId = monoBlackId,
          name = "Spotify Duo",
          amountMinor = 249_00L,
          currency = "UAH",
          billingCycle = BillingCycle.MONTHLY.name,
          nextPaymentEpochMillis = now + 2 * dayMs,
        )
      )
      subscriptionDao.insert(
        SubscriptionEntity(
          workspaceId = monoWsId,
          accountId = monoBlackId,
          name = "Київстар Суперсила",
          amountMinor = 350_00L,
          currency = "UAH",
          billingCycle = BillingCycle.MONTHLY.name,
          nextPaymentEpochMillis = now + 6 * dayMs,
        )
      )
      subscriptionDao.insert(
        SubscriptionEntity(
          workspaceId = abankWsId,
          accountId = abankUsdId,
          name = "Figma Pro",
          amountMinor = 15_00L,
          currency = "USD",
          billingCycle = BillingCycle.MONTHLY.name,
          nextPaymentEpochMillis = now + 4 * dayMs,
        )
      )
      subscriptionDao.insert(
        SubscriptionEntity(
          workspaceId = allianceWsId,
          accountId = allianceEurId,
          name = "iCloud+ 2TB",
          amountMinor = 9_99L,
          currency = "EUR",
          billingCycle = BillingCycle.MONTHLY.name,
          nextPaymentEpochMillis = now + 11 * dayMs,
        )
      )

      // Ukrainian bank notification presets
      parsingRuleDao.insertAll(
        BankNotificationParser.defaultUkrainianPresets(
          aBankAccountId = abankUsdId,
          allianceAccountId = allianceEurId,
        )
      )

      // Cached demo currency rates
      currencyRateDao.upsertAll(
        listOf(
          CurrencyRateEntity("840_980", 840, 980, 41.20, 41.65, 0.0, now),
          CurrencyRateEntity("978_980", 978, 980, 44.50, 45.15, 0.0, now),
          CurrencyRateEntity("985_980", 985, 980, 0.0, 0.0, 10.45, now),
          CurrencyRateEntity("826_980", 826, 980, 0.0, 0.0, 52.60, now),
        )
      )

      prefs.setSelectedWorkspaceId(monoWsId)
    }
  }

  suspend fun clearAllData() {
    db.withTransaction {
      workspaceDao.deleteAll()
      parsingRuleDao.deleteAll()
      prefs.setSelectedWorkspaceId(AppSettings.ALL_WORKSPACES_ID)
    }
  }
}
