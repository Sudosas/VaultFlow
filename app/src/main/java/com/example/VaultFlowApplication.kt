package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferences
import com.example.data.remote.MonobankApiService
import com.example.data.repository.VaultRepository

class VaultFlowApplication : Application() {
  lateinit var database: AppDatabase
    private set

  lateinit var preferences: UserPreferences
    private set

  lateinit var repository: VaultRepository
    private set

  override fun onCreate() {
    super.onCreate()
    database = AppDatabase.getInstance(this)
    preferences = UserPreferences(this)
    repository =
      VaultRepository(
        db = database,
        prefs = preferences,
        monobankApi = MonobankApiService.create(),
      )
  }
}
