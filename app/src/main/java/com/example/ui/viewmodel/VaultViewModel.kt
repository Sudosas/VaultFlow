package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VaultFlowApplication
import com.example.data.local.AccountEntity
import com.example.data.local.AppSettings
import com.example.data.local.BankType
import com.example.data.local.BillingCycle
import com.example.data.local.CurrencyRateEntity
import com.example.data.local.GoalEntity
import com.example.data.local.ParsingRuleEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WorkspaceEntity
import com.example.data.repository.SyncResult
import com.example.ui.theme.ThemePresets
import com.example.ui.theme.WorkspaceThemeConfig
import com.example.ui.theme.WorkspaceThemeJson
import com.example.util.CurrencyConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultUiState(
  val settings: AppSettings = AppSettings(),
  val workspaces: List<WorkspaceEntity> = emptyList(),
  val allAccounts: List<AccountEntity> = emptyList(),
  val allTransactions: List<TransactionEntity> = emptyList(),
  val allGoals: List<GoalEntity> = emptyList(),
  val allSubscriptions: List<SubscriptionEntity> = emptyList(),
  val parsingRules: List<ParsingRuleEntity> = emptyList(),
  val currencyRates: List<CurrencyRateEntity> = emptyList(),
  val activeWorkspace: WorkspaceEntity? = null,
  val isAllWorkspacesMode: Boolean = true,
  val activeTheme: WorkspaceThemeConfig = ThemePresets.minimalDark,
  val displayCurrency: String = "UAH",
  val filteredAccounts: List<AccountEntity> = emptyList(),
  val filteredTransactions: List<TransactionEntity> = emptyList(),
  val filteredGoals: List<GoalEntity> = emptyList(),
  val filteredSubscriptions: List<SubscriptionEntity> = emptyList(),
  val totalBalanceMinor: Long = 0L,
  val monthlyIncomeMinor: Long = 0L,
  val monthlyExpenseMinor: Long = 0L,
  val monoCooldownSeconds: Int = 0,
  val isSyncingMonobank: Boolean = false,
)

sealed class UiEvent {
  data class ShowMessage(val text: String) : UiEvent()
  data class SyncFeedback(val result: SyncResult) : UiEvent()
  data object RecreateForLocale : UiEvent()
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as VaultFlowApplication
  private val repo = app.repository
  private val prefs = app.preferences

  private val isSyncingMono = MutableStateFlow(false)
  private val cooldownSeconds = MutableStateFlow(0)

  private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<UiEvent> = _events.asSharedFlow()

  private val coreDataFlow =
    combine(
      repo.settingsFlow,
      repo.workspacesFlow,
      repo.accountsFlow,
      repo.transactionsFlow,
      repo.goalsFlow,
    ) { settings, workspaces, accounts, transactions, goals ->
      CoreBundle(settings, workspaces, accounts, transactions, goals)
    }

  private val auxDataFlow =
    combine(
      repo.subscriptionsFlow,
      repo.parsingRulesFlow,
      repo.currencyRatesFlow,
      isSyncingMono,
      cooldownSeconds,
    ) { subs, rules, rates, syncing, cooldown ->
      AuxBundle(subs, rules, rates, syncing, cooldown)
    }

  val uiState: StateFlow<VaultUiState> =
    combine(coreDataFlow, auxDataFlow) { core, aux ->
        val settings = core.settings
        val workspaces = core.workspaces
        val selectedId = settings.selectedWorkspaceId
        val activeWorkspace =
          if (selectedId == AppSettings.ALL_WORKSPACES_ID) null
          else workspaces.firstOrNull { it.id == selectedId }
        val isAllMode = activeWorkspace == null
        val displayCurrency = activeWorkspace?.primaryCurrency ?: settings.globalCurrency

        val theme =
          when {
            activeWorkspace != null -> WorkspaceThemeJson.decodeOrDefault(activeWorkspace.themeJson)
            workspaces.isNotEmpty() -> WorkspaceThemeJson.decodeOrDefault(workspaces.first().themeJson)
            else -> ThemePresets.minimalDark
          }

        val filteredAccounts =
          if (isAllMode) core.accounts else core.accounts.filter { it.workspaceId == activeWorkspace!!.id }
        val filteredTransactions =
          if (isAllMode) core.transactions
          else core.transactions.filter { it.workspaceId == activeWorkspace!!.id }
        val filteredGoals =
          if (isAllMode) core.goals else core.goals.filter { it.workspaceId == activeWorkspace!!.id }
        val filteredSubs =
          if (isAllMode) aux.subs else aux.subs.filter { it.workspaceId == activeWorkspace!!.id }

        val totalBalance =
          filteredAccounts.sumOf { acc ->
            CurrencyConverter.convertMinor(acc.balanceMinor, acc.currency, displayCurrency, aux.rates)
          }

        val monthAgo = System.currentTimeMillis() - 30L * 86_400_000L
        var incomeSum = 0L
        var expenseSum = 0L
        for (tx in filteredTransactions) {
          if (tx.timestamp >= monthAgo) {
            val converted =
              CurrencyConverter.convertMinor(tx.amountMinor, tx.currency, displayCurrency, aux.rates)
            when (tx.type) {
              TransactionType.INCOME.name -> incomeSum += converted
              TransactionType.EXPENSE.name -> expenseSum += converted
            }
          }
        }

        VaultUiState(
          settings = settings,
          workspaces = workspaces,
          allAccounts = core.accounts,
          allTransactions = core.transactions,
          allGoals = core.goals,
          allSubscriptions = aux.subs,
          parsingRules = aux.rules,
          currencyRates = aux.rates,
          activeWorkspace = activeWorkspace,
          isAllWorkspacesMode = isAllMode,
          activeTheme = theme,
          displayCurrency = displayCurrency,
          filteredAccounts = filteredAccounts,
          filteredTransactions = filteredTransactions,
          filteredGoals = filteredGoals,
          filteredSubscriptions = filteredSubs,
          totalBalanceMinor = totalBalance,
          monthlyIncomeMinor = incomeSum,
          monthlyExpenseMinor = expenseSum,
          monoCooldownSeconds = aux.cooldown,
          isSyncingMonobank = aux.syncing,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState(),
      )

  init {
    viewModelScope.launch {
      repo.ensureDefaultParsingRules()
    }
    viewModelScope.launch {
      while (true) {
        val lastSync = uiState.value.settings.lastMonoSyncEpochMillis
        val elapsed = System.currentTimeMillis() - lastSync
        cooldownSeconds.value =
          if (lastSync > 0L && elapsed in 0 until 60_000L) {
            ((60_000L - elapsed) / 1000L).toInt().coerceAtLeast(1)
          } else {
            0
          }
        delay(1000L)
      }
    }
  }

  fun selectWorkspace(workspaceId: Long) {
    viewModelScope.launch { prefs.setSelectedWorkspaceId(workspaceId) }
  }

  fun createWorkspace(
    name: String,
    emoji: String,
    currency: String,
    theme: WorkspaceThemeConfig = ThemePresets.minimalDark,
    initialBalanceMinor: Long = 0L,
    defaultAccountName: String = "Main Card",
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repo.createWorkspace(
        name = name,
        emoji = emoji,
        primaryCurrency = currency,
        themeConfig = theme,
        initialBalanceMinor = initialBalanceMinor,
        createDefaultAccount = true,
        defaultAccountName = defaultAccountName,
      )
    }
  }

  fun updateWorkspaceDetails(workspace: WorkspaceEntity, name: String, emoji: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repo.updateWorkspace(workspace.copy(name = name.trim(), emoji = emoji.ifBlank { workspace.emoji }))
    }
  }

  fun deleteWorkspace(workspaceId: Long) {
    viewModelScope.launch { repo.deleteWorkspace(workspaceId) }
  }

  fun updateWorkspaceTheme(workspaceId: Long, config: WorkspaceThemeConfig) {
    viewModelScope.launch { repo.updateWorkspaceTheme(workspaceId, config) }
  }

  fun addAccount(
    workspaceId: Long,
    name: String,
    bankType: BankType,
    balanceMinor: Long,
    currency: String,
    maskedPan: String,
    colorHex: String,
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repo.addAccount(
        AccountEntity(
          workspaceId = workspaceId,
          name = name.trim(),
          bankType = bankType.name,
          balanceMinor = balanceMinor,
          currency = currency,
          maskedPan = maskedPan.trim(),
          cardColorHex = colorHex,
        )
      )
    }
  }

  fun reassignAccountWorkspace(accountId: Long, workspaceId: Long) {
    viewModelScope.launch { repo.reassignAccountWorkspace(accountId, workspaceId) }
  }

  fun deleteAccount(accountId: Long) {
    viewModelScope.launch { repo.deleteAccount(accountId) }
  }

  fun addTransaction(
    workspaceId: Long,
    accountId: Long,
    targetAccountId: Long?,
    amountMinor: Long,
    currency: String,
    type: TransactionType,
    category: String,
    title: String,
    note: String,
  ) {
    viewModelScope.launch {
      repo.addManualTransaction(
        workspaceId = workspaceId,
        accountId = accountId,
        targetAccountId = targetAccountId,
        amountMinor = amountMinor,
        currency = currency,
        type = type,
        category = category,
        title = title,
        note = note,
      )
    }
  }

  fun deleteTransaction(tx: TransactionEntity) {
    viewModelScope.launch { repo.deleteTransaction(tx) }
  }

  fun addGoal(
    workspaceId: Long,
    name: String,
    emoji: String,
    targetAmountMinor: Long,
    currency: String,
    colorHex: String,
  ) {
    if (name.isBlank() || targetAmountMinor <= 0L) return
    viewModelScope.launch {
      repo.addGoal(
        GoalEntity(
          workspaceId = workspaceId,
          name = name.trim(),
          emoji = emoji.ifBlank { "🎯" },
          targetAmountMinor = targetAmountMinor,
          currency = currency,
          colorHex = colorHex,
        )
      )
    }
  }

  fun topUpGoal(goalId: Long, amountMinor: Long, deductAccountId: Long?) {
    viewModelScope.launch { repo.topUpGoal(goalId, amountMinor, deductAccountId) }
  }

  fun deleteGoal(goalId: Long) {
    viewModelScope.launch { repo.deleteGoal(goalId) }
  }

  fun addSubscription(
    workspaceId: Long,
    accountId: Long?,
    name: String,
    amountMinor: Long,
    currency: String,
    cycle: BillingCycle,
    nextPaymentEpochMillis: Long,
  ) {
    if (name.isBlank() || amountMinor <= 0L) return
    viewModelScope.launch {
      repo.addSubscription(
        SubscriptionEntity(
          workspaceId = workspaceId,
          accountId = accountId,
          name = name.trim(),
          amountMinor = amountMinor,
          currency = currency,
          billingCycle = cycle.name,
          nextPaymentEpochMillis = nextPaymentEpochMillis,
        )
      )
    }
  }

  fun markSubscriptionPaid(sub: SubscriptionEntity) {
    viewModelScope.launch { repo.markSubscriptionPaid(sub) }
  }

  fun deleteSubscription(subId: Long) {
    viewModelScope.launch { repo.deleteSubscription(subId) }
  }

  fun saveMonobankToken(token: String) {
    viewModelScope.launch { prefs.setMonobankToken(token) }
  }

  fun syncMonobank(tokenOverride: String? = null, fetchStatements: Boolean = true) {
    if (isSyncingMono.value) return
    viewModelScope.launch {
      isSyncingMono.value = true
      val result = repo.syncMonobank(tokenOverride, fetchStatements)
      isSyncingMono.value = false
      _events.emit(UiEvent.SyncFeedback(result))
    }
  }

  fun refreshCurrencyRates(successText: String, failText: String) {
    viewModelScope.launch {
      val ok = repo.refreshCurrencyRates()
      _events.emit(UiEvent.ShowMessage(if (ok) successText else failText))
    }
  }

  fun saveParsingRule(rule: ParsingRuleEntity) {
    viewModelScope.launch { repo.saveParsingRule(rule) }
  }

  fun toggleParsingRule(rule: ParsingRuleEntity, enabled: Boolean) {
    viewModelScope.launch { repo.toggleParsingRule(rule, enabled) }
  }

  fun assignBankPushToWorkspace(rule: ParsingRuleEntity, workspaceId: Long) {
    viewModelScope.launch { repo.assignBankPushToWorkspace(rule, workspaceId) }
  }

  fun deleteParsingRule(ruleId: Long) {
    viewModelScope.launch { repo.deleteParsingRule(ruleId) }
  }

  fun loadUkrainianPresets() {
    viewModelScope.launch { repo.loadUkrainianParserPresets() }
  }

  fun simulatePushNotification(packageName: String, text: String, onResult: (Boolean) -> Unit) {
    viewModelScope.launch {
      val matched = repo.processIncomingNotification(packageName, text, System.currentTimeMillis())
      onResult(matched)
    }
  }

  fun toggleHideBalances() {
    viewModelScope.launch { prefs.toggleHideBalances() }
  }

  fun setBiometricLock(enabled: Boolean) {
    viewModelScope.launch { prefs.setBiometricLock(enabled) }
  }

  fun setShakeToHide(enabled: Boolean) {
    viewModelScope.launch { prefs.setShakeToHide(enabled) }
  }

  fun setGlobalCurrency(currency: String) {
    viewModelScope.launch { prefs.setGlobalCurrency(currency) }
  }

  fun setAppLanguage(languageTag: String) {
    viewModelScope.launch {
      prefs.setAppLanguage(languageTag)
      _events.emit(UiEvent.RecreateForLocale)
    }
  }

  fun populateDemoData(doneMessage: String) {
    viewModelScope.launch {
      repo.populateDemoData()
      _events.emit(UiEvent.ShowMessage(doneMessage))
    }
  }

  fun clearAllData() {
    viewModelScope.launch { repo.clearAllData() }
  }

  fun showMessage(text: String) {
    viewModelScope.launch { _events.emit(UiEvent.ShowMessage(text)) }
  }

  private data class CoreBundle(
    val settings: AppSettings,
    val workspaces: List<WorkspaceEntity>,
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val goals: List<GoalEntity>,
  )

  private data class AuxBundle(
    val subs: List<SubscriptionEntity>,
    val rules: List<ParsingRuleEntity>,
    val rates: List<CurrencyRateEntity>,
    val syncing: Boolean,
    val cooldown: Int,
  )
}
