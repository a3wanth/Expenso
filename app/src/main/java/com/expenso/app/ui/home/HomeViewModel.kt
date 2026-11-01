package com.expenso.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.BudgetProgress
import com.expenso.app.data.DateX
import com.expenso.app.data.DueItem
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.FinanceSnapshot
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.local.TransactionEntity
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class HomeUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val monthLabel: String = "",
    val greeting: String = "",
    val todayLabel: String = "",
    val balanceCents: Long = 0,
    val monthIncomeCents: Long = 0,
    val monthExpenseCents: Long = 0,
    val monthSavingsCents: Long = 0,
    val budgets: List<BudgetProgress> = emptyList(),
    val budgetTotalCents: Long = 0,
    val budgetSpentCents: Long = 0,
    val budgetSeries: List<Long> = emptyList(),
    val monthDays: Int = 30,
    val weekly: List<Pair<LocalDate, Long>> = emptyList(),
    val weeklyTotalCents: Long = 0,
    val upcoming: List<DueItem> = emptyList(),
    val overdue: List<DueItem> = emptyList(),
    val recent: List<TransactionEntity> = emptyList(),
    val categoryColors: Map<String, String> = emptyMap(),
    val categoryIcons: Map<String, String> = emptyMap(),
    val activeLoans: Int = 0,
    val nextEmiCents: Long = 0
) {
    val hiddenBalance: Boolean get() = preferences.hideBalance
    val alertCount: Int get() = overdue.size + upcoming.count { DateX.daysBetween(DateX.today(), it.dueDate) <= 7 }
    val budgetProgress: Float
        get() = if (budgetTotalCents <= 0) 0f else (budgetSpentCents.toFloat() / budgetTotalCents).coerceIn(0f, 1f)
}

class HomeViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.snapshot,
        settings.preferences
    ) { snapshot, prefs -> buildState(snapshot, prefs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(snapshot: FinanceSnapshot, prefs: UserPreferences): HomeUiState {
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val (income, expense) = FinanceMath.monthTotals(snapshot.transactions, month)
        val budget = FinanceMath.budgetProgress(snapshot, month)
        val weekly = FinanceMath.weeklyExpenses(snapshot.transactions, today)
        val upcoming = FinanceMath.upcoming(snapshot, 30)
        val overdue = FinanceMath.overdueItems(snapshot)
        val loans = FinanceMath.loanSummaries(snapshot).filter { !it.isComplete }
        return HomeUiState(
            loaded = true,
            preferences = prefs,
            monthLabel = DateX.monthLabel(month),
            greeting = DateX.greeting(),
            todayLabel = today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            balanceCents = FinanceMath.balance(snapshot.transactions),
            monthIncomeCents = income,
            monthExpenseCents = expense,
            monthSavingsCents = income - expense,
            budgets = budget,
            budgetTotalCents = budget.sumOf { it.budget.amountCents },
            budgetSpentCents = budget.sumOf { it.spentCents },
            budgetSeries = FinanceMath.cumulativeBudgetSpendByDay(
                snapshot.transactions,
                month,
                budget.map { it.budget.category }.toSet()
            ),
            monthDays = month.lengthOfMonth(),
            weekly = weekly,
            weeklyTotalCents = weekly.sumOf { it.second },
            upcoming = upcoming,
            overdue = overdue,
            recent = snapshot.transactions.take(5),
            categoryColors = snapshot.categories.associate { it.name to it.colorHex },
            categoryIcons = snapshot.categories.associate { it.name to it.iconKey },
            activeLoans = loans.size,
            nextEmiCents = loans.mapNotNull { it.nextInstallment }.minOfOrNull { it.amountCents } ?: 0L
        )
    }

    fun toggleBalanceVisibility() {
        viewModelScope.launch {
            settings.setHideBalance(!uiState.value.hiddenBalance)
        }
    }

    fun weeklyLabels(): List<String> {
        val values = uiState.value.weekly
        return if (values.isEmpty()) emptyList() else values.map {
            it.first.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
    }
}
