package com.expenso.app.ui.emi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenso.app.data.DateX
import com.expenso.app.data.DueItem
import com.expenso.app.data.DueKind
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val month: YearMonth = YearMonth.now(),
    val monthLabel: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val items: List<DueItem> = emptyList(),
    val selectedItems: List<DueItem> = emptyList(),
    val paidCount: Int = 0,
    val upcomingCount: Int = 0,
    val overdueCount: Int = 0,
    val upcomingAmountCents: Long = 0,
    val overdueAmountCents: Long = 0
)

class CalendarViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.snapshot,
        settings.preferences,
        month,
        selectedDate
    ) { snapshot, prefs, currentMonth, currentDate ->
        val items = FinanceMath.dueItemsForMonth(snapshot, currentMonth)
        val selected = items.filter { it.dueDate == currentDate }
        CalendarUiState(
            loaded = true,
            preferences = prefs,
            month = currentMonth,
            monthLabel = DateX.monthLabel(currentMonth),
            selectedDate = currentDate,
            items = items,
            selectedItems = selected,
            paidCount = items.count { it.status == DueStatus.PAID },
            upcomingCount = items.count { it.status == DueStatus.UPCOMING },
            overdueCount = items.count { it.status == DueStatus.OVERDUE },
            upcomingAmountCents = items.filter { it.status != DueStatus.PAID }.sumOf { it.amountCents },
            overdueAmountCents = items.filter { it.status == DueStatus.OVERDUE }.sumOf { it.amountCents }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        selectAppropriateDate()
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        selectAppropriateDate()
    }

    fun goToToday() {
        month.value = YearMonth.now()
        selectedDate.value = LocalDate.now()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun setPaid(item: DueItem, paid: Boolean) {
        viewModelScope.launch {
            val snapshot = repository.snapshot.first()
            when (item.kind) {
                DueKind.BILL -> {
                    val bill = snapshot.bills.firstOrNull { it.id == item.referenceId } ?: return@launch
                    repository.markBillPaid(bill, item.monthKey ?: DateX.monthKey(month.value), paid)
                }
                DueKind.LOAN -> {
                    val loan = snapshot.loans.firstOrNull { it.id == item.referenceId } ?: return@launch
                    item.installmentNumber?.let { repository.markInstallmentPaid(loan, it, paid) }
                }
                DueKind.CARD -> {
                    if (!paid) return@launch
                    val card = snapshot.cards.firstOrNull { it.id == item.referenceId } ?: return@launch
                    if (card.balanceCents > 0) {
                        repository.recordCardPayment(card, card.balanceCents, item.monthKey ?: DateX.monthKey(month.value))
                    }
                }
            }
        }
    }

    private fun selectAppropriateDate() {
        val current = month.value
        selectedDate.value = if (current == YearMonth.now()) LocalDate.now() else current.atDay(1)
    }
}
