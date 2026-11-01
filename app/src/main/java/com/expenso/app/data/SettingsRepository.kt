package com.expenso.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "expenso_settings")

data class UserPreferences(
    val name: String = "Alex Morgan",
    val avatarId: String = "",
    val currencyCode: String = "USD",
    val themeMode: String = "SYSTEM",
    val hideBalance: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dueReminders: Boolean = true,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val demoMode: Boolean = false,
    val categoriesSeeded: Boolean = false
) {
    val initials: String
        get() {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts.first().take(1).uppercase()
                else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
            }
        }

    val hasPin: Boolean get() = pinHash.isNotEmpty()
    val lockActive: Boolean get() = appLockEnabled && hasPin
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val name = stringPreferencesKey("name")
        val avatar = stringPreferencesKey("avatar")
        val currency = stringPreferencesKey("currency")
        val theme = stringPreferencesKey("theme")
        val hideBalance = booleanPreferencesKey("hide_balance")
        val notifications = booleanPreferencesKey("notifications")
        val dueReminders = booleanPreferencesKey("due_reminders")
        val appLock = booleanPreferencesKey("app_lock")
        val pinHash = stringPreferencesKey("pin_hash")
        val demo = booleanPreferencesKey("demo_mode")
        val seeded = booleanPreferencesKey("categories_seeded")
    }

    val preferences: Flow<UserPreferences> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            UserPreferences(
                name = prefs[Keys.name] ?: "Alex Morgan",
                avatarId = prefs[Keys.avatar] ?: "",
                currencyCode = prefs[Keys.currency] ?: "USD",
                themeMode = prefs[Keys.theme] ?: "SYSTEM",
                hideBalance = prefs[Keys.hideBalance] ?: false,
                notificationsEnabled = prefs[Keys.notifications] ?: true,
                dueReminders = prefs[Keys.dueReminders] ?: true,
                appLockEnabled = prefs[Keys.appLock] ?: false,
                pinHash = prefs[Keys.pinHash] ?: "",
                demoMode = prefs[Keys.demo] ?: false,
                categoriesSeeded = prefs[Keys.seeded] ?: false
            )
        }

    suspend fun updateProfile(name: String, avatarId: String) {
        context.settingsDataStore.edit {
            it[Keys.name] = name.trim().ifBlank { "Expenso User" }
            it[Keys.avatar] = avatarId
        }
    }

    suspend fun setCurrency(code: String) {
        context.settingsDataStore.edit { it[Keys.currency] = code }
    }

    suspend fun setTheme(mode: String) {
        context.settingsDataStore.edit { it[Keys.theme] = mode }
    }

    suspend fun setHideBalance(hidden: Boolean) {
        context.settingsDataStore.edit { it[Keys.hideBalance] = hidden }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.notifications] = enabled }
    }

    suspend fun setDueReminders(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.dueReminders] = enabled }
    }

    suspend fun setAppLock(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.appLock] = enabled }
    }

    suspend fun setPin(pin: String) {
        context.settingsDataStore.edit {
            it[Keys.pinHash] = hashPin(pin)
            it[Keys.appLock] = true
        }
    }

    suspend fun clearPin() {
        context.settingsDataStore.edit {
            it[Keys.pinHash] = ""
            it[Keys.appLock] = false
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        var stored = ""
        context.settingsDataStore.edit { prefs ->
            stored = prefs[Keys.pinHash] ?: ""
        }
        return stored.isNotEmpty() && stored == hashPin(pin)
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.demo] = enabled }
    }

    suspend fun markCategoriesSeeded() {
        context.settingsDataStore.edit { it[Keys.seeded] = true }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("expenso-lock-v1:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
