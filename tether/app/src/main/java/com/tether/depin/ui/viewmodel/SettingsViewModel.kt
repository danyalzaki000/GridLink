package com.tether.depin.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gridlink_settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // Preference keys
    private object Keys {
        val PERSISTENT_BACKGROUND = booleanPreferencesKey("persistent_background")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val DAILY_DATA_LIMIT_GB = floatPreferencesKey("daily_data_limit_gb")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val persistentBackground: StateFlow<Boolean> = dataStore.data
        .map { it[Keys.PERSISTENT_BACKGROUND] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wifiOnly: StateFlow<Boolean> = dataStore.data
        .map { it[Keys.WIFI_ONLY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val batterySaver: StateFlow<Boolean> = dataStore.data
        .map { it[Keys.BATTERY_SAVER] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailyDataLimitGb: StateFlow<Float> = dataStore.data
        .map { it[Keys.DAILY_DATA_LIMIT_GB] ?: 4.5f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4.5f)

    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setPersistentBackground(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[Keys.PERSISTENT_BACKGROUND] = enabled }
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
        }
    }

    fun setBatterySaver(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[Keys.BATTERY_SAVER] = enabled }
        }
    }

    fun setDailyDataLimitGb(limitGb: Float) {
        viewModelScope.launch {
            dataStore.edit { it[Keys.DAILY_DATA_LIMIT_GB] = limitGb }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
        }
    }
}
