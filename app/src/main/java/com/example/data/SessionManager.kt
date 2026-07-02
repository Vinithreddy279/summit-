package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "summit_sessions")

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        
        // Settings keys
        private val KEY_USE_IMPERIAL = booleanPreferencesKey("use_imperial")
        private val KEY_AUTO_PAUSE = booleanPreferencesKey("auto_pause_enabled")
        private val KEY_GPS_ACCURACY_THRESHOLD = intPreferencesKey("gps_accuracy_threshold")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val email = preferences[KEY_USER_EMAIL]
        if (email.isNullOrEmpty()) null else email
    }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMEMBER_ME] ?: false
    }

    val useImperialFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_USE_IMPERIAL] ?: false
    }

    val autoPauseFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_PAUSE] ?: false
    }

    val gpsAccuracyThresholdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_GPS_ACCURACY_THRESHOLD] ?: 10 // defaults to 10 meters
    }

    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE]
    }

    suspend fun saveSession(email: String, rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            if (preferences[KEY_REMEMBER_ME] != true) {
                preferences[KEY_USER_EMAIL] = ""
            }
        }
    }

    suspend fun setRememberMe(rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMEMBER_ME] = rememberMe
        }
    }

    suspend fun setUseImperial(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_IMPERIAL] = enabled
        }
    }

    suspend fun setAutoPause(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_PAUSE] = enabled
        }
    }

    suspend fun setGpsAccuracyThreshold(meters: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GPS_ACCURACY_THRESHOLD] = meters
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }
}
