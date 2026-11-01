package com.expenso.app.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.DateX
import com.expenso.app.data.DueItem
import com.expenso.app.data.DueKind
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.LoanSummary
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.UserPreferences
import com.expenso.app.data.local.BillEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class EmiTab { LOANS, BILLS }

data class BillRow(
    val bill: BillEntity,
    val dueDate: LocalDate,
    val status: String,
    val monthKey: String
)

data class EmiUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val tab: EmiTab = EmiTab.LOANS,
    val activeLoans: List<LoanSummary> = emptyList(),
    val completedLoans: List<LoanSummary> = emptyList(),
    val bills: List<BillRow> = emptyList(),
    val monthlyEmiCents: Long = 0,
    val outstandingCents: Long = 0,
    val totalInterestCents: Long = 0,
    val totalRepaidCents: Long = 0,
    val nextDueDate: LocalDate? = null,
    val overdueBills: Int = 0,
    val monthLabel: String = "",
    val currentMonthKey: String = ""
)

class EmiViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val tab = MutableStateFlow(EmiTab.LOANS)

    val uiState: StateFlow<EmiUiState> = combine(
        repository.snapshot,
        settings.preferences,
        tab
    ) { snapshot, prefs, currentTab ->
        val month = YearMonth.now()
        val summaries = FinanceMath.loanSummaries(snapshot)
        val active = summaries.filter { !it.isComplete }
        val dueItems = FinanceMath.dueItemsForMonth(snapshot, month)
        val billItems = dueItems.filter { it.kind == DueKind.BILL }
        val bills = snapshot.bills.mapNotNull { bill ->
            val item = billItems.firstOrNull { it.referenceId == bill.id } ?: return@mapNotNull null
            BillRow(
                bill = bill,
                dueDate = item.dueDate,
                status = item.status,
                monthKey = DateX.monthKey(month)
            )
        }.sortedBy { it.dueDate }
        EmiUiState(
            loaded = true,
            preferences = prefs,
            tab = currentTab,
            activeLoans = active,
            completedLoans = summaries.filter { it.isComplete },
            bills = bills,
            monthlyEmiCents = active.sumOf { it.loan.emiCents },
            outstandingCents = active.sumOf { it.outstandingPrincipalCents },
            totalInterestCents = active.sumOf { it.totalInterestCents },
            totalRepaidCents = summaries.sumOf { it.paidCents },
            nextDueDate = active.mapNotNull { it.nextInstallment?.dueDate }.minOrNull(),
            overdueBills = billItems.count { it.status == DueStatus.OVERDUE },
            monthLabel = DateX.monthLabel(month),
            currentMonthKey = DateX.monthKey(month)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmiUiState())

    fun setTab(value: EmiTab) {
        tab.value = value
    }

    fun deleteLoan(id: Long) {
        viewModelScope.launch { repository.deleteLoan(id) }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch { repository.deleteBill(id) }
    }

    fun setBillPaid(bill: BillEntity, paid: Boolean) {
        viewModelScope.launch {
            repository.markBillPaid(bill, DateX.monthKey(LocalDate.now()), paid)
        }
    }
}
