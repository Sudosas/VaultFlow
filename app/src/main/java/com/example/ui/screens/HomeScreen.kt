package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.BankType
import com.example.data.local.GoalEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionSource
import com.example.data.local.TransactionType
import com.example.data.local.WorkspaceEntity
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddWorkspaceDialog
import com.example.ui.components.CashflowChartCanvas
import com.example.ui.components.EditWorkspaceDialog
import com.example.ui.components.SplitMoneyText
import com.example.ui.components.VaultCard
import com.example.ui.components.WorkspaceSwitcherBar
import com.example.ui.components.categoryVisualFor
import com.example.ui.theme.HomeBlockType
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.theme.WorkspaceThemeConfig
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.VaultUiState
import com.example.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round

@Composable
fun HomeScreen(
  uiState: VaultUiState,
  onSelectWorkspace: (Long) -> Unit,
  onCreateWorkspace: (String, String, String, WorkspaceThemeConfig, Long, String) -> Unit,
  onUpdateWorkspace: (WorkspaceEntity, String, String) -> Unit,
  onDeleteWorkspace: (Long) -> Unit,
  onAddAccount: (Long, String, BankType, Long, String, String, String) -> Unit,
  onTogglePrivacy: () -> Unit,
  onOpenQuickTransaction: (TransactionType) -> Unit,
  onOpenGoalsTab: () -> Unit,
  onTopUpGoal: (Long, Long, Long?) -> Unit,
  onMarkSubscriptionPaid: (SubscriptionEntity) -> Unit,
  onDeleteTransaction: (TransactionEntity) -> Unit,
  onOpenThemeStudio: () -> Unit,
  onLoadDemo: () -> Unit,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  val hide = uiState.settings.hideBalances

  var showAddWorkspace by remember { mutableStateOf(false) }
  var showEditWorkspace by remember { mutableStateOf(false) }
  var showAddAccount by remember { mutableStateOf(false) }
  var topUpGoalTarget by remember { mutableStateOf<GoalEntity?>(null) }
  var selectedTxForDelete by remember { mutableStateOf<TransactionEntity?>(null) }

  val defaultAccLabel = stringResource(R.string.main_account_default)
  val todayLabel = stringResource(R.string.today)
  val yesterdayLabel = stringResource(R.string.yesterday)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 112.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // Top Header + Workspace Pill Bar
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                  if (uiState.activeWorkspace != null) {
                    Modifier.clickable { showEditWorkspace = true }
                  } else Modifier
                )
                .padding(vertical = 4.dp),
          ) {
            Text(
              text =
                uiState.activeWorkspace?.let { "${it.emoji} ${it.name}" }
                  ?: stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
              color = theme.primaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (uiState.activeWorkspace != null) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_workspace),
                tint = theme.secondaryText,
                modifier = Modifier.size(15.dp),
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            IconButton(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTogglePrivacy()
              },
              modifier = Modifier.testTag("toggle_privacy_button"),
            ) {
              Icon(
                imageVector = if (hide) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = stringResource(R.string.tap_to_hide),
                tint = theme.secondaryText,
              )
            }
            IconButton(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenThemeStudio()
              },
              modifier = Modifier.testTag("open_studio_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = stringResource(R.string.theme_studio_title),
                tint = theme.accent,
              )
            }
          }
        }

        WorkspaceSwitcherBar(
          workspaces = uiState.workspaces,
          selectedWorkspaceId = uiState.settings.selectedWorkspaceId,
          onSelectWorkspace = onSelectWorkspace,
          onAddWorkspace = { showAddWorkspace = true },
        )
      }
    }

    if (uiState.workspaces.isEmpty()) {
      item {
        VaultCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
          Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = theme.accent,
            modifier = Modifier.size(44.dp),
          )
          Spacer(Modifier.height(14.dp))
          Text(
            text = stringResource(R.string.empty_dashboard_title),
            style = MaterialTheme.typography.titleLarge,
            color = theme.primaryText,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.empty_dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = theme.secondaryText,
          )
          Spacer(Modifier.height(18.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = onLoadDemo,
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).testTag("empty_load_demo_button"),
            ) {
              Text(stringResource(R.string.load_demo_now))
            }
            OutlinedButton(
              onClick = { showAddWorkspace = true },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f),
            ) {
              Text(stringResource(R.string.new_workspace), color = theme.primaryText)
            }
          }
        }
      }
    } else {
      val visibleBlocks = uiState.activeTheme.homeBlocks.filter { it.visible }
      visibleBlocks.forEach { block ->
        when (block.type) {
          HomeBlockType.BALANCE -> {
            item(key = "block_balance") {
              HeroBalanceZone(
                uiState = uiState,
                hide = hide,
                onTogglePrivacy = {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  onTogglePrivacy()
                },
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }

          HomeBlockType.ACCOUNTS -> {
            item(key = "block_accounts") {
              BankCardsCarousel(
                accounts = uiState.filteredAccounts,
                hide = hide,
                onAddAccount = { showAddAccount = true },
              )
            }
          }

          HomeBlockType.QUICK_ACTIONS -> {
            item(key = "block_quick_actions") {
              QuickActionsCirclesRow(
                onExpense = { onOpenQuickTransaction(TransactionType.EXPENSE) },
                onIncome = { onOpenQuickTransaction(TransactionType.INCOME) },
                onTransfer = { onOpenQuickTransaction(TransactionType.TRANSFER) },
                onGoals = onOpenGoalsTab,
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }

          HomeBlockType.TRANSACTIONS -> {
            item(key = "block_transactions") {
              DayGroupedTransactionsSection(
                transactions = uiState.filteredTransactions.take(25),
                hide = hide,
                todayLabel = todayLabel,
                yesterdayLabel = yesterdayLabel,
                onTransactionClick = { selectedTxForDelete = it },
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }

          HomeBlockType.CHART -> {
            item(key = "block_chart") {
              VaultCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(
                  text = stringResource(R.string.block_chart),
                  style = MaterialTheme.typography.titleMedium,
                  color = theme.primaryText,
                )
                Spacer(Modifier.height(12.dp))
                CashflowChartCanvas(
                  transactions = uiState.filteredTransactions,
                  displayCurrency = uiState.displayCurrency,
                  rates = uiState.currencyRates,
                )
              }
            }
          }

          HomeBlockType.GOALS -> {
            item(key = "block_goals") {
              GoalsPreviewCard(
                goals = uiState.filteredGoals,
                hide = hide,
                onTopUpClick = { topUpGoalTarget = it },
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }

          HomeBlockType.SUBSCRIPTIONS -> {
            item(key = "block_subscriptions") {
              SubscriptionsPreviewCard(
                subscriptions = uiState.filteredSubscriptions,
                hide = hide,
                onMarkPaid = onMarkSubscriptionPaid,
                modifier = Modifier.padding(horizontal = 20.dp),
              )
            }
          }
        }
      }
    }
  }

  if (showAddWorkspace) {
    AddWorkspaceDialog(
      onDismiss = { showAddWorkspace = false },
      onCreate = { name, emoji, currency, preset, initialBalanceMinor ->
        onCreateWorkspace(name, emoji, currency, preset, initialBalanceMinor, defaultAccLabel)
        showAddWorkspace = false
      },
    )
  }

  if (showEditWorkspace) {
    uiState.activeWorkspace?.let { ws ->
      EditWorkspaceDialog(
        workspace = ws,
        onDismiss = { showEditWorkspace = false },
        onSave = { name, emoji ->
          onUpdateWorkspace(ws, name, emoji)
          showEditWorkspace = false
        },
        onCustomizeTheme = {
          showEditWorkspace = false
          onOpenThemeStudio()
        },
        onDelete = {
          onDeleteWorkspace(ws.id)
          showEditWorkspace = false
        },
      )
    }
  }

  if (showAddAccount) {
    val targetWs = uiState.activeWorkspace ?: uiState.workspaces.firstOrNull()
    if (targetWs != null) {
      AddAccountDialog(
        defaultCurrency = targetWs.primaryCurrency,
        onDismiss = { showAddAccount = false },
        onCreate = { name, bankType, balanceMinor, currency, pan ->
          onAddAccount(targetWs.id, name, bankType, balanceMinor, currency, pan, "#10B981")
          showAddAccount = false
        },
      )
    }
  }

  topUpGoalTarget?.let { goal ->
    TopUpGoalDialog(
      goal = goal,
      accounts = uiState.allAccounts,
      onDismiss = { topUpGoalTarget = null },
      onConfirm = { amountMinor, accountId ->
        onTopUpGoal(goal.id, amountMinor, accountId)
        topUpGoalTarget = null
      },
    )
  }

  selectedTxForDelete?.let { tx ->
    AlertDialog(
      onDismissRequest = { selectedTxForDelete = null },
      title = { Text(tx.title) },
      text = {
        Text(
          "${MoneyFormatter.formatMinor(tx.amountMinor, tx.currency, hide = false)} • ${
            MoneyFormatter.formatRelativeDate(tx.timestamp)
          }${if (tx.note.isNotBlank()) "\n\n${tx.note}" else ""}"
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteTransaction(tx)
            selectedTxForDelete = null
          }
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = theme.expense)
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.delete_transaction), color = theme.expense)
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedTxForDelete = null }) {
          Text(stringResource(R.string.close))
        }
      },
    )
  }
}

@Composable
private fun HeroBalanceZone(
  uiState: VaultUiState,
  hide: Boolean,
  onTogglePrivacy: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(theme.cornerRadius))
        .clickable(onClick = onTogglePrivacy)
        .padding(vertical = 8.dp)
        .testTag("balance_hero_card"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text =
        if (uiState.isAllWorkspacesMode) stringResource(R.string.net_worth)
        else stringResource(R.string.workspace_balance),
      style = MaterialTheme.typography.bodyMedium,
      color = theme.secondaryText,
    )
    Spacer(Modifier.height(6.dp))
    AnimatedContent(
      targetState = Triple(uiState.totalBalanceMinor, uiState.displayCurrency, hide),
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "heroBalance",
    ) { (balance, cur, isHidden) ->
      SplitMoneyText(
        amountMinor = balance,
        currency = cur,
        hide = isHidden,
        majorSize = 40.sp,
        minorSize = 22.sp,
        color = theme.primaryText,
      )
    }
    Spacer(Modifier.height(12.dp))

    // Compact Monthly Income / Expense Pill Badge
    Surface(
      shape = RoundedCornerShape(50),
      color = theme.cardSurfaceAlpha,
      border = BorderStroke(0.5.dp, theme.borderColor),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "↑ ${MoneyFormatter.formatMinor(uiState.monthlyIncomeMinor, uiState.displayCurrency, hide, showPlus = true)}",
          style = MaterialTheme.typography.labelLarge,
          color = theme.income,
        )
        Text("•", color = theme.secondaryText)
        Text(
          text = "↓ ${MoneyFormatter.formatMinor(-uiState.monthlyExpenseMinor, uiState.displayCurrency, hide)}",
          style = MaterialTheme.typography.labelLarge,
          color = theme.expense,
        )
        Text(
          text = stringResource(R.string.this_month),
          style = MaterialTheme.typography.labelMedium,
          color = theme.secondaryText,
        )
      }
    }
  }
}

@Composable
private fun BankCardsCarousel(
  accounts: List<AccountEntity>,
  hide: Boolean,
  onAddAccount: () -> Unit,
) {
  val theme = LocalWorkspaceTheme.current

  if (accounts.isEmpty()) {
    VaultCard(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      onClick = onAddAccount,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(stringResource(R.string.no_accounts_yet), color = theme.secondaryText)
        Text(stringResource(R.string.add_account), color = theme.accent, fontWeight = FontWeight.SemiBold)
      }
    }
    return
  }

  LazyRow(
    contentPadding = PaddingValues(horizontal = 20.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(accounts, key = { it.id }) { account ->
      FintechBankCard(account = account, hide = hide)
    }
    item(key = "add_card_tile") {
      val shape = RoundedCornerShape(theme.cornerRadius)
      Surface(
        modifier =
          Modifier.width(72.dp)
            .height(138.dp)
            .clip(shape)
            .clickable(onClick = onAddAccount)
            .testTag("add_account_button"),
        shape = shape,
        color = theme.cardSurfaceAlpha,
        border = BorderStroke(0.5.dp, theme.borderColor),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.add_account),
            tint = theme.accent,
            modifier = Modifier.size(26.dp),
          )
        }
      }
    }
  }
}

@Composable
fun FintechBankCard(
  account: AccountEntity,
  hide: Boolean,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val shape = RoundedCornerShape(theme.cornerRadius)

  val (gradientStart, gradientEnd, badgeText) =
    when (account.bankType) {
      BankType.MONOBANK.name ->
        if (account.name.contains("White", true) || account.name.contains("Біла", true)) {
          Triple(Color(0xFF2D3342), Color(0xFF1A1E29), "monobank")
        } else {
          Triple(Color(0xFF1D202B), Color(0xFF0E1016), "monobank")
        }
      BankType.ABANK.name -> Triple(Color(0xFF065F46), Color(0xFF022C22), "A-Bank")
      BankType.ALLIANCE.name -> Triple(Color(0xFF1E40AF), Color(0xFF0F172A), "Alliance")
      BankType.PRIVAT.name -> Triple(Color(0xFF166534), Color(0xFF0A2E1A), "Privat24")
      BankType.PUMB.name -> Triple(Color(0xFF991B1B), Color(0xFF3F0D12), "ПУМБ")
      else -> {
        val base = parseHexColor(account.cardColorHex, theme.accent)
        Triple(base.copy(alpha = 0.35f), theme.cardSurface, "VaultFlow")
      }
    }

  val cardText = Color(0xFFF9FAFB)
  val lastFour = account.maskedPan.takeLast(4).ifBlank { account.currency }

  Surface(
    modifier = modifier.width(236.dp).height(138.dp).clip(shape),
    shape = shape,
    color = Color.Transparent,
    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Brush.linearGradient(colors = listOf(gradientStart, gradientEnd)))
          .padding(16.dp)
    ) {
      // Subtle EMV Chip + Contactless motif
      Canvas(modifier = Modifier.align(Alignment.TopEnd).size(36.dp, 24.dp)) {
        drawRoundRect(
          color = Color.White.copy(alpha = 0.25f),
          topLeft = Offset(0f, 2f),
          size = Size(28.dp.toPx(), 20.dp.toPx()),
          cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
          style = Stroke(width = 1.dp.toPx()),
        )
      }

      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelLarge,
            color = cardText.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = account.name,
            style = MaterialTheme.typography.labelMedium,
            color = cardText.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Column {
          SplitMoneyText(
            amountMinor = account.balanceMinor,
            currency = account.currency,
            hide = hide,
            majorSize = 22.sp,
            minorSize = 14.sp,
            color = cardText,
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = "•••• $lastFour",
            style = MaterialTheme.typography.labelMedium,
            color = cardText.copy(alpha = 0.6f),
          )
        }
      }
    }
  }
}

@Composable
private fun QuickActionsCirclesRow(
  onExpense: () -> Unit,
  onIncome: () -> Unit,
  onTransfer: () -> Unit,
  onGoals: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    CircleQuickAction(
      icon = Icons.Default.ArrowUpward,
      label = stringResource(R.string.quick_expense),
      tint = theme.expense,
      onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onExpense()
      },
      modifier = Modifier.testTag("quick_expense_btn"),
    )
    CircleQuickAction(
      icon = Icons.Default.ArrowDownward,
      label = stringResource(R.string.quick_income),
      tint = theme.income,
      onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onIncome()
      },
      modifier = Modifier.testTag("quick_income_btn"),
    )
    CircleQuickAction(
      icon = Icons.AutoMirrored.Filled.CompareArrows,
      label = stringResource(R.string.quick_transfer),
      tint = theme.accent,
      onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onTransfer()
      },
      modifier = Modifier.testTag("quick_transfer_btn"),
    )
    CircleQuickAction(
      icon = Icons.Default.Flag,
      label = stringResource(R.string.quick_goals),
      tint = theme.primaryText,
      onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onGoals()
      },
      modifier = Modifier.testTag("quick_goals_btn"),
    )
  }
}

@Composable
private fun CircleQuickAction(
  icon: ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  Column(
    modifier = modifier.width(74.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      modifier = Modifier.size(56.dp).clip(CircleShape).clickable(onClick = onClick),
      shape = CircleShape,
      color = tint.copy(alpha = 0.14f),
      border = BorderStroke(0.5.dp, tint.copy(alpha = 0.32f)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
      }
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = theme.primaryText,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun DayGroupedTransactionsSection(
  transactions: List<TransactionEntity>,
  hide: Boolean,
  todayLabel: String,
  yesterdayLabel: String,
  onTransactionClick: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  if (transactions.isEmpty()) {
    VaultCard(modifier = modifier.fillMaxWidth()) {
      Text(stringResource(R.string.no_transactions_yet), color = theme.secondaryText)
    }
    return
  }

  val grouped = remember(transactions, todayLabel, yesterdayLabel) {
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    transactions.groupBy { tx ->
      val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
      when {
        cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
          cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> todayLabel
        cal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
          cal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> yesterdayLabel
        else -> dateFmt.format(Date(tx.timestamp))
      }
    }
  }

  val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    grouped.forEach { (dayTitle, dayList) ->
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = dayTitle,
          style = MaterialTheme.typography.labelLarge,
          color = theme.secondaryText,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        VaultCard(modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dayList.forEachIndexed { idx, tx ->
              val isIncome = tx.type == TransactionType.INCOME.name
              val signedAmount = if (isIncome) tx.amountMinor else -tx.amountMinor
              val visual = categoryVisualFor(tx.category)
              val sourceBadge =
                when (tx.source) {
                  TransactionSource.MONOBANK.name -> " • Mono"
                  TransactionSource.NOTIFICATION.name -> " • Push"
                  else -> ""
                }

              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTransactionClick(tx) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(42.dp)
                      .clip(CircleShape)
                      .background(visual.color.copy(alpha = 0.16f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.color,
                    modifier = Modifier.size(20.dp),
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = tx.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${timeFmt.format(Date(tx.timestamp))}$sourceBadge",
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.secondaryText,
                  )
                }

                SplitMoneyText(
                  amountMinor = signedAmount,
                  currency = tx.currency,
                  hide = hide,
                  showPlus = isIncome,
                  majorSize = 16.sp,
                  minorSize = 12.sp,
                  color = if (isIncome) theme.income else theme.primaryText,
                )
              }

              if (idx < dayList.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = theme.borderColor)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GoalsPreviewCard(
  goals: List<GoalEntity>,
  hide: Boolean,
  onTopUpClick: (GoalEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  VaultCard(modifier = modifier.fillMaxWidth()) {
    Text(stringResource(R.string.block_goals), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    if (goals.isEmpty()) {
      Text(stringResource(R.string.no_goals_yet), color = theme.secondaryText)
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        goals.forEach { goal ->
          val progress =
            if (goal.targetAmountMinor > 0L) {
              (goal.currentAmountMinor.toFloat() / goal.targetAmountMinor.toFloat()).coerceIn(0f, 1f)
            } else 0f
          val goalColor = parseHexColor(goal.colorHex, theme.accent)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("${goal.emoji} ${goal.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
              TextButton(onClick = { onTopUpClick(goal) }) {
                Text(stringResource(R.string.top_up), color = goalColor)
              }
            }
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
              color = goalColor,
              trackColor = theme.borderColor,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(MoneyFormatter.formatMinor(goal.currentAmountMinor, goal.currency, hide), style = MaterialTheme.typography.labelMedium)
              Text(MoneyFormatter.formatMinor(goal.targetAmountMinor, goal.currency, hide), style = MaterialTheme.typography.labelMedium, color = theme.secondaryText)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SubscriptionsPreviewCard(
  subscriptions: List<SubscriptionEntity>,
  hide: Boolean,
  onMarkPaid: (SubscriptionEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val now = System.currentTimeMillis()
  VaultCard(modifier = modifier.fillMaxWidth()) {
    Text(stringResource(R.string.block_subscriptions), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    if (subscriptions.isEmpty()) {
      Text(stringResource(R.string.no_subscriptions_yet), color = theme.secondaryText)
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        subscriptions.take(4).forEach { sub ->
          val days = ((sub.nextPaymentEpochMillis - now) / 86_400_000L).toInt()
          val dueLabel =
            when {
              days < 0 -> stringResource(R.string.overdue_days, -days)
              days == 0 -> stringResource(R.string.due_today)
              else -> stringResource(R.string.due_in_days, days)
            }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(sub.name, style = MaterialTheme.typography.bodyLarge)
              Text(
                "$dueLabel • ${MoneyFormatter.formatMinor(sub.amountMinor, sub.currency, hide)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (days <= 2) theme.expense else theme.secondaryText,
              )
            }
            OutlinedButton(onClick = { onMarkPaid(sub) }) {
              Text(stringResource(R.string.mark_paid), color = theme.accent)
            }
          }
        }
      }
    }
  }
}

@Composable
fun TopUpGoalDialog(
  goal: GoalEntity,
  accounts: List<AccountEntity>,
  onDismiss: () -> Unit,
  onConfirm: (amountMinor: Long, deductAccountId: Long?) -> Unit,
) {
  var amountText by remember { mutableStateOf("500") }
  var selectedAccount by remember {
    mutableStateOf<AccountEntity?>(accounts.firstOrNull { it.workspaceId == goal.workspaceId })
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${goal.emoji} ${goal.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("${stringResource(R.string.top_up_amount)} (${goal.currency})") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (accounts.isNotEmpty()) {
          Text(stringResource(R.string.deduct_from_account), style = MaterialTheme.typography.labelMedium)
          accounts.take(4).forEach { acc ->
            val checked = selectedAccount?.id == acc.id
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedAccount = if (checked) null else acc }
                  .padding(vertical = 6.dp, horizontal = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(acc.name, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
              Text(MoneyFormatter.formatMinor(acc.balanceMinor, acc.currency))
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
          val minor = round(parsed * 100.0).toLong()
          if (minor > 0L) onConfirm(minor, selectedAccount?.id)
        }
      ) {
        Text(stringResource(R.string.top_up))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
