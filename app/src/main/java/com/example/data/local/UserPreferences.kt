package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "vaultflow_prefs")

data class AppSettings(
  val monobankToken: String = "",
  val lastMonoSyncEpochMillis: Long = 0L,
  val biometricLockEnabled: Boolean = false,
  val shakeToHideEnabled: Boolean = true,
  val hideBalances: Boolean = false,
  val globalCurrency: String = "UAH",
  val selectedWorkspaceId: Long = ALL_WORKSPACES_ID,
  val appLanguage: String = "system",
) {
  companion object {
    const val ALL_WORKSPACES_ID = -1L
  }
}

class UserPreferences(private val context: Context) {
  private object Keys {
    val MONO_TOKEN = stringPreferencesKey("monobank_token")
    val LAST_MONO_SYNC = longPreferencesKey("last_mono_sync")
    val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    val SHAKE_TO_HIDE = booleanPreferencesKey("shake_to_hide")
    val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
    val GLOBAL_CURRENCY = stringPreferencesKey("global_currency")
    val SELECTED_WORKSPACE = longPreferencesKey("selected_workspace_id")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
  }

  val settingsFlow: Flow<AppSettings> =
    context.vaultDataStore.data.map { prefs ->
      AppSettings(
        monobankToken = prefs[Keys.MONO_TOKEN] ?: "",
        lastMonoSyncEpochMillis = prefs[Keys.LAST_MONO_SYNC] ?: 0L,
        biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
        shakeToHideEnabled = prefs[Keys.SHAKE_TO_HIDE] ?: true,
        hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
        globalCurrency = prefs[Keys.GLOBAL_CURRENCY] ?: "UAH",
        selectedWorkspaceId = prefs[Keys.SELECTED_WORKSPACE] ?: AppSettings.ALL_WORKSPACES_ID,
        appLanguage = prefs[Keys.APP_LANGUAGE] ?: "system",
      )
    }

  suspend fun getCurrentSettings(): AppSettings = settingsFlow.first()

  suspend fun setMonobankToken(token: String) {
    context.vaultDataStore.edit { it[Keys.MONO_TOKEN] = token.trim() }
  }

  suspend fun setLastMonoSync(epochMillis: Long) {
    context.vaultDataStore.edit { it[Keys.LAST_MONO_SYNC] = epochMillis }
  }

  suspend fun setBiometricLock(enabled: Boolean) {
    context.vaultDataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
  }

  suspend fun setShakeToHide(enabled: Boolean) {
    context.vaultDataStore.edit { it[Keys.SHAKE_TO_HIDE] = enabled }
  }

  suspend fun setHideBalances(hide: Boolean) {
    context.vaultDataStore.edit { it[Keys.HIDE_BALANCES] = hide }
  }

  suspend fun toggleHideBalances() {
    context.vaultDataStore.edit {
      val current = it[Keys.HIDE_BALANCES] ?: false
      it[Keys.HIDE_BALANCES] = !current
    }
  }

  suspend fun setGlobalCurrency(currency: String) {
    context.vaultDataStore.edit { it[Keys.GLOBAL_CURRENCY] = currency }
  }

  suspend fun setSelectedWorkspaceId(workspaceId: Long) {
    context.vaultDataStore.edit { it[Keys.SELECTED_WORKSPACE] = workspaceId }
  }

  suspend fun setAppLanguage(languageTag: String) {
    context.getSharedPreferences("vaultflow_locale", Context.MODE_PRIVATE)
      .edit()
      .putString("app_language", languageTag)
      .apply()
    context.vaultDataStore.edit { it[Keys.APP_LANGUAGE] = languageTag }
  }

  companion object {
    fun getLanguageSync(context: Context): String =
      context.getSharedPreferences("vaultflow_locale", Context.MODE_PRIVATE)
        .getString("app_language", "system") ?: "system"
  }
}
