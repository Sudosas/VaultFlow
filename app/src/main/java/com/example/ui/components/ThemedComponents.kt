package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.AppSettings
import com.example.data.local.BankType
import com.example.data.local.CurrencyRateEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WorkspaceEntity
import com.example.ui.theme.CardVisualStyle
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.theme.ThemePresets
import com.example.ui.theme.WorkspaceThemeConfig
import com.example.ui.theme.parseHexColor
import com.example.util.CurrencyConverter
import com.example.util.MoneyFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

@Composable
fun VaultBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val theme = LocalWorkspaceTheme.current
  val primaryGlow = theme.accent.copy(alpha = if (theme.isLight) 0.08f else 0.14f)
  val secondaryGlow = theme.income.copy(alpha = if (theme.isLight) 0.04f else 0.08f)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(theme.background)
        .background(
          Brush.radialGradient(
            colors = listOf(primaryGlow, Color.Transparent),
            center = Offset(120f, 80f),
            radius = 950f,
          )
        )
        .background(
          Brush.radialGradient(
            colors = listOf(secondaryGlow, Color.Transparent),
            center = Offset(980f, 1700f),
            radius = 900f,
          )
        ),
    content = content,
  )
}

@Composable
fun VaultCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  accentTint: Color? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val theme = LocalWorkspaceTheme.current
  val shape = RoundedCornerShape(theme.cornerRadius)
  val baseColor = accentTint ?: theme.cardSurface

  val backgroundBrush =
    when (theme.cardStyle) {
      CardVisualStyle.GRADIENT ->
        Brush.linearGradient(
          colors =
            listOf(
              baseColor.copy(alpha = theme.config.cardAlpha.coerceIn(0.85f, 1f)),
              baseColor
                .copy(
                  red = (baseColor.red * 0.88f + theme.accent.red * 0.12f).coerceIn(0f, 1f),
                  green = (baseColor.green * 0.88f + theme.accent.green * 0.12f).coerceIn(0f, 1f),
                  blue = (baseColor.blue * 0.88f + theme.accent.blue * 0.12f).coerceIn(0f, 1f),
                  alpha = theme.config.cardAlpha.coerceIn(0.85f, 1f),
                ),
            ),
          start = Offset(0f, 0f),
          end = Offset(900f, 650f),
        )

      CardVisualStyle.GLASS ->
        Brush.linearGradient(
          colors =
            listOf(
              baseColor.copy(alpha = theme.config.cardAlpha.coerceIn(0.35f, 0.72f)),
              theme.accent.copy(alpha = 0.08f),
            )
        )

      CardVisualStyle.MINIMAL ->
        Brush.linearGradient(colors = listOf(baseColor, baseColor))
    }

  val border =
    if (theme.borderWidth > 0.dp) BorderStroke(theme.borderWidth, theme.borderColor) else null

  Surface(
    modifier =
      modifier
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = shape,
    color = Color.Transparent,
    contentColor = theme.primaryText,
    border = border,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.background(backgroundBrush).padding(16.dp),
      content = content,
    )
  }
}

@Composable
fun SplitMoneyText(
  amountMinor: Long,
  currency: String,
  hide: Boolean = false,
  showPlus: Boolean = false,
  majorSize: TextUnit = 38.sp,
  minorSize: TextUnit = 21.sp,
  color: Color = LocalWorkspaceTheme.current.primaryText,
  modifier: Modifier = Modifier,
) {
  val sym = MoneyFormatter.symbolFor(currency)
  if (hide) {
    Text(
      text = "•••• $sym",
      style =
        TextStyle(
          fontFamily = LocalWorkspaceTheme.current.fontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = majorSize,
          fontFeatureSettings = "tnum",
        ),
      color = color,
      modifier = modifier,
    )
    return
  }

  val absVal = abs(amountMinor)
  val whole = absVal / 100L
  val frac = (absVal % 100L).toInt()
  val prefix =
    when {
      amountMinor < 0L -> "-"
      showPlus && amountMinor > 0L -> "+"
      else -> ""
    }

  val wholeFormatter = remember {
    DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' })
  }
  val wholeStr = "$prefix${wholeFormatter.format(whole)}"
  val fracStr = String.format(Locale.US, ".%02d %s", frac, sym)

  val annotated = buildAnnotatedString {
    withStyle(
      SpanStyle(
        fontSize = majorSize,
        fontWeight = FontWeight.Bold,
        color = color,
      )
    ) {
      append(wholeStr)
    }
    withStyle(
      SpanStyle(
        fontSize = minorSize,
        fontWeight = FontWeight.SemiBold,
        color = color.copy(alpha = 0.52f),
      )
    ) {
      append(fracStr)
    }
  }

  Text(
    text = annotated,
    style =
      TextStyle(
        fontFamily = LocalWorkspaceTheme.current.fontFamily,
        fontFeatureSettings = "tnum",
      ),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
  )
}

@Composable
fun WorkspaceSwitcherBar(
  workspaces: List<WorkspaceEntity>,
  selectedWorkspaceId: Long,
  onSelectWorkspace: (Long) -> Unit,
  onAddWorkspace: () -> Unit,
  includeAllOption: Boolean = true,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (includeAllOption) {
      val selected = selectedWorkspaceId == AppSettings.ALL_WORKSPACES_ID
      WorkspacePill(
        label = stringResource(R.string.all_workspaces),
        icon = Icons.Default.Public,
        selected = selected,
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onSelectWorkspace(AppSettings.ALL_WORKSPACES_ID)
        },
        modifier = Modifier.testTag("workspace_chip_all"),
      )
    }

    workspaces.forEach { ws ->
      val selected = selectedWorkspaceId == ws.id
      WorkspacePill(
        label = "${ws.emoji} ${ws.name}",
        icon = null,
        selected = selected,
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onSelectWorkspace(ws.id)
        },
        modifier = Modifier.testTag("workspace_chip_${ws.id}"),
      )
    }

    Surface(
      modifier =
        Modifier.size(36.dp)
          .clip(CircleShape)
          .clickable {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onAddWorkspace()
          }
          .testTag("add_workspace_chip"),
      shape = CircleShape,
      color = theme.accent.copy(alpha = 0.14f),
      border = BorderStroke(0.5.dp, theme.accent.copy(alpha = 0.45f)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = stringResource(R.string.new_workspace),
          tint = theme.accent,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun WorkspacePill(
  label: String,
  icon: ImageVector?,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val contentColor =
    if (selected) {
      if (theme.isLight) Color.White else Color(0xFF06120D)
    } else {
      theme.primaryText
    }
  Surface(
    modifier = modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    shape = RoundedCornerShape(50),
    color = if (selected) theme.accent else theme.cardSurfaceAlpha,
    border =
      BorderStroke(
        width = 0.5.dp,
        color = if (selected) theme.accent else theme.borderColor,
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

data class CategoryVisual(
  val key: String,
  val icon: ImageVector,
  val color: Color,
)

fun categoryVisualFor(categoryKey: String): CategoryVisual =
  when (categoryKey.uppercase()) {
    "GROCERIES" -> CategoryVisual("GROCERIES", Icons.Default.ShoppingCart, Color(0xFF10B981))
    "DINING" -> CategoryVisual("DINING", Icons.Default.LocalCafe, Color(0xFFF59E0B))
    "TRANSPORT" -> CategoryVisual("TRANSPORT", Icons.Default.DirectionsCar, Color(0xFF38BDF8))
    "UTILITIES" -> CategoryVisual("UTILITIES", Icons.Default.Home, Color(0xFF8B5CF6))
    "TECH" -> CategoryVisual("TECH", Icons.Default.LaptopMac, Color(0xFF6366F1))
    "HEALTH" -> CategoryVisual("HEALTH", Icons.Default.Favorite, Color(0xFFF43F5E))
    "ENTERTAINMENT" -> CategoryVisual("ENTERTAINMENT", Icons.Default.Movie, Color(0xFFEC4899))
    "SALARY" -> CategoryVisual("SALARY", Icons.Default.Payments, Color(0xFF22C55E))
    "CASHBACK" -> CategoryVisual("CASHBACK", Icons.Default.Redeem, Color(0xFF14B8A6))
    else -> CategoryVisual("OTHER", Icons.Default.ShoppingBag, Color(0xFF94A3B8))
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionSheet(
  workspaces: List<WorkspaceEntity>,
  accounts: List<AccountEntity>,
  activeWorkspace: WorkspaceEntity?,
  initialType: TransactionType,
  onDismiss: () -> Unit,
  onSave: (
    workspaceId: Long,
    accountId: Long,
    targetAccountId: Long?,
    amountMinor: Long,
    currency: String,
    type: TransactionType,
    category: String,
    title: String,
    note: String,
  ) -> Unit,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var txType by remember { mutableStateOf(initialType) }
  var rawDigits by remember { mutableStateOf("") }
  var titleText by remember { mutableStateOf("") }

  val defaultWs = activeWorkspace ?: workspaces.firstOrNull()
  val wsAccounts =
    remember(defaultWs?.id, accounts) {
      val list = if (defaultWs != null) accounts.filter { it.workspaceId == defaultWs.id } else accounts
      list.ifEmpty { accounts }
    }
  var selectedAccount by remember(wsAccounts) { mutableStateOf(wsAccounts.firstOrNull()) }
  var targetAccount by remember(accounts) {
    mutableStateOf(accounts.firstOrNull { it.id != selectedAccount?.id } ?: accounts.lastOrNull())
  }

  val categories =
    listOf(
      "GROCERIES" to stringResource(R.string.cat_groceries),
      "DINING" to stringResource(R.string.cat_dining),
      "TRANSPORT" to stringResource(R.string.cat_transport),
      "UTILITIES" to stringResource(R.string.cat_utilities),
      "TECH" to stringResource(R.string.cat_tech),
      "HEALTH" to stringResource(R.string.cat_health),
      "ENTERTAINMENT" to stringResource(R.string.cat_entertainment),
      "SALARY" to stringResource(R.string.cat_salary),
      "CASHBACK" to stringResource(R.string.cat_cashback),
      "OTHER" to stringResource(R.string.cat_other),
    )
  var selectedCategory by remember(txType) {
    mutableStateOf(if (txType == TransactionType.INCOME) "SALARY" else "GROCERIES")
  }

  val parsedDouble = rawDigits.toDoubleOrNull() ?: 0.0
  val parsedMinor = round(parsedDouble * 100.0).toLong()
  val currency = selectedAccount?.currency ?: defaultWs?.primaryCurrency ?: "UAH"
  val sym = MoneyFormatter.symbolFor(currency)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = theme.cardSurface,
    contentColor = theme.primaryText,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Segmented Type Switcher
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val types =
          listOf(
            TransactionType.EXPENSE to stringResource(R.string.transaction_type_expense),
            TransactionType.INCOME to stringResource(R.string.transaction_type_income),
            TransactionType.TRANSFER to stringResource(R.string.transaction_type_transfer),
          )
        types.forEachIndexed { idx, (type, label) ->
          SegmentedButton(
            selected = txType == type,
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              txType = type
            },
            shape = SegmentedButtonDefaults.itemShape(index = idx, count = types.size),
          ) {
            Text(label, maxLines = 1)
          }
        }
      }

      // Huge Live Amount Display
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val displayAmount = if (rawDigits.isEmpty()) "0" else rawDigits
        val amountColor =
          when (txType) {
            TransactionType.INCOME -> theme.income
            TransactionType.EXPENSE -> theme.expense
            TransactionType.TRANSFER -> theme.accent
          }
        AnimatedContent(
          targetState = "$displayAmount $sym",
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "amountDisplay",
        ) { formatted ->
          Text(
            text = formatted,
            style =
              TextStyle(
                fontFamily = theme.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                fontFeatureSettings = "tnum",
              ),
            color = amountColor,
          )
        }
      }

      // 1-Row Category Icon Pills
      if (txType != TransactionType.TRANSFER) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          categories.forEach { (key, label) ->
            val visual = categoryVisualFor(key)
            val isPicked = selectedCategory == key
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier =
                Modifier.clip(RoundedCornerShape(14.dp))
                  .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedCategory = key
                  }
                  .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(46.dp)
                    .clip(CircleShape)
                    .background(if (isPicked) visual.color else visual.color.copy(alpha = 0.14f))
                    .border(
                      width = if (isPicked) 2.dp else 0.5.dp,
                      color = if (isPicked) Color.White else visual.color.copy(alpha = 0.35f),
                      shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = visual.icon,
                  contentDescription = label,
                  tint = if (isPicked) Color.White else visual.color,
                  modifier = Modifier.size(20.dp),
                )
              }
              Spacer(Modifier.height(4.dp))
              Text(
                text = label.substringBefore(" "),
                style = MaterialTheme.typography.labelMedium,
                color = if (isPicked) theme.primaryText else theme.secondaryText,
                maxLines = 1,
              )
            }
          }
        }
      }

      // Account Picker Strip + Optional Title
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        accounts.forEach { acc ->
          val picked = selectedAccount?.id == acc.id
          FilterChip(
            selected = picked,
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              selectedAccount = acc
            },
            label = {
              Text("${acc.name} • ${MoneyFormatter.formatMinor(acc.balanceMinor, acc.currency)}")
            },
          )
        }
      }

      if (txType == TransactionType.TRANSFER && accounts.size > 1) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          accounts.filter { it.id != selectedAccount?.id }.forEach { acc ->
            val picked = targetAccount?.id == acc.id
            FilterChip(
              selected = picked,
              onClick = { targetAccount = acc },
              label = { Text("→ ${acc.name}") },
            )
          }
        }
      }

      OutlinedTextField(
        value = titleText,
        onValueChange = { titleText = it },
        placeholder = { Text(stringResource(R.string.optional_note_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("quick_sheet_title_input"),
      )

      // Built-in Fintech Numeric Keypad
      val keypadRows =
        listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf(".", "0", "DEL"),
        )
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keypadRows.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            row.forEach { key ->
              Surface(
                modifier =
                  Modifier.weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      when (key) {
                        "DEL" -> if (rawDigits.isNotEmpty()) rawDigits = rawDigits.dropLast(1)
                        "." -> {
                          if (!rawDigits.contains('.')) {
                            rawDigits = if (rawDigits.isEmpty()) "0." else "$rawDigits."
                          }
                        }
                        else -> {
                          val afterDot = rawDigits.substringAfter('.', "")
                          val hasDot = rawDigits.contains('.')
                          if ((!hasDot || afterDot.length < 2) && rawDigits.length < 10) {
                            rawDigits = if (rawDigits == "0") key else "$rawDigits$key"
                          }
                        }
                      }
                    }
                    .testTag("keypad_$key"),
                shape = RoundedCornerShape(14.dp),
                color = theme.background.copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, theme.borderColor),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (key == "DEL") {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Backspace,
                      contentDescription = "Backspace",
                      tint = theme.primaryText,
                      modifier = Modifier.size(20.dp),
                    )
                  } else {
                    Text(
                      text = key,
                      style = MaterialTheme.typography.titleLarge,
                      color = theme.primaryText,
                    )
                  }
                }
              }
            }
          }
        }
      }

      Button(
        onClick = {
          val acc = selectedAccount ?: return@Button
          if (parsedMinor <= 0L) return@Button
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          val resolvedTitle =
            titleText.trim().ifBlank {
              if (txType == TransactionType.TRANSFER) targetAccount?.name ?: "Transfer"
              else categories.firstOrNull { it.first == selectedCategory }?.second ?: selectedCategory
            }
          onSave(
            acc.workspaceId,
            acc.id,
            if (txType == TransactionType.TRANSFER) targetAccount?.id else null,
            parsedMinor,
            acc.currency,
            txType,
            if (txType == TransactionType.TRANSFER) "TRANSFER" else selectedCategory,
            resolvedTitle,
            "",
          )
          onDismiss()
        },
        enabled = parsedMinor > 0L && selectedAccount != null,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = if (txType == TransactionType.EXPENSE) theme.expense else theme.accent
          ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("quick_sheet_save_btn"),
      ) {
        Text(stringResource(R.string.save_tx_cta), style = MaterialTheme.typography.titleMedium)
      }
      Spacer(Modifier.height(6.dp))
    }
  }
}

@Composable
fun CashflowChartCanvas(
  transactions: List<TransactionEntity>,
  displayCurrency: String,
  rates: List<CurrencyRateEntity>,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val now = System.currentTimeMillis()
  val dayMs = 86_400_000L

  val buckets = remember(transactions, displayCurrency, rates) {
    (6 downTo 0).map { daysAgo ->
      val start = now - (daysAgo + 1) * dayMs
      val end = now - daysAgo * dayMs
      var inc = 0L
      var exp = 0L
      transactions.forEach { tx ->
        if (tx.timestamp in start until end) {
          val converted =
            CurrencyConverter.convertMinor(tx.amountMinor, tx.currency, displayCurrency, rates)
          if (tx.type == TransactionType.INCOME.name) inc += converted
          else if (tx.type == TransactionType.EXPENSE.name) exp += converted
        }
      }
      inc to exp
    }
  }

  val maxVal = (buckets.maxOfOrNull { maxOf(it.first, it.second) } ?: 1L).coerceAtLeast(1L)

  Canvas(modifier = modifier.fillMaxWidth().height(128.dp)) {
    val barGroupWidth = size.width / 7f
    val barWidth = (barGroupWidth * 0.28f).coerceAtLeast(8f)
    val chartHeight = size.height - 12.dp.toPx()

    drawLine(
      color = theme.borderColor,
      start = Offset(0f, chartHeight),
      end = Offset(size.width, chartHeight),
      strokeWidth = 0.5.dp.toPx(),
    )

    buckets.forEachIndexed { idx, (inc, exp) ->
      val cx = idx * barGroupWidth + barGroupWidth / 2f
      val incHeight = (inc.toFloat() / maxVal.toFloat()) * (chartHeight - 6.dp.toPx())
      val expHeight = (exp.toFloat() / maxVal.toFloat()) * (chartHeight - 6.dp.toPx())

      if (inc > 0L) {
        drawRoundRect(
          color = theme.income,
          topLeft = Offset(cx - barWidth - 2.dp.toPx(), chartHeight - incHeight),
          size = Size(barWidth, incHeight.coerceAtLeast(4.dp.toPx())),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
      }
      if (exp > 0L) {
        drawRoundRect(
          color = theme.expense,
          topLeft = Offset(cx + 2.dp.toPx(), chartHeight - expHeight),
          size = Size(barWidth, expHeight.coerceAtLeast(4.dp.toPx())),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
      }
    }
  }
}

@Composable
fun CategoryBreakdownRing(
  slices: List<Pair<String, Long>>,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val palette =
    listOf(
      theme.accent,
      theme.expense,
      theme.income,
      Color(0xFF38BDF8),
      Color(0xFFFBBF24),
      Color(0xFFA78BFA),
      Color(0xFFF472B6),
    )
  val total = slices.sumOf { it.second }.coerceAtLeast(1L)

  Canvas(modifier = modifier.size(130.dp)) {
    val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
    var startAngle = -90f
    if (slices.isEmpty()) {
      drawArc(
        color = theme.borderColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = stroke,
      )
      return@Canvas
    }
    slices.forEachIndexed { index, (_, amount) ->
      val sweep = (amount.toFloat() / total.toFloat()) * 360f
      if (sweep > 1f) {
        drawArc(
          color = palette[index % palette.size],
          startAngle = startAngle,
          sweepAngle = (sweep - 3f).coerceAtLeast(1f),
          useCenter = false,
          style = stroke,
        )
        startAngle += sweep
      }
    }
  }
}

@Composable
fun DesignerColorDialog(
  title: String,
  initialHex: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var selectedHex by remember(initialHex) { mutableStateOf(initialHex.uppercase()) }
  val curatedColors =
    listOf(
      "#0D0E12", "#16181F", "#1B202B", "#242B38",
      "#080612", "#141029", "#140D16", "#221526",
      "#F5F5F7", "#FFFFFF", "#F9FAFB", "#9CA3AF",
      "#10B981", "#00F5D4", "#007AFF", "#38BDF8",
      "#8B5CF6", "#EC4899", "#F43F5E", "#F97316",
      "#FBBF24", "#A3BE8C", "#22C55E", "#111827",
    )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        curatedColors.chunked(6).forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            row.forEach { hex ->
              val picked = selectedHex.equals(hex, ignoreCase = true)
              Box(
                modifier =
                  Modifier.size(38.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(hex))
                    .border(
                      width = if (picked) 2.5.dp else 0.5.dp,
                      color = if (picked) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                      shape = CircleShape,
                    )
                    .clickable { selectedHex = hex },
                contentAlignment = Alignment.Center,
              ) {
                if (picked) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (hex in listOf("#FFFFFF", "#F5F5F7", "#F9FAFB", "#00F5D4")) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }
        OutlinedTextField(
          value = selectedHex,
          onValueChange = { selectedHex = it.take(9) },
          label = { Text(stringResource(R.string.hex_code)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(onClick = { onConfirm(selectedHex) }) {
        Text(stringResource(R.string.apply))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}

@Composable
fun AddWorkspaceDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, emoji: String, currency: String, preset: WorkspaceThemeConfig, initialBalanceMinor: Long) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var emoji by remember { mutableStateOf("💼") }
  var currency by remember { mutableStateOf("UAH") }
  var balanceText by remember { mutableStateOf("") }
  var selectedPreset by remember { mutableStateOf(ThemePresets.obsidian) }

  val emojis = listOf("🍔", "💼", "🚀", "🏠", "💳", "✈️", "🎮", "🛡️")
  val currencies = listOf("UAH", "USD", "EUR", "PLN", "GBP")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.new_workspace)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.workspace_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("workspace_name_input"),
        )
        OutlinedTextField(
          value = balanceText,
          onValueChange = { balanceText = it },
          label = { Text("${stringResource(R.string.initial_balance)} ($currency)") },
          placeholder = { Text("0.00") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          emojis.forEach { e ->
            FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) })
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          currencies.forEach { cur ->
            FilterChip(selected = currency == cur, onClick = { currency = cur }, label = { Text(cur) })
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ThemePresets.all.forEach { preset ->
            FilterChip(
              selected = selectedPreset.presetName == preset.presetName,
              onClick = { selectedPreset = preset },
              label = { Text(preset.presetName) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            val parsed = balanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
            val minor = round(parsed * 100.0).toLong()
            onCreate(name, emoji, currency, selectedPreset, minor)
          }
        },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("confirm_create_workspace"),
      ) {
        Text(stringResource(R.string.create))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}

@Composable
fun EditWorkspaceDialog(
  workspace: WorkspaceEntity,
  onDismiss: () -> Unit,
  onSave: (name: String, emoji: String) -> Unit,
  onCustomizeTheme: () -> Unit,
  onDelete: () -> Unit,
) {
  var name by remember(workspace.id) { mutableStateOf(workspace.name) }
  var emoji by remember(workspace.id) { mutableStateOf(workspace.emoji) }
  val emojis = listOf("🍔", "💼", "🚀", "🏠", "💳", "✈️", "🎮", "🛡️")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.edit_workspace)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.workspace_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          emojis.forEach { e ->
            FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) })
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TextButton(onClick = onCustomizeTheme, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.theme_studio_title))
          }
          TextButton(onClick = onDelete) {
            Text(stringResource(R.string.delete_workspace), color = MaterialTheme.colorScheme.error)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { if (name.isNotBlank()) onSave(name, emoji) }, enabled = name.isNotBlank()) {
        Text(stringResource(R.string.save_changes))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}

@Composable
fun AddAccountDialog(
  defaultCurrency: String,
  onDismiss: () -> Unit,
  onCreate: (name: String, bankType: BankType, balanceMinor: Long, currency: String, pan: String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var balanceText by remember { mutableStateOf("0") }
  var pan by remember { mutableStateOf("") }
  var currency by remember { mutableStateOf(defaultCurrency) }
  var bankType by remember { mutableStateOf(BankType.MANUAL) }

  val currencies = listOf("UAH", "USD", "EUR", "PLN", "GBP")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.add_account)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.account_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = balanceText,
          onValueChange = { balanceText = it },
          label = { Text(stringResource(R.string.initial_balance)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = pan,
          onValueChange = { pan = it.take(4) },
          label = { Text(stringResource(R.string.last_four_digits)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BankType.entries.forEach { bt ->
            FilterChip(selected = bankType == bt, onClick = { bankType = bt }, label = { Text(bt.name) })
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          currencies.forEach { cur ->
            FilterChip(selected = currency == cur, onClick = { currency = cur }, label = { Text(cur) })
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = balanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
          val minor = round(parsed * 100.0).toLong()
          val panMasked = if (pan.isNotBlank()) "•••• $pan" else ""
          onCreate(name, bankType, minor, currency, panMasked)
        },
        enabled = name.isNotBlank(),
      ) {
        Text(stringResource(R.string.create))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
