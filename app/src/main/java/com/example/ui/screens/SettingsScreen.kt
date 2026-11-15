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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.VaultCard
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.viewmodel.VaultUiState
import com.example.util.BiometricHelper

@Composable
fun SettingsScreen(
  uiState: VaultUiState,
  onToggleBiometric: (Boolean) -> Unit,
  onToggleShakeToHide: (Boolean) -> Unit,
  onToggleHideBalances: () -> Unit,
  onChangeLanguage: (String) -> Unit,
  onChangeGlobalCurrency: (String) -> Unit,
  onPopulateDemo: () -> Unit,
  onClearAllData: () -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  val biometricUnavailableMsg = stringResource(R.string.biometric_unavailable)
  var showClearConfirm by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item {
      Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        color = theme.primaryText,
      )
    }

    // Security & Privacy
    item {
      VaultCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.security_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))

        SettingSwitchRow(
          icon = Icons.Default.Fingerprint,
          title = stringResource(R.string.biometric_lock),
          subtitle = stringResource(R.string.biometric_lock_desc),
          checked = uiState.settings.biometricLockEnabled,
          onCheckedChange = { enable ->
            if (enable && !BiometricHelper.isAvailable(context)) {
              onShowMessage(biometricUnavailableMsg)
            } else {
              onToggleBiometric(enable)
            }
          },
          modifier = Modifier.testTag("biometric_lock_switch"),
        )

        Spacer(Modifier.height(10.dp))
        SettingSwitchRow(
          icon = Icons.Default.Vibration,
          title = stringResource(R.string.shake_to_hide),
          subtitle = stringResource(R.string.shake_to_hide_desc),
          checked = uiState.settings.shakeToHideEnabled,
          onCheckedChange = onToggleShakeToHide,
          modifier = Modifier.testTag("shake_hide_switch"),
        )

        Spacer(Modifier.height(10.dp))
        SettingSwitchRow(
          icon = Icons.Default.VisibilityOff,
          title = stringResource(R.string.tap_to_hide),
          subtitle = stringResource(R.string.privacy_hidden_hint),
          checked = uiState.settings.hideBalances,
          onCheckedChange = { onToggleHideBalances() },
          modifier = Modifier.testTag("privacy_mode_switch"),
        )
      }
    }

    // Language & Global Currency
    item {
      VaultCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.language_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.app_language), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val langs =
            listOf(
              "system" to stringResource(R.string.lang_system),
              "en" to stringResource(R.string.lang_en),
              "uk" to stringResource(R.string.lang_uk),
            )
          langs.forEach { (tag, label) ->
            FilterChip(
              selected = uiState.settings.appLanguage == tag,
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChangeLanguage(tag)
              },
              label = { Text(label) },
              modifier = Modifier.testTag("lang_chip_$tag"),
            )
          }
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.global_currency), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("UAH", "USD", "EUR", "PLN", "GBP").forEach { cur ->
            FilterChip(
              selected = uiState.settings.globalCurrency == cur,
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChangeGlobalCurrency(cur)
              },
              label = { Text(cur) },
            )
          }
        }
      }
    }

    // Demo Mode & Reset
    item {
      VaultCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.demo_section), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
          stringResource(R.string.fill_demo_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = theme.secondaryText,
        )
        Spacer(Modifier.height(12.dp))
        Button(
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPopulateDemo()
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("settings_load_demo_btn"),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.fill_demo_data))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
          onClick = { showClearConfirm = true },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("clear_all_data_btn"),
        ) {
          Icon(Icons.Default.DeleteForever, contentDescription = null, tint = theme.expense, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.clear_all_data), color = theme.expense)
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text(stringResource(R.string.clear_all_data)) },
      text = { Text(stringResource(R.string.fill_demo_desc)) },
      confirmButton = {
        Button(
          onClick = {
            onClearAllData()
            showClearConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.expense),
        ) {
          Text(stringResource(R.string.delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }
}

@Composable
private fun SettingSwitchRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(38.dp)
          .clip(CircleShape)
          .background(theme.accent.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge, color = theme.primaryText)
      Text(subtitle, style = MaterialTheme.typography.labelMedium, color = theme.secondaryText)
    }
    Switch(
      checked = checked,
      onCheckedChange = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onCheckedChange(it)
      },
    )
  }
}
