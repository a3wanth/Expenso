package com.expenso.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val loanCount: Int = 0,
    val billCount: Int = 0,
    val cardCount: Int = 0,
    val categoryCount: Int = 0
)

class SettingsViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.snapshot,
        settings.preferences
    ) { snapshot, prefs ->
        SettingsUiState(
            loaded = true,
            preferences = prefs,
            transactionCount = snapshot.transactions.size,
            budgetCount = snapshot.budgets.size,
            loanCount = snapshot.loans.size,
            billCount = snapshot.bills.size,
            cardCount = snapshot.cards.size,
            categoryCount = snapshot.categories.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun saveProfile(name: String, avatarId: String) {
        viewModelScope.launch { settings.updateProfile(name, avatarId) }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settings.setCurrency(code) }
    }

    fun setTheme(mode: String) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { settings.setNotifications(enabled) }
    }

    fun setDueReminders(enabled: Boolean) {
        viewModelScope.launch { settings.setDueReminders(enabled) }
    }

    fun setHideBalance(hidden: Boolean) {
        viewModelScope.launch { settings.setHideBalance(hidden) }
    }

    fun enableAppLock(pin: String) {
        viewModelScope.launch { settings.setPin(pin) }
    }

    fun changePin(pin: String) {
        viewModelScope.launch { settings.setPin(pin) }
    }

    fun disableAppLock() {
        viewModelScope.launch { settings.clearPin() }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadDemoData()
            settings.markCategoriesSeeded()
            settings.setDemoMode(true)
        }
    }

    fun removeDemoData() {
        viewModelScope.launch {
            repository.clearFinancialData()
            repository.seedCategoriesIfEmpty()
            settings.markCategoriesSeeded()
            settings.setDemoMode(false)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearFinancialData()
            settings.setDemoMode(false)
        }
    }

    suspend fun buildExportCsv(): String = repository.buildCsv()
}
