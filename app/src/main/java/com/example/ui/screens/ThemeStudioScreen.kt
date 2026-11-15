package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AddWorkspaceDialog
import com.example.ui.components.DesignerColorDialog
import com.example.ui.components.SplitMoneyText
import com.example.ui.components.VaultBackground
import com.example.ui.components.VaultCard
import com.example.ui.components.WorkspaceSwitcherBar
import com.example.ui.theme.CardVisualStyle
import com.example.ui.theme.HomeBlockType
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.theme.ThemeFontStyle
import com.example.ui.theme.ThemePresets
import com.example.ui.theme.VaultFlowTheme
import com.example.ui.theme.WorkspaceThemeConfig
import com.example.ui.theme.WorkspaceThemeJson
import com.example.ui.theme.fontFamilyFor
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.VaultUiState

private enum class StudioColorSlot {
  ACCENT,
  BACKGROUND,
  CARD,
  INCOME,
  EXPENSE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioScreen(
  uiState: VaultUiState,
  onBack: () -> Unit,
  onSelectWorkspace: (Long) -> Unit,
  onCreateWorkspace: (String, String, String, WorkspaceThemeConfig, Long, String) -> Unit,
  onUpdateTheme: (Long, WorkspaceThemeConfig) -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  val targetWorkspace = uiState.activeWorkspace ?: uiState.workspaces.firstOrNull()

  var selectedTab by remember { mutableIntStateOf(0) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var activeColorSlot by remember { mutableStateOf<StudioColorSlot?>(null) }
  var showImportDialog by remember { mutableStateOf(false) }
  var showAddWs by remember { mutableStateOf(false) }

  val config =
    remember(targetWorkspace?.themeJson) {
      targetWorkspace?.let { WorkspaceThemeJson.decodeOrDefault(it.themeJson) }
        ?: ThemePresets.obsidian
    }

  val copiedMsg = stringResource(R.string.theme_copied_clipboard)
  val importedMsg = stringResource(R.string.theme_imported_success)
  val importErrMsg = stringResource(R.string.theme_import_error)
  val defaultAccLabel = stringResource(R.string.main_account_default)

  fun applyConfig(updated: WorkspaceThemeConfig) {
    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    if (targetWorkspace != null) {
      onUpdateTheme(targetWorkspace.id, updated)
    }
  }

  VaultBackground {
    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(stringResource(R.string.theme_studio_title), color = theme.primaryText)
              Text(
                text =
                  if (targetWorkspace != null) "${targetWorkspace.emoji} ${targetWorkspace.name}"
                  else stringResource(R.string.theme_select_workspace_hint),
                style = MaterialTheme.typography.labelMedium,
                color = theme.secondaryText,
              )
            }
          },
          navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("studio_back_button")) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.close),
                tint = theme.primaryText,
              )
            }
          },
          actions = {
            Box {
              IconButton(
                onClick = { showOverflowMenu = true },
                modifier = Modifier.testTag("studio_overflow_menu"),
              ) {
                Icon(Icons.Default.MoreVert, contentDescription = "JSON Options", tint = theme.primaryText)
              }
              DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
              ) {
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.share_theme_json)) },
                  onClick = {
                    val jsonStr = WorkspaceThemeJson.encode(config)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("VaultFlow Theme JSON", jsonStr))
                    onShowMessage(copiedMsg)
                    showOverflowMenu = false
                  },
                )
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.paste_theme_json_menu)) },
                  onClick = {
                    showOverflowMenu = false
                    showImportDialog = true
                  },
                )
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
      },
    ) { innerPadding ->
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        WorkspaceSwitcherBar(
          workspaces = uiState.workspaces,
          selectedWorkspaceId = targetWorkspace?.id ?: -1L,
          onSelectWorkspace = onSelectWorkspace,
          onAddWorkspace = { showAddWs = true },
          includeAllOption = false,
        )

        // Pinned Interactive Live Preview
        VaultFlowTheme(themeConfig = config) {
          val previewTheme = LocalWorkspaceTheme.current
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(previewTheme.cornerRadius))
                .background(previewTheme.background)
                .border(0.5.dp, previewTheme.borderColor, RoundedCornerShape(previewTheme.cornerRadius))
                .padding(14.dp)
          ) {
            VaultCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = targetWorkspace?.let { "${it.emoji} ${it.name}" } ?: "VaultFlow",
                    style = MaterialTheme.typography.labelMedium,
                    color = previewTheme.secondaryText,
                  )
                  Spacer(Modifier.height(2.dp))
                  SplitMoneyText(
                    amountMinor = 48_250_40L,
                    currency = targetWorkspace?.primaryCurrency ?: "UAH",
                    majorSize = 26.sp,
                    minorSize = 15.sp,
                    color = previewTheme.primaryText,
                  )
                }
                Surface(
                  shape = RoundedCornerShape(50),
                  color = previewTheme.accent,
                ) {
                  Text(
                    text = config.presetName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (previewTheme.isLight) Color.White else Color(0xFF06120D),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }
              Spacer(Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("↑ +18 400 ₴", style = MaterialTheme.typography.labelLarge, color = previewTheme.income)
                Text("↓ -6 120 ₴", style = MaterialTheme.typography.labelLarge, color = previewTheme.expense)
              }
            }
          }
        }

        // 3 Clean Bottom-Style Studio Tabs
        val tabs =
          listOf(
            stringResource(R.string.studio_tab_style_preset),
            stringResource(R.string.studio_tab_shape),
            stringResource(R.string.studio_tab_blocks),
          )
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
          tabs.forEachIndexed { idx, label ->
            SegmentedButton(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
            ) {
              Text(label, maxLines = 1)
            }
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          when (selectedTab) {
            0 -> {
              // Tab 1: 1-Click Designer Presets + Accent Color Circle Grid
              item {
                VaultCard(modifier = Modifier.fillMaxWidth()) {
                  Text(stringResource(R.string.theme_presets), style = MaterialTheme.typography.titleMedium)
                  Spacer(Modifier.height(12.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    ThemePresets.all.forEach { preset ->
                      val isSelected = config.presetName == preset.presetName
                      val bg = parseHexColor(preset.backgroundColorHex)
                      val card = parseHexColor(preset.cardColorHex)
                      val accent = parseHexColor(preset.accentColorHex)
                      val textCol = parseHexColor(preset.primaryTextColorHex)

                      Surface(
                        modifier =
                          Modifier.width(126.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { applyConfig(preset.copy(homeBlocks = config.homeBlocks)) }
                            .testTag("preset_${preset.presetName.lowercase().replace(' ', '_')}"),
                        shape = RoundedCornerShape(16.dp),
                        color = bg,
                        border =
                          BorderStroke(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) theme.accent else card,
                          ),
                      ) {
                        Column(
                          modifier = Modifier.padding(12.dp),
                          verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                              Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                              Box(Modifier.size(12.dp).clip(CircleShape).background(card))
                            }
                            if (isSelected) {
                              Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                            }
                          }
                          Text(
                            text = preset.presetName,
                            style = MaterialTheme.typography.labelLarge,
                            color = textCol,
                            fontFamily = fontFamilyFor(preset.fontStyle),
                            maxLines = 1,
                          )
                        }
                      }
                    }
                  }
                }
              }

              // Curated Accent Color Circles + Palette Swatches
              item {
                VaultCard(modifier = Modifier.fillMaxWidth()) {
                  Text(stringResource(R.string.quick_accent_tint), style = MaterialTheme.typography.titleMedium)
                  Spacer(Modifier.height(12.dp))
                  val accentCircles =
                    listOf(
                      "#10B981", "#00F5D4", "#007AFF", "#38BDF8",
                      "#8B5CF6", "#EC4899", "#F43F5E", "#F97316",
                      "#FBBF24", "#88C0D0",
                    )
                  accentCircles.chunked(5).forEach { row ->
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      row.forEach { hex ->
                        val picked = config.accentColorHex.equals(hex, ignoreCase = true)
                        Box(
                          modifier =
                            Modifier.size(44.dp)
                              .clip(CircleShape)
                              .background(parseHexColor(hex))
                              .border(
                                width = if (picked) 2.5.dp else 0.5.dp,
                                color = if (picked) theme.primaryText else theme.borderColor,
                                shape = CircleShape,
                              )
                              .clickable { applyConfig(config.copy(presetName = "Custom", accentColorHex = hex)) },
                          contentAlignment = Alignment.Center,
                        ) {
                          if (picked) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                          }
                        }
                      }
                    }
                  }

                  Spacer(Modifier.height(12.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    val slots =
                      listOf(
                        Triple(StudioColorSlot.BACKGROUND, stringResource(R.string.color_background), config.backgroundColorHex),
                        Triple(StudioColorSlot.CARD, stringResource(R.string.color_card), config.cardColorHex),
                        Triple(StudioColorSlot.INCOME, stringResource(R.string.color_income), config.incomeColorHex),
                        Triple(StudioColorSlot.EXPENSE, stringResource(R.string.color_expense), config.expenseColorHex),
                      )
                    slots.forEach { (slot, label, hex) ->
                      Surface(
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { activeColorSlot = slot },
                        shape = RoundedCornerShape(50),
                        color = theme.background.copy(alpha = 0.55f),
                        border = BorderStroke(0.5.dp, theme.borderColor),
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          Box(
                            Modifier.size(14.dp)
                              .clip(CircleShape)
                              .background(parseHexColor(hex))
                              .border(0.5.dp, theme.borderColor, CircleShape)
                          )
                          Text(label, style = MaterialTheme.typography.labelMedium, color = theme.primaryText)
                        }
                      }
                    }
                  }
                }
              }
            }

            1 -> {
              // Tab 2: Clean 3-Pick Shape Controls (Corners / Card Style / Font)
              item {
                VaultCard(modifier = Modifier.fillMaxWidth()) {
                  Text(stringResource(R.string.corners_label), style = MaterialTheme.typography.titleMedium)
                  Spacer(Modifier.height(8.dp))
                  val cornerModes =
                    listOf(
                      stringResource(R.string.corners_sharp) to 6f,
                      stringResource(R.string.corners_soft) to 18f,
                      stringResource(R.string.corners_round) to 28f,
                    )
                  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    cornerModes.forEachIndexed { idx, (label, dpVal) ->
                      val selected =
                        when (idx) {
                          0 -> config.cornerRadiusDp <= 10f
                          1 -> config.cornerRadiusDp in 10.1f..22f
                          else -> config.cornerRadiusDp > 22f
                        }
                      SegmentedButton(
                        selected = selected,
                        onClick = { applyConfig(config.copy(presetName = "Custom", cornerRadiusDp = dpVal)) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = cornerModes.size),
                      ) {
                        Text(label, maxLines = 1)
                      }
                    }
                  }

                  Spacer(Modifier.height(16.dp))
                  Text(stringResource(R.string.card_style_label), style = MaterialTheme.typography.titleMedium)
                  Spacer(Modifier.height(8.dp))
                  val cardModes =
                    listOf(
                      stringResource(R.string.card_style_gradient) to CardVisualStyle.GRADIENT,
                      stringResource(R.string.card_style_glass) to CardVisualStyle.GLASS,
                      stringResource(R.string.card_style_minimal) to CardVisualStyle.MINIMAL,
                    )
                  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    cardModes.forEachIndexed { idx, (label, style) ->
                      SegmentedButton(
                        selected = config.cardStyle == style,
                        onClick = { applyConfig(config.copy(presetName = "Custom", cardStyle = style)) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = cardModes.size),
                      ) {
                        Text(label, maxLines = 1)
                      }
                    }
                  }

                  Spacer(Modifier.height(16.dp))
                  Text(stringResource(R.string.font_style), style = MaterialTheme.typography.titleMedium)
                  Spacer(Modifier.height(8.dp))
                  val fontModes =
                    listOf(
                      stringResource(R.string.font_modern) to ThemeFontStyle.DEFAULT,
                      stringResource(R.string.font_tech) to ThemeFontStyle.MONOSPACE,
                      stringResource(R.string.font_classic) to ThemeFontStyle.SERIF,
                    )
                  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    fontModes.forEachIndexed { idx, (label, font) ->
                      SegmentedButton(
                        selected = config.fontStyle == font,
                        onClick = { applyConfig(config.copy(presetName = "Custom", fontStyle = font)) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = fontModes.size),
                      ) {
                        Text(label, maxLines = 1, fontFamily = fontFamilyFor(font))
                      }
                    }
                  }
                }
              }
            }

            2 -> {
              // Tab 3: Home Screen Blocks Checklist + Order
              item {
                VaultCard(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(stringResource(R.string.layout_builder), style = MaterialTheme.typography.titleMedium)
                    TextButton(
                      onClick = { applyConfig(config.copy(homeBlocks = WorkspaceThemeConfig.defaultHomeBlocks())) }
                    ) {
                      Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(stringResource(R.string.reset_layout))
                    }
                  }
                  Spacer(Modifier.height(8.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    config.homeBlocks.forEachIndexed { index, block ->
                      val blockName =
                        when (block.type) {
                          HomeBlockType.BALANCE -> stringResource(R.string.block_balance)
                          HomeBlockType.ACCOUNTS -> stringResource(R.string.block_accounts)
                          HomeBlockType.QUICK_ACTIONS -> stringResource(R.string.block_quick_actions)
                          HomeBlockType.CHART -> stringResource(R.string.block_chart)
                          HomeBlockType.GOALS -> stringResource(R.string.block_goals)
                          HomeBlockType.SUBSCRIPTIONS -> stringResource(R.string.block_subscriptions)
                          HomeBlockType.TRANSACTIONS -> stringResource(R.string.block_transactions)
                        }
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = theme.background.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, theme.borderColor),
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                          Text(
                            text = blockName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = theme.primaryText,
                            modifier = Modifier.weight(1f),
                          )
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                              onClick = {
                                if (index > 0) {
                                  val list = config.homeBlocks.toMutableList()
                                  val item = list.removeAt(index)
                                  list.add(index - 1, item)
                                  applyConfig(config.copy(homeBlocks = list))
                                }
                              },
                              enabled = index > 0,
                            ) {
                              Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(
                              onClick = {
                                if (index < config.homeBlocks.lastIndex) {
                                  val list = config.homeBlocks.toMutableList()
                                  val item = list.removeAt(index)
                                  list.add(index + 1, item)
                                  applyConfig(config.copy(homeBlocks = list))
                                }
                              },
                              enabled = index < config.homeBlocks.lastIndex,
                            ) {
                              Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                            }
                            Switch(
                              checked = block.visible,
                              onCheckedChange = { checked ->
                                val list = config.homeBlocks.toMutableList()
                                list[index] = block.copy(visible = checked)
                                applyConfig(config.copy(homeBlocks = list))
                              },
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  activeColorSlot?.let { slot ->
    val currentHex =
      when (slot) {
        StudioColorSlot.ACCENT -> config.accentColorHex
        StudioColorSlot.BACKGROUND -> config.backgroundColorHex
        StudioColorSlot.CARD -> config.cardColorHex
        StudioColorSlot.INCOME -> config.incomeColorHex
        StudioColorSlot.EXPENSE -> config.expenseColorHex
      }
    DesignerColorDialog(
      title = stringResource(R.string.pick_color),
      initialHex = currentHex,
      onDismiss = { activeColorSlot = null },
      onConfirm = { newHex ->
        val updated =
          when (slot) {
            StudioColorSlot.ACCENT -> config.copy(presetName = "Custom", accentColorHex = newHex)
            StudioColorSlot.BACKGROUND -> config.copy(presetName = "Custom", backgroundColorHex = newHex)
            StudioColorSlot.CARD -> config.copy(presetName = "Custom", cardColorHex = newHex)
            StudioColorSlot.INCOME -> config.copy(presetName = "Custom", incomeColorHex = newHex)
            StudioColorSlot.EXPENSE -> config.copy(presetName = "Custom", expenseColorHex = newHex)
          }
        applyConfig(updated)
        activeColorSlot = null
      },
    )
  }

  if (showImportDialog) {
    var jsonInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showImportDialog = false },
      title = { Text(stringResource(R.string.import_theme_json)) },
      text = {
        OutlinedTextField(
          value = jsonInput,
          onValueChange = { jsonInput = it },
          label = { Text(stringResource(R.string.paste_theme_json)) },
          minLines = 5,
          maxLines = 10,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val parsed = WorkspaceThemeJson.decodeOrNull(jsonInput)
            if (parsed != null) {
              applyConfig(parsed)
              onShowMessage(importedMsg)
              showImportDialog = false
            } else {
              onShowMessage(importErrMsg)
            }
          }
        ) {
          Text(stringResource(R.string.apply))
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportDialog = false }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }

  if (showAddWs) {
    AddWorkspaceDialog(
      onDismiss = { showAddWs = false },
      onCreate = { name, emoji, currency, preset, initialBalanceMinor ->
        onCreateWorkspace(name, emoji, currency, preset, initialBalanceMinor, defaultAccLabel)
        showAddWs = false
      },
    )
  }
}
