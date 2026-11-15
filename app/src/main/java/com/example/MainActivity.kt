package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.data.repository.SyncResult
import com.example.ui.components.QuickAddTransactionSheet
import com.example.ui.components.VaultBackground
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BanksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeStudioScreen
import com.example.ui.theme.LocalWorkspaceTheme
import com.example.ui.theme.VaultFlowTheme
import com.example.ui.viewmodel.UiEvent
import com.example.ui.viewmodel.VaultViewModel
import com.example.util.BiometricHelper
import com.example.util.LocaleHelper
import com.example.util.ShakeDetector
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object AnalyticsRoute
@Serializable object BanksRoute
@Serializable object ThemeStudioRoute
@Serializable object SettingsRoute

private data class NavTab(
  val route: Any,
  val labelRes: Int,
  val icon: ImageVector,
  val tag: String,
)

class MainActivity : FragmentActivity() {

  override fun attachBaseContext(newBase: Context) {
    val lang = UserPreferences.getLanguageSync(newBase)
    super.attachBaseContext(LocaleHelper.wrapContext(newBase, lang))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val vm: VaultViewModel = viewModel()
      val uiState by vm.uiState.collectAsStateWithLifecycle()
      val snackbarHostState = remember { SnackbarHostState() }
      var isUnlocked by rememberSaveable { mutableStateOf(false) }

      DisposableEffect(uiState.settings.shakeToHideEnabled) {
        val detector =
          if (uiState.settings.shakeToHideEnabled) {
            ShakeDetector(this@MainActivity) { vm.toggleHideBalances() }.also { it.start() }
          } else null
        onDispose { detector?.stop() }
      }

      val syncSuccessFmt = stringResource(R.string.sync_success)
      val cooldownFmt = stringResource(R.string.sync_cooldown)
      val ratesUpdatedMsg = stringResource(R.string.currency_rates_updated)
      val demoLoadedMsg = stringResource(R.string.demo_loaded_msg)
      val unlockTitle = stringResource(R.string.unlock_vaultflow)
      val unlockSub = stringResource(R.string.unlock_subtitle)

      LaunchedEffect(Unit) {
        vm.events.collect { event ->
          when (event) {
            is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
            is UiEvent.SyncFeedback -> {
              val msg =
                when (val r = event.result) {
                  is SyncResult.Success -> String.format(syncSuccessFmt, r.accountsUpdated, r.newTransactions)
                  is SyncResult.RateLimited -> String.format(cooldownFmt, r.remainingSeconds)
                  is SyncResult.Error -> r.message
                }
              snackbarHostState.showSnackbar(msg)
            }
            UiEvent.RecreateForLocale -> recreate()
          }
        }
      }

      VaultFlowTheme(themeConfig = uiState.activeTheme) {
        val theme = LocalWorkspaceTheme.current
        val needsAuth = uiState.settings.biometricLockEnabled && !isUnlocked

        LaunchedEffect(needsAuth) {
          if (needsAuth && BiometricHelper.isAvailable(this@MainActivity)) {
            BiometricHelper.authenticate(
              activity = this@MainActivity,
              title = unlockTitle,
              subtitle = unlockSub,
              onSuccess = { isUnlocked = true },
              onError = {},
            )
          }
        }

        if (needsAuth) {
          VaultBackground {
            Column(
              modifier = Modifier.fillMaxSize().padding(24.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(64.dp),
              )
              Spacer(Modifier.height(16.dp))
              Text(unlockTitle, style = MaterialTheme.typography.headlineMedium, color = theme.primaryText)
              Spacer(Modifier.height(8.dp))
              Text(unlockSub, style = MaterialTheme.typography.bodyMedium, color = theme.secondaryText)
              Spacer(Modifier.height(24.dp))
              Button(
                onClick = {
                  if (BiometricHelper.isAvailable(this@MainActivity)) {
                    BiometricHelper.authenticate(
                      activity = this@MainActivity,
                      title = unlockTitle,
                      subtitle = unlockSub,
                      onSuccess = { isUnlocked = true },
                      onError = {},
                    )
                  } else {
                    isUnlocked = true
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              ) {
                Text(stringResource(R.string.unlock_button))
              }
            }
          }
        } else {
          VaultFlowMainScaffold(
            vm = vm,
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            ratesUpdatedMsg = ratesUpdatedMsg,
            demoLoadedMsg = demoLoadedMsg,
          )
        }
      }
    }
  }
}

@Composable
private fun VaultFlowMainScaffold(
  vm: VaultViewModel,
  uiState: com.example.ui.viewmodel.VaultUiState,
  snackbarHostState: SnackbarHostState,
  ratesUpdatedMsg: String,
  demoLoadedMsg: String,
) {
  val theme = LocalWorkspaceTheme.current
  val haptics = LocalHapticFeedback.current
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = navBackStackEntry?.destination
  val isStudioRoute = currentDest?.hasRoute(ThemeStudioRoute::class) == true

  var quickSheetType by remember { mutableStateOf<TransactionType?>(null) }

  val tabs =
    listOf(
      NavTab(HomeRoute, R.string.nav_home, Icons.Default.Home, "nav_home"),
      NavTab(AnalyticsRoute, R.string.nav_analytics, Icons.Default.Analytics, "nav_analytics"),
      NavTab(BanksRoute, R.string.nav_banks, Icons.Default.AccountBalance, "nav_banks"),
      NavTab(SettingsRoute, R.string.nav_settings, Icons.Default.Settings, "nav_settings"),
    )

  fun navigateToTab(route: Any) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  VaultBackground {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 600.dp

      Row(modifier = Modifier.fillMaxSize()) {
        if (useRail && !isStudioRoute) {
          NavigationRail(
            containerColor = theme.cardSurfaceAlpha,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
          ) {
            Spacer(Modifier.height(12.dp))
            tabs.forEach { tab ->
              val selected = currentDest?.hasRoute(tab.route::class) == true
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                modifier = Modifier.testTag(tab.tag),
              )
            }
          }
        }

        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.statusBars,
          snackbarHost = { SnackbarHost(snackbarHostState) },
          floatingActionButton = {
            if (currentDest?.hasRoute(HomeRoute::class) == true && uiState.allAccounts.isNotEmpty()) {
              FloatingActionButton(
                onClick = {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  quickSheetType = TransactionType.EXPENSE
                },
                shape = CircleShape,
                containerColor = theme.accent,
                contentColor = if (theme.isLight) Color.White else Color(0xFF06120D),
                modifier = Modifier.testTag("fab_add_transaction"),
              ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction))
              }
            }
          },
          bottomBar = {
            if (!useRail && !isStudioRoute) {
              NavigationBar(
                containerColor = theme.cardSurface.copy(alpha = 0.94f),
                windowInsets = WindowInsets.navigationBars,
              ) {
                tabs.forEach { tab ->
                  val selected = currentDest?.hasRoute(tab.route::class) == true
                  NavigationBarItem(
                    selected = selected,
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      navigateToTab(tab.route)
                    },
                    icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                    label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                    modifier = Modifier.testTag(tab.tag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = HomeRoute) {
              composable<HomeRoute> {
                HomeScreen(
                  uiState = uiState,
                  onSelectWorkspace = vm::selectWorkspace,
                  onCreateWorkspace = vm::createWorkspace,
                  onUpdateWorkspace = vm::updateWorkspaceDetails,
                  onDeleteWorkspace = vm::deleteWorkspace,
                  onAddAccount = vm::addAccount,
                  onTogglePrivacy = vm::toggleHideBalances,
                  onOpenQuickTransaction = { type -> quickSheetType = type },
                  onOpenGoalsTab = { navigateToTab(AnalyticsRoute) },
                  onTopUpGoal = vm::topUpGoal,
                  onMarkSubscriptionPaid = vm::markSubscriptionPaid,
                  onDeleteTransaction = vm::deleteTransaction,
                  onOpenThemeStudio = { navController.navigate(ThemeStudioRoute) },
                  onLoadDemo = { vm.populateDemoData(demoLoadedMsg) },
                )
              }

              composable<AnalyticsRoute> {
                AnalyticsScreen(
                  uiState = uiState,
                  onRefreshRates = { vm.refreshCurrencyRates(ratesUpdatedMsg, "Network error") },
                  onAddGoal = vm::addGoal,
                  onTopUpGoal = vm::topUpGoal,
                  onDeleteGoal = vm::deleteGoal,
                  onAddSubscription = vm::addSubscription,
                  onMarkSubPaid = vm::markSubscriptionPaid,
                  onDeleteSub = vm::deleteSubscription,
                )
              }

              composable<BanksRoute> {
                BanksScreen(
                  uiState = uiState,
                  onSyncMonobank = { token, fetch -> vm.syncMonobank(token, fetch) },
                  onReassignAccount = vm::reassignAccountWorkspace,
                  onLoadUkrainianPresets = vm::loadUkrainianPresets,
                  onSaveRule = vm::saveParsingRule,
                  onToggleRule = vm::toggleParsingRule,
                  onAssignBankToWorkspace = vm::assignBankPushToWorkspace,
                  onDeleteRule = vm::deleteParsingRule,
                  onSimulatePush = vm::simulatePushNotification,
                  onShowMessage = vm::showMessage,
                )
              }

              composable<ThemeStudioRoute> {
                ThemeStudioScreen(
                  uiState = uiState,
                  onBack = { navController.popBackStack() },
                  onSelectWorkspace = vm::selectWorkspace,
                  onCreateWorkspace = vm::createWorkspace,
                  onUpdateTheme = vm::updateWorkspaceTheme,
                  onShowMessage = vm::showMessage,
                )
              }

              composable<SettingsRoute> {
                SettingsScreen(
                  uiState = uiState,
                  onToggleBiometric = vm::setBiometricLock,
                  onToggleShakeToHide = vm::setShakeToHide,
                  onToggleHideBalances = vm::toggleHideBalances,
                  onChangeLanguage = vm::setAppLanguage,
                  onChangeGlobalCurrency = vm::setGlobalCurrency,
                  onPopulateDemo = { vm.populateDemoData(demoLoadedMsg) },
                  onClearAllData = vm::clearAllData,
                  onShowMessage = vm::showMessage,
                )
              }
            }
          }
        }
      }
    }
  }

  quickSheetType?.let { initialType ->
    QuickAddTransactionSheet(
      workspaces = uiState.workspaces,
      accounts = uiState.allAccounts,
      activeWorkspace = uiState.activeWorkspace,
      initialType = initialType,
      onDismiss = { quickSheetType = null },
      onSave = vm::addTransaction,
    )
  }
}
