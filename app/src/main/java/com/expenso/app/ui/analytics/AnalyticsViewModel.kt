package com.expenso.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.AccountType
import com.expenso.app.data.CategoryTotal
import com.expenso.app.data.DateX
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.LoanSummary
import com.expenso.app.data.MonthTotal
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.TxType
import com.expenso.app.data.UserPreferences
import com.expenso.app.data.local.CreditCardEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class AnalyticsTab { SPEND, CREDIT, EMI }

data class AnalyticsUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val tab: AnalyticsTab = AnalyticsTab.SPEND,
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val previousExpenseCents: Long = 0,
    val changePercent: Double? = null,
    val categories: List<CategoryTotal> = emptyList(),
    val trend: List<MonthTotal> = emptyList(),
    val dailyAverageCents: Long = 0,
    val transactionCount: Int = 0,
    val creditCards: List<CreditCardEntity> = emptyList(),
    val creditLimitCents: Long = 0,
    val creditBalanceCents: Long = 0,
    val creditUtilization: Float = 0f,
    val creditSpendCents: Long = 0,
    val loans: List<LoanSummary> = emptyList(),
    val completedLoans: Int = 0,
    val monthlyEmiCents: Long = 0,
    val outstandingCents: Long = 0,
    val totalInterestCents: Long = 0,
    val nextEmiDate: LocalDate? = null
)

class AnalyticsViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val tab = MutableStateFlow(AnalyticsTab.SPEND)
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.snapshot,
        settings.preferences,
        tab,
        selectedMonth
    ) { snapshot, prefs, currentTab, month ->
        val (income, expense) = FinanceMath.monthTotals(snapshot.transactions, month)
        val previous = FinanceMath.monthTotals(snapshot.transactions, month.minusMonths(1)).second
        val change = if (previous > 0) ((expense - previous).toDouble() / previous) * 100.0 else null
        val colorMap = snapshot.categories.associate { it.name to it.colorHex }
        val categories = FinanceMath.expenseByCategory(
            snapshot.transactions,
            month.atDay(1),
            month.atEndOfMonth()
        ).map { it.copy(colorHex = colorMap[it.category] ?: it.colorHex) }
        val trend = (5 downTo 0).map { offset ->
            val m = month.minusMonths(offset.toLong())
            val totals = FinanceMath.monthTotals(snapshot.transactions, m)
            MonthTotal(m, totals.first, totals.second)
        }
        val daysElapsed = when {
            month == YearMonth.now() -> LocalDate.now().dayOfMonth
            month.isBefore(YearMonth.now()) -> month.lengthOfMonth()
            else -> 1
        }
        val monthStart = month.atDay(1).toEpochDay()
        val monthEnd = month.atEndOfMonth().toEpochDay()
        val monthTransactions = snapshot.transactions.filter { it.dateEpochDay in monthStart..monthEnd }
        val creditSpend = monthTransactions
            .filter { it.type == TxType.EXPENSE && it.account == AccountType.CREDIT }
            .sumOf { it.amountCents }
        val (limit, balance, utilization) = FinanceMath.creditSummary(snapshot.cards)
        val summaries = FinanceMath.loanSummaries(snapshot)
        val activeLoans = summaries.filter { !it.isComplete }
        val nextDue = activeLoans.mapNotNull { it.nextInstallment }.minByOrNull { it.dueDate }
        AnalyticsUiState(
            loaded = true,
            preferences = prefs,
            tab = currentTab,
            selectedMonth = month,
            monthLabel = DateX.monthLabel(month),
            isCurrentMonth = month == YearMonth.now(),
            incomeCents = income,
            expenseCents = expense,
            previousExpenseCents = previous,
            changePercent = change,
            categories = categories,
            trend = trend,
            dailyAverageCents = if (daysElapsed > 0) expense / daysElapsed else 0,
            transactionCount = monthTransactions.count { it.type == TxType.EXPENSE },
            creditCards = snapshot.cards,
            creditLimitCents = limit,
            creditBalanceCents = balance,
            creditUtilization = utilization,
            creditSpendCents = creditSpend,
            loans = activeLoans,
            completedLoans = summaries.count { it.isComplete },
            monthlyEmiCents = activeLoans.sumOf { it.loan.emiCents },
            outstandingCents = activeLoans.sumOf { it.outstandingPrincipalCents },
            totalInterestCents = activeLoans.sumOf { it.totalInterestCents },
            nextEmiDate = nextDue?.dueDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setTab(value: AnalyticsTab) {
        tab.value = value
    }

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        if (selectedMonth.value < YearMonth.now()) {
            selectedMonth.value = selectedMonth.value.plusMonths(1)
        }
    }

    fun saveCard(
        id: Long = 0,
        name: String,
        last4: String,
        limitCents: Long,
        balanceCents: Long,
        dueDay: Int,
        colorIndex: Int
    ) {
        viewModelScope.launch {
            repository.saveCard(
                CreditCardEntity(
                    id = id,
                    name = name,
                    last4 = last4,
                    limitCents = limitCents,
                    balanceCents = balanceCents,
                    dueDay = dueDay,
                    colorIndex = colorIndex
                )
            )
        }
    }

    fun deleteCard(id: Long) {
        viewModelScope.launch { repository.deleteCard(id) }
    }

    fun payCard(card: CreditCardEntity, amountCents: Long) {
        viewModelScope.launch {
            repository.recordCardPayment(card, amountCents, DateX.monthKey(LocalDate.now()))
        }
    }
}
