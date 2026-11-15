package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.ParsingRuleDirection
import com.example.data.local.ParsingRuleEntity
import com.example.data.parser.BankNotificationParser
import com.example.ui.components.VaultCard
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.viewmodel.VaultUiState
import com.example.util.MoneyFormatter
import com.example.util.NotificationAccessHelper

@Composable
fun BanksScreen(
  uiState: VaultUiState,
  onSyncMonobank: (token: String, fetchStatements: Boolean) -> Unit,
  onReassignAccount: (accountId: Long, workspaceId: Long) -> Unit,
  onLoadUkrainianPresets: () -> Unit,
  onSaveRule: (ParsingRuleEntity) -> Unit,
  onToggleRule: (ParsingRuleEntity, Boolean) -> Unit,
  onAssignBankToWorkspace: (ParsingRuleEntity, Long) -> Unit,
  onDeleteRule: (Long) -> Unit,
  onSimulatePush: (packageName: String, text: String, onDone: (Boolean) -> Unit) -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val theme = LocalWorkspaceTheme.current

  var tokenInput by remember(uiState.settings.monobankToken) {
    mutableStateOf(uiState.settings.monobankToken)
  }
  var listenerEnabled by remember {
    mutableStateOf(NotificationAccessHelper.isListenerEnabled(context))
  }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    listenerEnabled = NotificationAccessHelper.isListenerEnabled(context)
  }

  var showDeveloperTools by remember { mutableStateOf(false) }
  var editingRule by remember { mutableStateOf<ParsingRuleEntity?>(null) }
  var showSimulateDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item {
      Text(
        text = stringResource(R.string.banks_title),
        style = MaterialTheme.typography.headlineMedium,
        color = theme.primaryText,
      )
    }

    // 1. Monobank Branded Card
    item {
      val shape = RoundedCornerShape(theme.cornerRadius)
      Surface(
        modifier = Modifier.fillMaxWidth().clip(shape),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)),
      ) {
        Column(
          modifier =
            Modifier.background(
              Brush.linearGradient(colors = listOf(Color(0xFF1F2330), Color(0xFF101218)))
            )
              .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "monobank",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.monobank.ua/"))
                context.startActivity(intent)
              },
              border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.open_mono_portal), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
          }

          Text(stringResource(R.string.mono_step_1), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f))
          Text(stringResource(R.string.mono_step_2), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f))

          OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text(stringResource(R.string.monobank_token_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("monobank_token_input"),
          )

          if (uiState.monoCooldownSeconds > 0) {
            Text(
              text = stringResource(R.string.sync_cooldown, uiState.monoCooldownSeconds),
              style = MaterialTheme.typography.labelMedium,
              color = theme.accent,
            )
          }

          Button(
            onClick = { onSyncMonobank(tokenInput, true) },
            enabled = tokenInput.isNotBlank() && !uiState.isSyncingMonobank && uiState.monoCooldownSeconds == 0,
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mono_sync_statements_btn"),
          ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sync_now))
          }
        }
      }
    }

    // 2. Instant Push Banks Section (Zero Regex Visible!)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.auto_push_banks),
          style = MaterialTheme.typography.titleMedium,
          color = theme.primaryText,
        )
        Text(
          text = stringResource(R.string.auto_push_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = theme.secondaryText,
        )
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable { NotificationAccessHelper.openListenerSettings(context) },
          shape = RoundedCornerShape(14.dp),
          color = (if (listenerEnabled) theme.income else theme.expense).copy(alpha = 0.14f),
          border = BorderStroke(0.5.dp, (if (listenerEnabled) theme.income else theme.expense).copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = if (listenerEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (listenerEnabled) theme.income else theme.expense,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  if (listenerEnabled) stringResource(R.string.listener_permission_granted)
                  else stringResource(R.string.listener_permission_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = theme.primaryText,
              )
              if (!listenerEnabled) {
                Text(
                  text = stringResource(R.string.grant_listener_access),
                  style = MaterialTheme.typography.labelMedium,
                  color = theme.accent,
                )
              }
            }
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = theme.accent)
          }
        }
      }
    }

    items(uiState.parsingRules, key = { it.id }) { rule ->
      val (bankTitle, bankAccent) =
        when {
          rule.packageName.contains("abank") -> "А-Банк" to Color(0xFF10B981)
          rule.packageName.contains("alliance") -> "Альянс Банк" to Color(0xFF3B82F6)
          rule.packageName.contains("privat") -> "ПриватБанк" to Color(0xFF22C55E)
          rule.packageName.contains("pumb") -> "ПУМБ" to Color(0xFFEF4444)
          else -> rule.name to theme.accent
        }
      val linkedAccount = uiState.allAccounts.firstOrNull { it.id == rule.targetAccountId }

      VaultCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier.size(36.dp).clip(CircleShape).background(bankAccent.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = bankTitle.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = bankAccent,
                fontWeight = FontWeight.Bold,
              )
            }
            Column {
              Text(bankTitle, style = MaterialTheme.typography.titleMedium, color = theme.primaryText)
              Text(
                text = linkedAccount?.name ?: stringResource(R.string.target_workspace_label),
                style = MaterialTheme.typography.labelMedium,
                color = theme.secondaryText,
              )
            }
          }
          Switch(
            checked = rule.isEnabled,
            onCheckedChange = { onToggleRule(rule, it) },
          )
        }

        if (uiState.workspaces.isNotEmpty()) {
          Spacer(Modifier.height(10.dp))
          Text(
            text = stringResource(R.string.target_workspace_label),
            style = MaterialTheme.typography.labelMedium,
            color = theme.secondaryText,
          )
          Spacer(Modifier.height(6.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            uiState.workspaces.forEach { ws ->
              val isSelected = linkedAccount?.workspaceId == ws.id
              FilterChip(
                selected = isSelected,
                onClick = { onAssignBankToWorkspace(rule, ws.id) },
                label = { Text("${ws.emoji} ${ws.name}") },
              )
            }
          }
        }
      }
    }

    // Account Workspace Reassignment (if user has imported Monobank cards)
    if (uiState.allAccounts.isNotEmpty() && uiState.workspaces.isNotEmpty()) {
      item {
        VaultCard(modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.assign_to_workspace), style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(10.dp))
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.allAccounts.forEach { acc ->
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(acc.name, style = MaterialTheme.typography.bodyLarge, color = theme.primaryText)
                  Text(
                    text = MoneyFormatter.formatMinor(acc.balanceMinor, acc.currency, uiState.settings.hideBalances),
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.accent,
                  )
                }
                Row(
                  modifier = Modifier.horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  uiState.workspaces.forEach { ws ->
                    FilterChip(
                      selected = acc.workspaceId == ws.id,
                      onClick = { onReassignAccount(acc.id, ws.id) },
                      label = { Text("${ws.emoji} ${ws.name}") },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Hidden "For Developers" Progressive Disclosure Section
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(
          onClick = { showDeveloperTools = !showDeveloperTools },
          modifier = Modifier.fillMaxWidth().testTag("developer_regex_toggle"),
        ) {
          Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.secondaryText)
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.developer_mode), color = theme.secondaryText)
        }

        AnimatedVisibility(visible = showDeveloperTools) {
          VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    editingRule =
                      ParsingRuleEntity(
                        name = "",
                        packageName = "ua.com.abank",
                        targetAccountId = uiState.allAccounts.firstOrNull()?.id,
                        regexPattern = """([0-9\s]+[.,][0-9]{2})\s*(UAH|USD|EUR|грн)\s*(.*)""",
                        sampleText = "Списання 240,50 грн у Rozetka",
                      )
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                  modifier = Modifier.weight(1f).testTag("new_regex_rule_btn"),
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(stringResource(R.string.add_parsing_rule), maxLines = 1)
                }
                OutlinedButton(
                  onClick = { showSimulateDialog = true },
                  modifier = Modifier.weight(1f).testTag("simulate_push_btn"),
                ) {
                  Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(stringResource(R.string.simulate_push_test), maxLines = 1)
                }
              }
              OutlinedButton(
                onClick = onLoadUkrainianPresets,
                modifier = Modifier.fillMaxWidth().testTag("load_ukrainian_presets_btn"),
              ) {
                Text(stringResource(R.string.load_ukrainian_presets))
              }

              uiState.parsingRules.forEach { rule ->
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { editingRule = rule }
                      .padding(vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(rule.name, style = MaterialTheme.typography.bodyMedium)
                    Text(rule.packageName, style = MaterialTheme.typography.labelMedium, color = theme.accent)
                  }
                  IconButton(onClick = { onDeleteRule(rule.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = theme.expense)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  editingRule?.let { initial ->
    RegexRuleEditorDialog(
      initialRule = initial,
      accounts = uiState.allAccounts,
      onDismiss = { editingRule = null },
      onSave = {
        onSaveRule(it)
        editingRule = null
      },
    )
  }

  if (showSimulateDialog) {
    SimulatePushDialog(
      rules = uiState.parsingRules,
      onDismiss = { showSimulateDialog = false },
      onSimulate = { pkg, text ->
        onSimulatePush(pkg, text) { matched ->
          onShowMessage(if (matched) "Push matched & transaction added!" else "No active rule matched.")
        }
        showSimulateDialog = false
      },
    )
  }
}

@Composable
private fun RegexRuleEditorDialog(
  initialRule: ParsingRuleEntity,
  accounts: List<AccountEntity>,
  onDismiss: () -> Unit,
  onSave: (ParsingRuleEntity) -> Unit,
) {
  var name by remember { mutableStateOf(initialRule.name) }
  var pkg by remember { mutableStateOf(initialRule.packageName) }
  var regex by remember { mutableStateOf(initialRule.regexPattern) }
  var amountGroup by remember { mutableIntStateOf(initialRule.amountGroupIndex) }
  var currencyGroup by remember { mutableIntStateOf(initialRule.currencyGroupIndex) }
  var merchantGroup by remember { mutableIntStateOf(initialRule.merchantGroupIndex) }
  var targetAccountId by remember { mutableStateOf(initialRule.targetAccountId) }
  var sampleText by remember { mutableStateOf(initialRule.sampleText) }
  var direction by remember {
    mutableStateOf(
      runCatching { ParsingRuleDirection.valueOf(initialRule.direction) }
        .getOrDefault(ParsingRuleDirection.AUTO)
    )
  }

  val draftRule =
    initialRule.copy(
      name = name.ifBlank { pkg },
      packageName = pkg,
      regexPattern = regex,
      amountGroupIndex = amountGroup,
      currencyGroupIndex = currencyGroup,
      merchantGroupIndex = merchantGroup,
      targetAccountId = targetAccountId,
      direction = direction.name,
      sampleText = sampleText,
    )
  val livePreview = remember(sampleText, regex, amountGroup, currencyGroup, merchantGroup, direction) {
    BankNotificationParser.parse(sampleText, draftRule)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.add_parsing_rule)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.rule_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = pkg,
          onValueChange = { pkg = it },
          label = { Text(stringResource(R.string.package_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = regex,
          onValueChange = { regex = it },
          label = { Text(stringResource(R.string.regex_pattern)) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = amountGroup.toString(),
            onValueChange = { amountGroup = it.toIntOrNull() ?: 1 },
            label = { Text(stringResource(R.string.amount_group)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = currencyGroup.toString(),
            onValueChange = { currencyGroup = it.toIntOrNull() ?: 0 },
            label = { Text(stringResource(R.string.currency_group)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = merchantGroup.toString(),
          onValueChange = { merchantGroup = it.toIntOrNull() ?: 0 },
          label = { Text(stringResource(R.string.merchant_group)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (accounts.isNotEmpty()) {
          Text(stringResource(R.string.target_account), style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            accounts.forEach { acc ->
              FilterChip(
                selected = targetAccountId == acc.id,
                onClick = { targetAccountId = acc.id },
                label = { Text(acc.name) },
              )
            }
          }
        }
        OutlinedTextField(
          value = sampleText,
          onValueChange = { sampleText = it },
          label = { Text(stringResource(R.string.test_sample_text)) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          text =
            if (livePreview != null) {
              "${livePreview.transactionType.name}: ${
                MoneyFormatter.formatMinor(livePreview.amountMinor, livePreview.currency)
              } • ${livePreview.merchant}"
            } else {
              stringResource(R.string.regex_no_match)
            },
          color =
            if (livePreview != null) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(draftRule) },
        enabled = pkg.isNotBlank() && regex.isNotBlank(),
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}

@Composable
private fun SimulatePushDialog(
  rules: List<ParsingRuleEntity>,
  onDismiss: () -> Unit,
  onSimulate: (packageName: String, text: String) -> Unit,
) {
  val firstRule = rules.firstOrNull()
  var pkg by remember { mutableStateOf(firstRule?.packageName ?: "ua.com.abank") }
  var text by remember {
    mutableStateOf(firstRule?.sampleText?.ifBlank { "Списання 345,50 грн у Silpo Kyiv" } ?: "Списання 345,50 грн у Silpo Kyiv")
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.simulate_push_test)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rules.isNotEmpty()) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            rules.forEach { rule ->
              FilterChip(
                selected = pkg == rule.packageName,
                onClick = {
                  pkg = rule.packageName
                  if (rule.sampleText.isNotBlank()) text = rule.sampleText
                },
                label = { Text(rule.name) },
              )
            }
          }
        }
        OutlinedTextField(
          value = pkg,
          onValueChange = { pkg = it },
          label = { Text(stringResource(R.string.package_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text(stringResource(R.string.test_sample_text)) },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSimulate(pkg, text) }) {
        Text(stringResource(R.string.apply))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
