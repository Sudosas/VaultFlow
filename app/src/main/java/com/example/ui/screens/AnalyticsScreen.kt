package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.BillingCycle
import com.example.data.local.GoalEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.local.TransactionType
import com.example.data.remote.IsoCurrencyMapper
import com.example.ui.components.CashflowChartCanvas
import com.example.ui.components.CategoryBreakdownRing
import com.example.ui.components.SplitMoneyText
import com.example.ui.components.VaultCard
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.VaultUiState
import com.example.util.CurrencyConverter
import com.example.util.MoneyFormatter
import java.util.Locale
import kotlin.math.round

@Composable
fun AnalyticsScreen(
  uiState: VaultUiState,
  onRefreshRates: () -> Unit,
  onAddGoal: (Long, String, String, Long, String, String) -> Unit,
  onTopUpGoal: (Long, Long, Long?) -> Unit,
  onDeleteGoal: (Long) -> Unit,
  onAddSubscription: (Long, Long?, String, Long, String, BillingCycle, Long) -> Unit,
  onMarkSubPaid: (SubscriptionEntity) -> Unit,
  onDeleteSub: (Long) -> Unit,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  val hide = uiState.settings.hideBalances
  var selectedTab by remember { mutableIntStateOf(0) }
  var showAddGoal by remember { mutableStateOf(false) }
  var showAddSub by remember { mutableStateOf(false) }
  var topUpGoal by remember { mutableStateOf<GoalEntity?>(null) }

  val tabs =
    listOf(
      stringResource(R.string.tab_overview),
      stringResource(R.string.tab_goals),
      stringResource(R.string.tab_subscriptions),
    )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item {
      Text(
        text = stringResource(R.string.analytics_title),
        style = MaterialTheme.typography.headlineMedium,
        color = theme.primaryText,
      )
    }

    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, label ->
          SegmentedButton(
            selected = selectedTab == index,
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              selectedTab = index
            },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
          ) {
            Text(label, maxLines = 1)
          }
        }
      }
    }

    when (selectedTab) {
      0 -> {
        item {
          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.block_chart), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            CashflowChartCanvas(
              transactions = uiState.filteredTransactions,
              displayCurrency = uiState.displayCurrency,
              rates = uiState.currencyRates,
            )
          }
        }

        item {
          val categoryTotals = remember(uiState.filteredTransactions, uiState.displayCurrency, uiState.currencyRates) {
            uiState.filteredTransactions
              .filter { it.type == TransactionType.EXPENSE.name }
              .groupBy { it.category }
              .map { (cat, list) ->
                cat to
                  list.sumOf {
                    CurrencyConverter.convertMinor(
                      it.amountMinor,
                      it.currency,
                      uiState.displayCurrency,
                      uiState.currencyRates,
                    )
                  }
              }
              .sortedByDescending { it.second }
          }
          val grandTotal = categoryTotals.sumOf { it.second }.coerceAtLeast(1L)

          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.spending_by_category), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              CategoryBreakdownRing(slices = categoryTotals)
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (categoryTotals.isEmpty()) {
                  Text(stringResource(R.string.no_transactions_yet), color = theme.secondaryText)
                } else {
                  categoryTotals.take(6).forEach { (cat, amount) ->
                    val pct = (amount * 100.0 / grandTotal).toInt()
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text("$cat ($pct%)", style = MaterialTheme.typography.bodyMedium, color = theme.primaryText)
                      Text(
                        MoneyFormatter.formatMinor(amount, uiState.displayCurrency, hide),
                        style = MaterialTheme.typography.labelLarge,
                        color = theme.expense,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        item {
          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(stringResource(R.string.currency_rates_title), style = MaterialTheme.typography.titleMedium)
              IconButton(onClick = onRefreshRates) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_rates), tint = theme.accent)
              }
            }
            Spacer(Modifier.height(8.dp))
            val keyRates =
              uiState.currencyRates.filter { it.currencyCodeB == 980 && it.currencyCodeA in listOf(840, 978, 985, 826) }
            if (keyRates.isEmpty()) {
              Text("USD ≈ 41.35 ₴ • EUR ≈ 44.80 ₴", color = theme.secondaryText)
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keyRates.forEach { rate ->
                  val iso = IsoCurrencyMapper.toIso(rate.currencyCodeA)
                  val rateText =
                    if (rate.rateBuy > 0 && rate.rateSell > 0) {
                      String.format(Locale.US, "%.2f / %.2f ₴", rate.rateBuy, rate.rateSell)
                    } else {
                      String.format(Locale.US, "%.2f ₴", rate.rateCross)
                    }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text("$iso / UAH", style = MaterialTheme.typography.bodyLarge, color = theme.primaryText)
                    Text(rateText, style = MaterialTheme.typography.labelLarge, color = theme.accent)
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        item {
          Button(
            onClick = { showAddGoal = true },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_goal))
          }
        }
        if (uiState.filteredGoals.isEmpty()) {
          item {
            VaultCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(44.dp)
                      .clip(CircleShape)
                      .background(theme.accent.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(Icons.Default.Flag, contentDescription = null, tint = theme.accent)
                }
                Text(
                  text = stringResource(R.string.no_goals_yet),
                  style = MaterialTheme.typography.bodyMedium,
                  color = theme.secondaryText,
                )
              }
            }
          }
        }
        items(uiState.filteredGoals, key = { it.id }) { goal ->
          val progress =
            if (goal.targetAmountMinor > 0L) {
              (goal.currentAmountMinor.toFloat() / goal.targetAmountMinor.toFloat()).coerceIn(0f, 1f)
            } else 0f
          val goalColor = parseHexColor(goal.colorHex, theme.accent)
          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("${goal.emoji} ${goal.name}", style = MaterialTheme.typography.titleMedium)
              IconButton(onClick = { onDeleteGoal(goal.id) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = theme.expense)
              }
            }
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
              color = goalColor,
              trackColor = theme.borderColor,
            )
            Spacer(Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "${MoneyFormatter.formatMinor(goal.currentAmountMinor, goal.currency, hide)} / ${
                  MoneyFormatter.formatMinor(goal.targetAmountMinor, goal.currency, hide)
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.secondaryText,
              )
              OutlinedButton(onClick = { topUpGoal = goal }) {
                Text(stringResource(R.string.top_up), color = goalColor)
              }
            }
          }
        }
      }

      2 -> {
        item {
          val monthlyBurnMinor =
            uiState.filteredSubscriptions.sumOf { sub ->
              val converted =
                CurrencyConverter.convertMinor(
                  sub.amountMinor,
                  sub.currency,
                  uiState.displayCurrency,
                  uiState.currencyRates,
                )
              when (sub.billingCycle) {
                BillingCycle.WEEKLY.name -> converted * 4
                BillingCycle.YEARLY.name -> converted / 12
                else -> converted
              }
            }
          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.monthly_burn_rate), style = MaterialTheme.typography.bodyMedium, color = theme.secondaryText)
            Spacer(Modifier.height(4.dp))
            SplitMoneyText(
              amountMinor = monthlyBurnMinor,
              currency = uiState.displayCurrency,
              hide = hide,
              majorSize = 32.sp,
              minorSize = 18.sp,
              color = theme.primaryText,
            )
          }
        }
        item {
          Button(
            onClick = { showAddSub = true },
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_subscription))
          }
        }
        if (uiState.filteredSubscriptions.isEmpty()) {
          item {
            VaultCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(44.dp)
                      .clip(CircleShape)
                      .background(theme.accent.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(Icons.Default.Repeat, contentDescription = null, tint = theme.accent)
                }
                Text(
                  text = stringResource(R.string.no_subscriptions_yet),
                  style = MaterialTheme.typography.bodyMedium,
                  color = theme.secondaryText,
                )
              }
            }
          }
        }
        items(uiState.filteredSubscriptions, key = { it.id }) { sub ->
          val days = ((sub.nextPaymentEpochMillis - System.currentTimeMillis()) / 86_400_000L).toInt()
          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(sub.name, style = MaterialTheme.typography.titleMedium)
                Text(
                  "${MoneyFormatter.formatMinor(sub.amountMinor, sub.currency, hide)} • ${sub.billingCycle}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = theme.secondaryText,
                )
                Text(
                  text =
                    when {
                      days < 0 -> stringResource(R.string.overdue_days, -days)
                      days == 0 -> stringResource(R.string.due_today)
                      else -> stringResource(R.string.due_in_days, days)
                    },
                  style = MaterialTheme.typography.labelMedium,
                  color = if (days <= 2) theme.expense else theme.accent,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onMarkSubPaid(sub) }) {
                  Text(stringResource(R.string.mark_paid))
                }
                IconButton(onClick = { onDeleteSub(sub.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = theme.expense)
                }
              }
            }
          }
        }
      }
    }
  }

  topUpGoal?.let { goal ->
    TopUpGoalDialog(
      goal = goal,
      accounts = uiState.allAccounts,
      onDismiss = { topUpGoal = null },
      onConfirm = { amountMinor, accId ->
        onTopUpGoal(goal.id, amountMinor, accId)
        topUpGoal = null
      },
    )
  }

  if (showAddGoal) {
    val ws = uiState.activeWorkspace ?: uiState.workspaces.firstOrNull()
    if (ws != null) {
      AddGoalDialog(
        defaultCurrency = ws.primaryCurrency,
        onDismiss = { showAddGoal = false },
        onCreate = { name, emoji, targetMinor, currency ->
          onAddGoal(ws.id, name, emoji, targetMinor, currency, "#10B981")
          showAddGoal = false
        },
      )
    }
  }

  if (showAddSub) {
    val ws = uiState.activeWorkspace ?: uiState.workspaces.firstOrNull()
    if (ws != null) {
      AddSubscriptionDialog(
        defaultCurrency = ws.primaryCurrency,
        accounts = uiState.allAccounts.filter { it.workspaceId == ws.id },
        onDismiss = { showAddSub = false },
        onCreate = { name, amountMinor, currency, cycle, accountId, nextPayMillis ->
          onAddSubscription(ws.id, accountId, name, amountMinor, currency, cycle, nextPayMillis)
          showAddSub = false
        },
      )
    }
  }
}

@Composable
private fun AddGoalDialog(
  defaultCurrency: String,
  onDismiss: () -> Unit,
  onCreate: (name: String, emoji: String, targetMinor: Long, currency: String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var emoji by remember { mutableStateOf("🎯") }
  var targetText by remember { mutableStateOf("10000") }
  var currency by remember { mutableStateOf(defaultCurrency) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.add_goal)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.goal_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = targetText,
          onValueChange = { targetText = it },
          label = { Text(stringResource(R.string.goal_target)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("🎯", "🛟", "💻", "🏔️", "🚗", "🏠").forEach { e ->
            FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) })
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("UAH", "USD", "EUR", "PLN").forEach { cur ->
            FilterChip(selected = currency == cur, onClick = { currency = cur }, label = { Text(cur) })
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val minor = round((targetText.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100.0).toLong()
          if (name.isNotBlank() && minor > 0L) onCreate(name, emoji, minor, currency)
        }
      ) { Text(stringResource(R.string.create)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}

@Composable
private fun AddSubscriptionDialog(
  defaultCurrency: String,
  accounts: List<AccountEntity>,
  onDismiss: () -> Unit,
  onCreate: (name: String, amountMinor: Long, currency: String, cycle: BillingCycle, accountId: Long?, nextPayMillis: Long) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("249") }
  var currency by remember { mutableStateOf(defaultCurrency) }
  var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
  var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.add_subscription)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.subscription_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text(stringResource(R.string.amount_label)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BillingCycle.entries.forEach { c ->
            FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.name) })
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("UAH", "USD", "EUR").forEach { cur ->
            FilterChip(selected = currency == cur, onClick = { currency = cur }, label = { Text(cur) })
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val minor = round((amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100.0).toLong()
          if (name.isNotBlank() && minor > 0L) {
            onCreate(
              name,
              minor,
              currency,
              cycle,
              selectedAccId,
              System.currentTimeMillis() + 7L * 86_400_000L,
            )
          }
        }
      ) { Text(stringResource(R.string.create)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
