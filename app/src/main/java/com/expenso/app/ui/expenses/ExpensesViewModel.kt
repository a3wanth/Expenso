package com.expenso.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.AccountType
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.local.TransactionEntity
import com.expenso.app.data.TxType
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class TransactionTab { ALL, EXPENSES, INCOME, CREDIT }

enum class DateFilter { ALL, THIS_MONTH, LAST_30, THIS_YEAR, CUSTOM }

data class ExpenseFilters(
    val query: String = "",
    val tab: TransactionTab = TransactionTab.ALL,
    val category: String? = null,
    val dateFilter: DateFilter = DateFilter.ALL,
    val customFrom: LocalDate = LocalDate.now().minusDays(30),
    val customTo: LocalDate = LocalDate.now()
)

data class CategoryOption(val name: String, val colorHex: String)

data class ExpensesUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val filters: ExpenseFilters = ExpenseFilters(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryOption> = emptyList(),
    val categoryColors: Map<String, String> = emptyMap(),
    val totalCount: Int = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val activeFilterCount: Int = 0
) {
    val hasActiveFilters: Boolean get() = activeFilterCount > 0
}

class ExpensesViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val filters = MutableStateFlow(ExpenseFilters())

    val uiState: StateFlow<ExpensesUiState> = combine(
        repository.snapshot,
        settings.preferences,
        filters
    ) { snapshot, prefs, currentFilters ->
        val filtered = snapshot.transactions.filter { tx -> matches(tx, currentFilters) }
        val categories = snapshot.categories
            .map { CategoryOption(it.name, it.colorHex) }
            .distinctBy { it.name }
        ExpensesUiState(
            loaded = true,
            preferences = prefs,
            filters = currentFilters,
            transactions = filtered,
            categories = categories,
            categoryColors = snapshot.categories.associate { it.name to it.colorHex },
            totalCount = snapshot.transactions.size,
            incomeCents = filtered.filter { it.type == TxType.INCOME }.sumOf { it.amountCents },
            expenseCents = filtered.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents },
            activeFilterCount = listOf(
                currentFilters.query.isNotBlank(),
                currentFilters.tab != TransactionTab.ALL,
                currentFilters.category != null,
                currentFilters.dateFilter != DateFilter.ALL
            ).count { it }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())

    private fun matches(tx: TransactionEntity, filter: ExpenseFilters): Boolean {
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim()
            val haystack = "${tx.title} ${tx.category} ${tx.notes}"
            if (!haystack.contains(q, ignoreCase = true)) return false
        }
        if (filter.category != null && tx.category != filter.category) return false
        val date = DateX.date(tx.dateEpochDay)
        val dateOk = when (filter.dateFilter) {
            DateFilter.ALL -> true
            DateFilter.THIS_MONTH -> {
                val month = YearMonth.now()
                !date.isBefore(month.atDay(1)) && !date.isAfter(month.atEndOfMonth())
            }
            DateFilter.LAST_30 -> !date.isBefore(LocalDate.now().minusDays(29)) && !date.isAfter(LocalDate.now())
            DateFilter.THIS_YEAR -> date.year == LocalDate.now().year
            DateFilter.CUSTOM -> !date.isBefore(filter.customFrom) && !date.isAfter(filter.customTo)
        }
        if (!dateOk) return false
        return when (filter.tab) {
            TransactionTab.ALL -> true
            TransactionTab.EXPENSES -> tx.type == TxType.EXPENSE && tx.account != AccountType.CREDIT
            TransactionTab.INCOME -> tx.type == TxType.INCOME
            TransactionTab.CREDIT -> tx.account == AccountType.CREDIT
        }
    }

    fun setQuery(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    fun setTab(tab: TransactionTab) {
        filters.value = filters.value.copy(tab = tab)
    }

    fun setCategory(category: String?) {
        filters.value = filters.value.copy(category = category)
    }

    fun setDateFilter(filter: DateFilter) {
        filters.value = filters.value.copy(dateFilter = filter)
    }

    fun setCustomRange(from: LocalDate, to: LocalDate) {
        filters.value = filters.value.copy(
            dateFilter = DateFilter.CUSTOM,
            customFrom = if (from.isAfter(to)) to else from,
            customTo = if (from.isAfter(to)) from else to
        )
    }

    fun clearFilters() {
        filters.value = ExpenseFilters()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun monthlyExpenseTotal(): Long {
        val month = YearMonth.now()
        return FinanceMath.totalExpense(uiState.value.transactions, month.atDay(1), month.atEndOfMonth())
    }
}
