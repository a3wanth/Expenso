package com.expenso.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RootViewModel(private val settings: SettingsRepository) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = settings.preferences
        .map<UserPreferences, UserPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private var startupLock = false

    init {
        viewModelScope.launch {
            val prefs = settings.preferences.first()
            startupLock = prefs.lockActive
            if (!prefs.lockActive) _unlocked.value = true
        }
    }

    fun unlock(pin: String) {
        viewModelScope.launch {
            if (settings.verifyPin(pin)) {
                _unlocked.value = true
                _pinError.value = null
            } else {
                _pinError.value = "That PIN isn't right. Try again."
            }
        }
    }

    fun clearPinError() {
        _pinError.value = null
    }

    fun lockNow() {
        viewModelScope.launch {
            if (settings.preferences.first().lockActive) {
                _unlocked.value = false
            }
        }
    }

    fun isStartupLocked(): Boolean = startupLock
}
