package com.example.ghostespcompanion.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Data class representing app settings
 */
data class AppSettings(
    val darkMode: Boolean = true,
    val hapticFeedback: Boolean = true,
    val autoConnect: Boolean = true,
    val showNotifications: Boolean = true,
    val privacyMode: Boolean = false
)

/**
 * Repository for managing app settings using DataStore
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
    }

    /**
     * Flow of app settings
     */
    val appSettings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                darkMode = preferences[PreferencesKeys.DARK_MODE] ?: true,
                hapticFeedback = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true,
                autoConnect = preferences[PreferencesKeys.AUTO_CONNECT] ?: true,
                showNotifications = preferences[PreferencesKeys.SHOW_NOTIFICATIONS] ?: true,
                privacyMode = preferences[PreferencesKeys.PRIVACY_MODE] ?: false
            )
        }

    /**
     * Update dark mode setting
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    /**
     * Update haptic feedback setting
     */
    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    /**
     * Update auto connect setting
     */
    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CONNECT] = enabled
        }
    }

    /**
     * Update show notifications setting
     */
    suspend fun setShowNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_NOTIFICATIONS] = enabled
        }
    }

    /**
     * Update privacy mode setting
     */
    suspend fun setPrivacyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_MODE] = enabled
        }
    }

    /**
     * Update all settings at once
     */
    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = settings.darkMode
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = settings.hapticFeedback
            preferences[PreferencesKeys.AUTO_CONNECT] = settings.autoConnect
            preferences[PreferencesKeys.SHOW_NOTIFICATIONS] = settings.showNotifications
            preferences[PreferencesKeys.PRIVACY_MODE] = settings.privacyMode
        }
    }
}
