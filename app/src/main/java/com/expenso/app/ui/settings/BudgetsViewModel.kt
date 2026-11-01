package com.expenso.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.BudgetProgress
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.Money
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val monthLabel: String = "",
    val budgets: List<BudgetProgress> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val totalLimitCents: Long = 0,
    val totalSpentCents: Long = 0,
    val overBudgetCount: Int = 0
) {
    val totalProgress: Float
        get() = if (totalLimitCents <= 0) 0f else (totalSpentCents.toFloat() / totalLimitCents).coerceIn(0f, 1f)
}

class BudgetsViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.snapshot,
        settings.preferences
    ) { snapshot, prefs ->
        val month = YearMonth.now()
        val progress = FinanceMath.budgetProgress(snapshot, month)
        val used = progress.map { it.budget.category }.toSet()
        BudgetUiState(
            loaded = true,
            preferences = prefs,
            monthLabel = DateX.monthLabel(month),
            budgets = progress,
            availableCategories = snapshot.categories
                .filter { it.type == "EXPENSE" && it.name !in used }
                .map { it.name }
                .distinct()
                .sorted(),
            totalLimitCents = progress.sumOf { it.budget.amountCents },
            totalSpentCents = progress.sumOf { it.spentCents },
            overBudgetCount = progress.count { it.spentCents > it.budget.amountCents }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun saveBudget(category: String, amountText: String) {
        val amount = Money.parseToCents(amountText)
        if (category.isBlank()) {
            _message.value = "Choose a category"
            return
        }
        if (amount == null) {
            _message.value = "Enter a limit greater than zero"
            return
        }
        viewModelScope.launch {
            repository.saveBudget(category, amount)
            _message.value = "Budget updated"
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            _message.value = "Budget removed"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
